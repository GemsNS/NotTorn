package nottorn.engine;

import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import nottorn.combat.CombatEngine;
import nottorn.combat.CombatLog;
import nottorn.crime.CrimeDefinition;
import nottorn.jobs.JobCareer;
import nottorn.model.GameConfig;
import nottorn.model.Player;
import nottorn.persistence.SaveManager;
import nottorn.travel.Destination;
import nottorn.ui.Renderer;
import nottorn.world.Npc;
import nottorn.world.NpcState;
import nottorn.world.TutorialNpc;
import nottorn.world.WorldMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Core game loop using a fixed-timestep delta-time architecture.
 *
 * Logic updates are decoupled from rendering so state advances at a
 * predictable rate regardless of frame time variance.
 *
 * Wired services:
 *  - {@link RegenerationEngine}  — energy / nerve / happiness ticks
 *  - {@link WorldMap}            — NPC FSM ticks (Phase 5)
 *  - {@link TutorialNpc}         — silent mission completion checks (Phase 5)
 *  - {@link CombatEngine}        — auto-triggered NPC combat (Phase 5)
 *  - {@link SaveManager}         — periodic auto-save every 60 s
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class GameEngine {

    /** 60 logical updates per second. */
    private static final long UPDATE_RATE_NS = 1_000_000_000L / 60;

    /** Auto-save interval in real seconds. */
    private static final double AUTO_SAVE_INTERVAL_S = 60.0;

    private final Screen             screen;
    private final Renderer           renderer;
    private final Player             player;
    private final RegenerationEngine regenEngine;
    private final SaveManager        saveManager;
    private final GameServices       services;
    private final CombatEngine       combatEngine;

    // Convenience aliases
    private WorldMap    world()       { return services.world; }
    private TutorialNpc tutorial()    { return services.tutorialNpc; }

    private volatile boolean running = false;
    private double autoSaveAccumulator = 0.0;

    // ────────────────────────────────────────────────────────────────────────

    public GameEngine(Screen screen,
                      Renderer renderer,
                      Player player,
                      RegenerationEngine regenEngine,
                      SaveManager saveManager,
                      GameServices services,
                      GameConfig config) {
        this.screen       = screen;
        this.renderer     = renderer;
        this.player       = player;
        this.regenEngine  = regenEngine;
        this.saveManager  = saveManager;
        this.services     = services;
        this.combatEngine = new CombatEngine(
                config.combat.baseCritChance,
                config.combat.baseUnarmedDamage);
    }

    /**
     * Enters the main game loop.  Blocks the calling thread until
     * {@link #stop()} is called or the user closes the window.
     */
    public void start() {
        running = true;

        long previousTime = System.nanoTime();
        long lag          = 0L;

        while (running) {
            long currentTime = System.nanoTime();
            long elapsed     = currentTime - previousTime;
            previousTime     = currentTime;
            lag             += elapsed;

            handleInput();

            // Fixed-timestep catch-up loop
            while (lag >= UPDATE_RATE_NS) {
                update(UPDATE_RATE_NS / 1_000_000_000.0);
                lag -= UPDATE_RATE_NS;
            }

            render();

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    public void stop() {
        running = false;
    }

    // ────────────────────────────────────────────────────────────────────────

    private void handleInput() {
        try {
            var key = screen.pollInput();
            if (key == null) return;

            switch (key.getKeyType()) {
                case EOF -> stop();
                case Escape -> {
                    if (renderer.getActiveMenu() == Renderer.MenuType.NONE) stop();
                    else renderer.closeMenu();
                }
                case Character -> {
                    // Let Renderer handle number-key selection first
                    renderer.onKey(key);

                    // Check if Renderer just registered a selection
                    int sel = renderer.getAndClearPendingSelection();
                    if (sel >= 0) {
                        handleMenuSelection(sel);
                        return;
                    }

                    // Top-level hotkeys (only when no menu is open)
                    if (renderer.getActiveMenu() == Renderer.MenuType.NONE
                            && renderer.getActivePanel() == Renderer.PanelType.NONE) {
                        char ch = Character.toLowerCase(key.getCharacter());
                        switch (ch) {
                            case 'c' -> openCrimeMenu();
                            case 'j' -> openJobMenu();
                            case 't' -> openTravelMenu();
                            case 'g' -> openGeorgeMenu();
                            case 'r' -> renderer.addMessage(
                                    services.travelService.returnHome(player, System.currentTimeMillis()));
                            case 'i' -> renderer.openPanel(Renderer.PanelType.INVENTORY);
                            case 'm' -> renderer.openPanel(Renderer.PanelType.MAP);
                            case 'l' -> renderer.openPanel(Renderer.PanelType.COMBAT_LOG);
                            case 'b' -> openBuyMenu();
                            case 's' -> openSellMenu();
                            default  -> { }
                        }
                    } else if (renderer.getActivePanel() != Renderer.PanelType.NONE) {
                        // Any key closes a full panel
                        renderer.closePanel();
                    }
                }
                default -> renderer.onKey(key);
            }
        } catch (Exception ignored) { }
    }

    // ── Menu builders ─────────────────────────────────────────────────────────

    private void openCrimeMenu() {
        List<String> items = new ArrayList<>();
        for (CrimeDefinition c : services.crimeCatalog.allCrimes()) {
            double rate = services.crimeService.effectiveSuccessRate(player, c);
            items.add(c.menuLine(rate));
        }
        renderer.openMenu(Renderer.MenuType.CRIMES, "CRIME  (nerve: " +
                (int) player.getNerve() + "/" + player.getNaturalNerveBar() + ")", items);
    }

    private void openJobMenu() {
        List<String> items = new ArrayList<>();
        for (JobCareer c : services.jobCatalog.allCareers()) {
            int best = services.jobCatalog.highestQualifyingRank(c.id,
                    player.getManualLabor(), player.getIntelligence(), player.getEndurance());
            String suffix = (best < 0) ? " [NO QUAL]" : " -> " + c.rankAt(best).title;
            items.add(c.name + suffix);
        }
        items.add("Resign from current job");
        renderer.openMenu(Renderer.MenuType.JOBS, "JOBS", items);
    }

    private void openTravelMenu() {
        List<String> items = new ArrayList<>();
        for (Destination d : services.travelCatalog.allDestinations()) {
            items.add(d.menuLine());
        }
        renderer.openMenu(Renderer.MenuType.TRAVEL, "TRAVEL  (cash: $" +
                String.format("%,d", player.getCash()) + ")", items);
    }

    private void openGeorgeMenu() {
        if (tutorial() == null) return;
        String msg = tutorial().interact(player);
        renderer.addMessage(msg);
    }

    private void openBuyMenu() {
        if (services.shop == null) return;
        List<String> items = new ArrayList<>();
        for (var entry : services.shop.getListing()) {
            var item = services.shop.getCatalog().getById(entry.getItemId());
            if (item == null) continue;
            items.add(String.format("%-22s  $%,6d   supply:%-4d  [%s]",
                    item.getName(), entry.getCurrentPrice(),
                    entry.getCurrentSupply(), item.getClass().getSimpleName()));
        }
        renderer.openMenu(Renderer.MenuType.BUY,
                "BUY  (cash: $" + String.format("%,d", player.getCash()) + ")", items);
    }

    private void openSellMenu() {
        if (services.shop == null) return;
        List<String> items = new ArrayList<>();
        List<Integer> sellIds = new ArrayList<>();
        for (var e : player.getInventory().getItems().entrySet()) {
            int id = e.getKey(); long qty = e.getValue();
            var item = services.shop.getCatalog().getById(id);
            if (item == null) continue;
            long price = services.shop.getPrice(id);
            long net   = (long)(price * 0.95);
            items.add(String.format("%-22s  x%-4d  buyback $%,d ea (-5%% tax)",
                    item.getName(), qty, net));
            sellIds.add(id);
        }
        if (items.isEmpty()) { renderer.addMessage("[Shop] Your inventory is empty."); return; }
        renderer.openMenu(Renderer.MenuType.SELL, "SELL", items);
        renderer.setMenuContext(sellIds);
    }

    // ── Menu selection handler ─────────────────────────────────────────────────

    private void handleMenuSelection(int sel) {
        switch (renderer.getActiveMenu()) {
            case CRIMES -> {
                CrimeDefinition crime = services.crimeCatalog.get(sel);
                if (crime != null) {
                    String result = services.crimeService.commit(player, crime, regenEngine);
                    renderer.addMessage(result);
                }
                renderer.closeMenu();
            }
            case JOBS -> {
                List<JobCareer> careers = services.jobCatalog.allCareers();
                if (sel < careers.size()) {
                    String result = services.jobService.enroll(player, careers.get(sel).id);
                    renderer.addMessage(result);
                } else {
                    // Resign option
                    renderer.addMessage(services.jobService.resign(player));
                }
                renderer.closeMenu();
            }
            case TRAVEL -> {
                Destination dest = services.travelCatalog.get(sel);
                if (dest != null) {
                    String result = services.travelService.depart(player, dest.id, System.currentTimeMillis());
                    renderer.addMessage(result);
                }
                renderer.closeMenu();
            }
            case BUY -> {
                if (services.shop != null) {
                    var listing = services.shop.getListing();
                    if (sel < listing.size()) {
                        String result = services.shop.buy(listing.get(sel).getItemId(), 1, player);
                        renderer.addMessage(result);
                        // Keep menu open with updated prices
                        openBuyMenu();
                        return;
                    }
                }
                renderer.closeMenu();
            }
            case SELL -> {
                if (services.shop != null) {
                    List<Integer> ctx = renderer.getMenuContext();
                    if (ctx != null && sel < ctx.size()) {
                        String result = services.shop.sell(ctx.get(sel), 1, player);
                        renderer.addMessage(result);
                        openSellMenu();
                        return;
                    }
                }
                renderer.closeMenu();
            }
            default -> renderer.closeMenu();
        }
    }

    private void update(double deltaSeconds) {
        // Regen ticks
        regenEngine.tick(player, deltaSeconds);

        // NPC FSM ticks
        if (world() != null) {
            world().tick(deltaSeconds, player.getCurrentLocationId());
            resolveNpcCombat();
        }

        // Tutorial mission silent completion check
        if (tutorial() != null) {
            String msg = tutorial().checkSilentCompletion(player);
            if (msg != null) renderer.addMessage(msg);
        }

        // Job daily tick (fires when 24 h of real time have elapsed)
        long now = System.currentTimeMillis();
        for (String m : services.jobService.tickIfDue(player, now)) renderer.addMessage(m);

        // Travel arrival check
        String arrival = services.travelService.checkArrival(player, now);
        if (arrival != null) renderer.addMessage(arrival);

        // Renderer state
        renderer.update(deltaSeconds);

        // Periodic auto-save
        autoSaveAccumulator += deltaSeconds;
        if (autoSaveAccumulator >= AUTO_SAVE_INTERVAL_S) {
            autoSaveAccumulator = 0;
            autoSave();
        }
    }

    /**
     * Checks for NPCs that transitioned to COMBAT at the player's location
     * and resolves a fight immediately.  The NPC is always the aggressor
     * (it chased the player).
     *
     * To prevent re-entrant loops, NPCs are set to IDLE (win) or DEAD (loss)
     * before the log messages are flushed.
     */
    private void resolveNpcCombat() {
        if (world() == null) return;
        List<Npc> combatants = world().getNpcsAt(player.getCurrentLocationId())
                .stream()
                .filter(n -> n.getState() == NpcState.COMBAT)
                .toList();

        for (Npc npc : combatants) {
            double playerCrit = player.getTotalCritChance(combatEngine.getBaseCritChance());

            CombatLog log = combatEngine.fight(
                    npc.getName(),     npc.getStats(),          npc.getCurrentHp(),  npc.getCritChance(),
                    player.getName(),  player.getBattleStats(),  player.getCurrentHitPoints(), playerCrit);

            if (log.isAttackerWon()) {
                long remaining = player.getCurrentHitPoints() - log.getTotalDamageByAttacker();
                player.setCurrentHitPoints(Math.max(1L, remaining));
                npc.setState(NpcState.IDLE);
                renderer.addMessage("[COMBAT] " + npc.getName() + " defeated you! -"
                        + log.getTotalDamageByAttacker() + " HP");
            } else {
                npc.setCurrentHp(0);
                npc.setState(NpcState.DEAD);
                renderer.addMessage("[COMBAT] You defeated " + npc.getName() + "! ("
                        + log.getTurnsElapsed() + " turns)");
            }

            // Flush last 4 combat lines to event log and store full log for [L] panel
            for (String entry : log.tail(4)) renderer.addMessage(entry);
            renderer.setLastCombatLog(log.getEntries());
        }
    }

    private void render() {
        try {
            renderer.render(screen);
        } catch (Exception ignored) { }
    }

    private void autoSave() {
        try {
            saveManager.save(player);
            if (services.shop != null) saveManager.saveShop(services.shop);
        } catch (Exception e) {
            renderer.addMessage("Auto-save failed: " + e.getMessage());
        }
    }
}
