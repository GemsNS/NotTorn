package nottorn;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import nottorn.crime.CrimeCatalog;
import nottorn.engine.GameEngine;
import nottorn.engine.GameServices;
import nottorn.engine.OfflineProgressionService;
import nottorn.engine.RegenerationEngine;
import nottorn.jobs.JobCatalog;
import nottorn.model.GameConfig;
import nottorn.model.Player;
import nottorn.persistence.SaveManager;
import nottorn.travel.TravelCatalog;
import nottorn.ui.Renderer;
import nottorn.world.TutorialNpc;
import nottorn.world.WorldMap;

/**
 * Application entry-point for NotTorn.
 *
 * Initialisation pipeline (Phase 5):
 *  1. Load game_config.json via SaveManager.
 *  2. Load or create the Player from savegame.json.
 *  3. Apply offline progression (catch-up ticks since last save).
 *  4. Synchronise the player's propertyMaxHappiness from the config.
 *  5. Recalculate the Natural Nerve Bar from stored Crime Experience.
 *  6. Load WorldMap (locations graph + NPC instances) and TutorialNpc (George).
 *  7. Spin up Lanterna in a Swing frame, hand off to GameEngine.
 *  8. On clean exit: perform a final save.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // ── 1. Load configuration ────────────────────────────────────────────
        SaveManager saveManager = new SaveManager();
        GameConfig  config      = saveManager.loadConfig();

        // ── 2. Load or create player ─────────────────────────────────────────
        Player player = saveManager.load();
        if (player == null) {
            player = new Player();
            // Initialise derived fields for a brand-new player
            player.setPropertyMaxHappiness(
                    config.getBaseHappinessForTier(player.getPropertyTier()));
            player.setNaturalMaxEnergy(config.naturalMaxEnergy);
            player.setAbsoluteMaxEnergy(config.absoluteMaxEnergy);
            player.setAbsoluteMaxHappiness(config.absoluteMaxHappiness);
        }

        // ── 3. Offline progression ───────────────────────────────────────────
        OfflineProgressionService offlineService =
                new OfflineProgressionService(config);
        String offlineSummary = offlineService.apply(player);

        // ── 4. Sync derived fields (in case config changed between sessions) ──
        player.setPropertyMaxHappiness(
                config.getBaseHappinessForTier(player.getPropertyTier()));
        player.setNaturalMaxEnergy(
                player.isDonator() ? config.donatorMaxEnergy : config.naturalMaxEnergy);

        // ── 5. Recalculate NNB from CE ───────────────────────────────────────
        RegenerationEngine regenEngine = new RegenerationEngine(config);
        regenEngine.recalculateNnb(player);

        // ── 6. Load world map, tutorial NPC, Phase 6 catalogs, and Shop ─────
        WorldMap      world        = WorldMap.load();
        TutorialNpc   tutorialNpc  = TutorialNpc.load();
        JobCatalog    jobCatalog   = JobCatalog.load();
        CrimeCatalog  crimeCatalog = CrimeCatalog.load();
        TravelCatalog travelCatalog = TravelCatalog.load();
        nottorn.economy.Shop shop   = saveManager.loadShop(config);

        GameServices services = new GameServices(world, tutorialNpc,
                                                  jobCatalog, crimeCatalog, travelCatalog, shop);
        for (String m : services.jobService.tickIfDue(player, System.currentTimeMillis())) {
            System.out.println("[OFFLINE JOB] " + m);
        }

        // Ensure new players start at CITY_CENTER and mark it visited
        if (player.getCurrentLocationId() == null) {
            player.setCurrentLocationId("CITY_CENTER");
        } else {
            player.setCurrentLocationId(player.getCurrentLocationId()); // triggers visited add
        }

        // ── 7. Boot Lanterna ─────────────────────────────────────────────────
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        factory.setInitialTerminalSize(new TerminalSize(120, 40));
        factory.setTerminalEmulatorTitle("NotTorn  –  Text-Based Crime Simulation");

        Terminal terminal = factory.createTerminal();
        Screen   screen   = new TerminalScreen(terminal);
        screen.startScreen();
        screen.setCursorPosition(null); // hide blinking cursor

        // ── 8. Wire up renderer and engine ───────────────────────────────────
        Renderer renderer = new Renderer(player, config, regenEngine, world, tutorialNpc);
        renderer.setShop(shop);
        renderer.showOfflineMessage(offlineSummary);

        GameEngine engine = new GameEngine(screen, renderer, player,
                                           regenEngine, saveManager,
                                           services, config);

        try {
            engine.start();
        } finally {
            try { saveManager.save(player);  } catch (Exception ignored) { }
            try { saveManager.saveShop(shop);} catch (Exception ignored) { }
            try { screen.stopScreen();       } catch (Exception ignored) { }
        }
    }
}
