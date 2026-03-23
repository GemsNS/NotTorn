package nottorn.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import nottorn.economy.Shop;
import nottorn.engine.RegenerationEngine;
import nottorn.model.GameConfig;
import nottorn.model.Player;
import nottorn.world.Location;
import nottorn.world.Npc;
import nottorn.world.TutorialNpc;
import nottorn.world.WorldMap;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * All Lanterna draw calls live here.  The Renderer is completely stateless
 * with respect to game logic — it only reads from the Player and
 * RegenerationEngine references it holds.
 *
 * Screen layout (120 × 40 default):
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  Row 0      : top border                                                │
 * │  Row 1      : title bar                                                 │
 * │  Row 2      : section divider  ├──── left panel ──┤ right panel ───────┤
 * │  Rows 3–36  : left panel (player stats) | right panel (event log)      │
 * │  Row 37     : section divider                                           │
 * │  Row 38     : status / timer bar                                        │
 * │  Row 39     : bottom border                                             │
 * └─────────────────────────────────────────────────────────────────────────┘
 * Left panel width  : 36 characters (cols 1–36)
 * Divider column    : 37
 * Right panel       : cols 38–(W-2)
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class Renderer {

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int LEFT_W   = 36;   // usable columns in left panel
    private static final int DIV_COL  = 37;   // column of the vertical divider
    private static final int BAR_W    = 22;   // width of stat progress bars

    // ── Colour palette ───────────────────────────────────────────────────────
    private static final TextColor BG          = TextColor.ANSI.BLACK;
    private static final TextColor BORDER      = new TextColor.RGB(180,  40,  40);
    private static final TextColor TITLE_FG    = TextColor.ANSI.YELLOW;
    private static final TextColor LABEL       = TextColor.ANSI.WHITE_BRIGHT;
    private static final TextColor VALUE       = TextColor.ANSI.WHITE;
    private static final TextColor DIM         = new TextColor.RGB(120, 120, 120);
    private static final TextColor COL_ENERGY  = new TextColor.RGB( 80, 200,  80);
    private static final TextColor COL_NERVE   = new TextColor.RGB(220,  60,  60);
    private static final TextColor COL_HAPPY   = new TextColor.RGB(180,  80, 220);
    private static final TextColor COL_WORK    = new TextColor.RGB( 80, 160, 220);
    private static final TextColor COL_MSG     = new TextColor.RGB(100, 200, 200);
    private static final TextColor COL_MSG_DIM = new TextColor.RGB( 60, 120, 120);
    private static final TextColor STATUS_BG   = new TextColor.RGB( 30,  90, 130);
    private static final TextColor STATUS_FG   = TextColor.ANSI.WHITE_BRIGHT;
    private static final TextColor SECTION     = new TextColor.RGB(160, 160,  60);
    private static final TextColor FULL_COL    = new TextColor.RGB(255, 215,   0);

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final Player             player;
    private final GameConfig         config;
    private final RegenerationEngine regen;
    private final WorldMap           world;        // may be null before Phase 5 data loads
    private final TutorialNpc        tutorialNpc;  // may be null
    private       Shop               shop;         // set via setShop(), used by inventory panel

    // ── Colours for world panel ───────────────────────────────────────────────
    private static final TextColor COL_LOCATION = new TextColor.RGB(100, 220, 180);
    private static final TextColor COL_NPC_HOST = new TextColor.RGB(220, 100,  80);
    private static final TextColor COL_NPC_SAFE = new TextColor.RGB(150, 150, 220);
    private static final TextColor COL_MISSION  = new TextColor.RGB(255, 200,  60);

    // ── Colour for menu ───────────────────────────────────────────────────────
    private static final TextColor COL_MENU_ITEM   = new TextColor.RGB(220, 220, 100);
    private static final TextColor COL_MENU_KEY    = new TextColor.RGB(255, 140,   0);
    private static final TextColor COL_MENU_HEADER = new TextColor.RGB(100, 200, 255);
    private static final TextColor COL_JAIL        = new TextColor.RGB(200,  80,  80);
    private static final TextColor COL_TRAVEL      = new TextColor.RGB(100, 200, 255);

    // ── Menu state ────────────────────────────────────────────────────────────
    public enum MenuType { NONE, CRIMES, JOBS, TRAVEL, GEORGE, BUY, SELL }

    private MenuType       activeMenu        = MenuType.NONE;
    private List<String>   menuItems         = new ArrayList<>();
    private String         menuTitle         = "";
    private int            pendingSelection  = -1;
    private List<Integer>  menuContext       = null;  // arbitrary int context (e.g. sell item IDs)

    // ── Panel state (full-screen overlays) ───────────────────────────────────
    public enum PanelType { NONE, INVENTORY, MAP, COMBAT_LOG }

    private PanelType    activePanel    = PanelType.NONE;
    private List<String> lastCombatLog  = new ArrayList<>();

    // ── State ────────────────────────────────────────────────────────────────
    private double totalSeconds = 0.0;
    private static final int      MAX_MESSAGES = 20;
    private final Deque<String>    messageLog   = new ArrayDeque<>(MAX_MESSAGES);

    // ────────────────────────────────────────────────────────────────────────

    public Renderer(Player player, GameConfig config, RegenerationEngine regen) {
        this(player, config, regen, null, null);
    }

    public Renderer(Player player, GameConfig config, RegenerationEngine regen,
                    WorldMap world, TutorialNpc tutorialNpc) {
        this.player      = player;
        this.config      = config;
        this.regen       = regen;
        this.world       = world;
        this.tutorialNpc = tutorialNpc;
    }

    // ── Menu API (called by GameEngine) ───────────────────────────────────────

    /** Opens a numbered selection menu in the right panel. */
    public void openMenu(MenuType type, String title, List<String> items) {
        this.activeMenu       = type;
        this.menuTitle        = title;
        this.menuItems        = new ArrayList<>(items);
        this.pendingSelection = -1;
    }

    /** Closes whatever menu is open. */
    public void closeMenu() {
        this.activeMenu       = MenuType.NONE;
        this.menuItems        = new ArrayList<>();
        this.pendingSelection = -1;
    }

    /**
     * Returns the pending 0-based selection index and clears it (one-shot).
     * Returns -1 if nothing was selected.
     */
    public int getAndClearPendingSelection() {
        int sel = pendingSelection;
        pendingSelection = -1;
        return sel;
    }

    public MenuType getActiveMenu() { return activeMenu; }

    public void setMenuContext(List<Integer> ctx) { this.menuContext = ctx; }
    public List<Integer> getMenuContext()         { return menuContext; }

    // ── Panel API ─────────────────────────────────────────────────────────────

    public void openPanel(PanelType type) {
        this.activePanel = type;
        closeMenu();
    }

    public void closePanel()                    { this.activePanel = PanelType.NONE; }
    public PanelType getActivePanel()           { return activePanel; }

    public void setLastCombatLog(List<String> log) {
        this.lastCombatLog = new ArrayList<>(log);
    }

    /** Called by Main after the Shop is loaded so the inventory panel can display names. */
    public void setShop(Shop shop) { this.shop = shop; }

    // ── Public API called by GameEngine ──────────────────────────────────────

    public void update(double deltaSeconds) {
        totalSeconds += deltaSeconds;
    }

    public void onKey(KeyStroke key) {
        if (key.getKeyType() == com.googlecode.lanterna.input.KeyType.Character) {
            char ch = key.getCharacter();
            if (activeMenu != MenuType.NONE) {
                if (ch >= '1' && ch <= '9') {
                    int idx = ch - '1';
                    if (idx < menuItems.size()) pendingSelection = idx;
                } else if (ch == '0') {
                    closeMenu();
                }
            }
            // Hotkeys to open menus — handled in GameEngine after checking Renderer
        }
        if (key.getKeyType() == com.googlecode.lanterna.input.KeyType.Escape) {
            closeMenu();
        }
    }

    /** Append a timestamped line to the right-panel event log. */
    public void addMessage(String text) {
        if (messageLog.size() >= MAX_MESSAGES) messageLog.pollFirst();
        LocalTime t = LocalTime.now();
        messageLog.addLast(String.format("[%02d:%02d:%02d] %s",
                t.getHour(), t.getMinute(), t.getSecond(), text));
    }

    /** Convenience: show offline summary at startup. */
    public void showOfflineMessage(String summary) {
        if (summary != null && !summary.isBlank()) addMessage(summary);
    }

    // ── Main render entry ────────────────────────────────────────────────────

    public void render(Screen screen) throws Exception {
        // On Linux, the OS delivers SIGWINCH when the terminal is resized.
        // Lanterna queues that event but does not apply it until
        // doResizeIfNecessary() is called.  Without this call,
        // screen.getTerminalSize() returns stale dimensions, the virtual buffers
        // remain the wrong size, and any area outside the old frame is never
        // written — leaving a frozen strip of old content after a resize.
        // When a resize is detected we use COMPLETE for that one frame so every
        // cell at the new size is pushed out, clearing any stale content.  Normal
        // frames continue to use DELTA (incremental diff, no blank flash).
        boolean resized = (screen.doResizeIfNecessary() != null);
        Screen.RefreshType refreshType = resized
                ? Screen.RefreshType.COMPLETE
                : Screen.RefreshType.DELTA;

        TextGraphics g    = screen.newTextGraphics();
        TerminalSize size  = screen.getTerminalSize();
        int W = size.getColumns();
        int H = size.getRows();

        fillBackground(g, W, H);

        if (activePanel != PanelType.NONE) {
            drawFullPanel(g, W, H);
            screen.refresh(refreshType);
            return;
        }

        drawOuterBorder(g, W, H);
        drawTitleBar(g, W);
        drawSectionDivider(g, W, 2);
        drawVerticalDivider(g, H);
        drawLeftPanel(g, W, H);
        drawRightPanel(g, W, H);
        drawSectionDivider(g, W, H - 2);
        drawStatusBar(g, W, H);

        screen.refresh(refreshType);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Border / chrome
    // ────────────────────────────────────────────────────────────────────────

    private void fillBackground(TextGraphics g, int W, int H) {
        g.setForegroundColor(BG);
        g.setBackgroundColor(BG);
        for (int r = 0; r < H; r++) g.putString(0, r, " ".repeat(W));
    }

    private void drawOuterBorder(TextGraphics g, int W, int H) {
        g.setForegroundColor(BORDER);
        g.setBackgroundColor(BG);
        String line = "+" + "=".repeat(W - 2) + "+";
        g.putString(0, 0,     line);
        g.putString(0, H - 1, line);
        for (int r = 1; r < H - 1; r++) {
            g.putString(0,     r, "|");
            g.putString(W - 1, r, "|");
        }
    }

    private void drawSectionDivider(TextGraphics g, int W, int row) {
        g.setForegroundColor(BORDER);
        g.setBackgroundColor(BG);
        // T-junction at left border, horizontal fill, T-junction at right border
        g.putString(0,     row, "+");
        g.putString(W - 1, row, "+");
        g.putString(1,     row, "-".repeat(W - 2));
    }

    private void drawVerticalDivider(TextGraphics g, int H) {
        g.setForegroundColor(BORDER);
        g.setBackgroundColor(BG);
        for (int r = 3; r <= H - 3; r++) {
            g.putString(DIV_COL, r, "|");
        }
        // Junction with section dividers
        g.putString(DIV_COL, 2,     "+");
        g.putString(DIV_COL, H - 2, "+");
    }

    private void drawTitleBar(TextGraphics g, int W) {
        String title = "  NotTorn  v1.0  |  Text-Based Crime Simulation  ";
        int    col   = Math.max(1, (W - title.length()) / 2);
        g.setForegroundColor(TITLE_FG);
        g.setBackgroundColor(BG);
        g.enableModifiers(SGR.BOLD);
        g.putString(col, 1, title);
        g.disableModifiers(SGR.BOLD);
    }

    // ────────────────────────────────────────────────────────────────────────
    // LEFT PANEL  – Player stats
    // ────────────────────────────────────────────────────────────────────────

    private void drawLeftPanel(TextGraphics g, int W, int H) {
        int row = 3;
        final int LC = 2; // left content start column

        row = drawSectionHeader(g, LC, row, "[ PLAYER ]");
        row = drawLabelValue(g, LC, row, "Name  :", player.getName());
        row = drawLabelValue(g, LC, row, "Level :", String.valueOf(player.getLevel()));
        row = drawLabelValue(g, LC, row, "Cash  :", "$" + formatNumber(player.getFiatBalance()));
        row++;

        // ── Energy ──────────────────────────────────────────────────────────
        row = drawSectionHeader(g, LC, row, "ENERGY");
        boolean energyFull = player.getEnergy() >= player.getNaturalMaxEnergy();
        drawBar(g, LC, row++, player.getEnergy(),
                player.getNaturalMaxEnergy(), BAR_W, COL_ENERGY);
        drawStatLine(g, LC, row++, player.getEnergy(),
                player.getNaturalMaxEnergy(), "/ " + player.getNaturalMaxEnergy(),
                energyFull, COL_ENERGY);

        if (energyFull) {
            drawTimerLine(g, LC, row++, "FULL", FULL_COL);
        } else {
            double secs = regen.secondsUntilEnergyTick(player);
            drawTimerLine(g, LC, row++, "Next +5 in  " + formatTime(secs), DIM);
        }
        row++;

        // ── Nerve ────────────────────────────────────────────────────────────
        row = drawSectionHeader(g, LC, row, "NERVE");
        boolean nerveFull = player.getNerve() >= player.getNaturalNerveBar();
        drawBar(g, LC, row++, player.getNerve(),
                player.getNaturalNerveBar(), BAR_W, COL_NERVE);
        drawStatLine(g, LC, row++, player.getNerve(),
                player.getNaturalNerveBar(), "/ " + player.getNaturalNerveBar(),
                nerveFull, COL_NERVE);

        String ceLabel = String.format("CE: %d  NNB: %d",
                player.getCrimeExperience(), player.getNaturalNerveBar());
        g.setForegroundColor(DIM);
        g.setBackgroundColor(BG);
        g.putString(LC, row++, ceLabel);

        if (nerveFull) {
            drawTimerLine(g, LC, row++, "FULL", FULL_COL);
        } else {
            double secs = regen.secondsUntilNerveTick(player);
            drawTimerLine(g, LC, row++, "Next +1 in  " + formatTime(secs), DIM);
        }
        row++;

        // ── Happiness ────────────────────────────────────────────────────────
        row = drawSectionHeader(g, LC, row, "HAPPINESS");
        double displayMax = Math.max(player.getPropertyMaxHappiness(), player.getHappiness());
        drawBar(g, LC, row++, player.getHappiness(), displayMax, BAR_W, COL_HAPPY);

        boolean overCap = player.getHappiness() > player.getPropertyMaxHappiness();
        String hapLabel = String.format("%.0f / %d%s",
                player.getHappiness(), player.getPropertyMaxHappiness(),
                overCap ? " [OVER-CAP]" : "");
        g.setForegroundColor(overCap ? COL_NERVE : COL_HAPPY);
        g.setBackgroundColor(BG);
        g.putString(LC, row++, hapLabel);

        double truncSecs = regen.secondsUntilHappinessTruncation();
        drawTimerLine(g, LC, row++, "Trunc in  " + formatTime(truncSecs),
                overCap ? COL_NERVE : DIM);
        row++;

        // ── Working stats ────────────────────────────────────────────────────
        if (row < H - 5) {
            row = drawSectionHeader(g, LC, row, "WORKING STATS");
            row = drawLabelValue(g, LC, row, "Manual :", formatNumber(player.getManualLabor()));
            row = drawLabelValue(g, LC, row, "Intel  :", formatNumber(player.getIntelligence()));
            row = drawLabelValue(g, LC, row, "Endur  :", formatNumber(player.getEndurance()));
            row++;
        }

        // ── Property ─────────────────────────────────────────────────────────
        if (row < H - 8) {
            row = drawSectionHeader(g, LC, row, "PROPERTY");
            row = drawLabelValue(g, LC, row, "Tier  :", player.getPropertyTier());
            row = drawLabelValue(g, LC, row, "Base  :", player.getPropertyMaxHappiness() + " happy");
            row++;
        }

        // ── Job ───────────────────────────────────────────────────────────────
        if (row < H - 6) {
            row = drawSectionHeader(g, LC, row, "JOB");
            if (player.getActiveJobCareerId() == null) {
                g.setForegroundColor(DIM);
                g.setBackgroundColor(BG);
                g.putString(LC, row++, "Unemployed  [J] to enroll");
            } else {
                String jStr = player.getActiveJobCareerId() + " Rank "
                        + (player.getActiveJobRankIndex() + 1);
                g.setForegroundColor(COL_WORK);
                g.setBackgroundColor(BG);
                g.putString(LC, row++, clip(jStr, LEFT_W - LC));
                if (player.isInJail()) {
                    long js = (player.getJailReleaseTimestamp() - System.currentTimeMillis()) / 1000;
                    g.setForegroundColor(COL_JAIL);
                    g.putString(LC, row++, clip("JAILED  " + formatTime(Math.max(0, js)), LEFT_W - LC));
                } else if (player.isTraveling()) {
                    long fs = (player.getTravelArrivalTimestamp() - System.currentTimeMillis()) / 1000;
                    g.setForegroundColor(COL_TRAVEL);
                    g.putString(LC, row++, clip("IN FLIGHT  ETA " + formatTime(Math.max(0, fs)), LEFT_W - LC));
                }
            }
            row++;
        }

        // ── Location ──────────────────────────────────────────────────────────
        if (world != null && row < H - 4) {
            row = drawSectionHeader(g, LC, row, "LOCATION");
            Location loc = world.getLocation(player.getCurrentLocationId());
            String locName = (loc != null) ? loc.getName() : player.getCurrentLocationId();
            g.setForegroundColor(COL_LOCATION);
            g.setBackgroundColor(BG);
            g.enableModifiers(SGR.BOLD);
            g.putString(LC, row++, clip(locName, LEFT_W - LC));
            g.disableModifiers(SGR.BOLD);

            if (loc != null && row < H - 4) {
                String exits = ">" + String.join("|", loc.getConnectedTo());
                g.setForegroundColor(DIM);
                g.putString(LC, row++, clip(exits, LEFT_W - LC));
            }
        }

        // ── Active mission ────────────────────────────────────────────────────
        if (tutorialNpc != null && row < H - 3) {
            String obj = tutorialNpc.getActiveObjectiveText(player);
            if (obj != null) {
                g.setForegroundColor(COL_MISSION);
                g.setBackgroundColor(BG);
                g.putString(LC, row, clip(obj, LEFT_W - LC));
            }
        }
    }

    // ── Left panel helpers ───────────────────────────────────────────────────

    private int drawSectionHeader(TextGraphics g, int col, int row, String text) {
        g.setForegroundColor(SECTION);
        g.setBackgroundColor(BG);
        g.enableModifiers(SGR.BOLD);
        g.putString(col, row, text);
        g.disableModifiers(SGR.BOLD);
        return row + 1;
    }

    private int drawLabelValue(TextGraphics g, int col, int row,
                                String label, String value) {
        g.setForegroundColor(DIM);
        g.setBackgroundColor(BG);
        g.putString(col, row, label);
        g.setForegroundColor(VALUE);
        g.putString(col + label.length() + 1, row, value);
        return row + 1;
    }

    private void drawBar(TextGraphics g, int col, int row,
                         double current, double max, int width,
                         TextColor fillColor) {
        int filled = (max > 0)
                     ? (int) Math.min(width, Math.round((current / max) * width))
                     : 0;
        filled = Math.max(0, Math.min(width, filled));

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < width; i++) bar.append(i < filled ? '=' : ' ');
        bar.append(']');

        g.setForegroundColor(fillColor);
        g.setBackgroundColor(BG);
        g.putString(col, row, bar.toString());
    }

    private void drawStatLine(TextGraphics g, int col, int row,
                              double current, double max, String suffix,
                              boolean full, TextColor color) {
        String line = String.format("%.0f %s", current, suffix);
        g.setForegroundColor(full ? FULL_COL : color);
        g.setBackgroundColor(BG);
        g.putString(col, row, line);
    }

    private void drawTimerLine(TextGraphics g, int col, int row,
                               String text, TextColor color) {
        g.setForegroundColor(color);
        g.setBackgroundColor(BG);
        g.putString(col, row, text);
    }

    // ────────────────────────────────────────────────────────────────────────
    // RIGHT PANEL  – Event log
    // ────────────────────────────────────────────────────────────────────────

    private void drawRightPanel(TextGraphics g, int W, int H) {
        int rc     = DIV_COL + 2;       // right content start column
        int rw     = W - rc - 2;        // usable width
        int row    = 3;

        // ── Active menu (overrides normal right panel when open) ───────────────
        if (activeMenu != MenuType.NONE) {
            row = drawMenuPanel(g, rc, rw, row, H);
            return;
        }

        // ── Location description ───────────────────────────────────────────────
        if (world != null) {
            row = drawSectionHeader(g, rc, row, "[ WORLD ]");
            Location loc = world.getLocation(player.getCurrentLocationId());
            if (loc != null && row < H - 5) {
                g.setForegroundColor(COL_LOCATION);
                g.setBackgroundColor(BG);
                g.enableModifiers(SGR.BOLD);
                g.putString(rc, row++, clip(loc.getName(), rw));
                g.disableModifiers(SGR.BOLD);
                g.setForegroundColor(DIM);
                g.putString(rc, row++, clip(loc.getDescription(), rw));
                row++;
            }

            // NPCs at the player's current location
            List<Npc> here = world.getNpcsAt(player.getCurrentLocationId());
            if (!here.isEmpty() && row < H - 5) {
                row = drawSectionHeader(g, rc, row, "[ NEARBY ]");
                for (Npc npc : here) {
                    if (row >= H - 5) break;
                    boolean hostile = !npc.isIndestructible() && !npc.isTutorial();
                    String npcLine  = String.format("  %-18s %-14s [%s]",
                            npc.getName(), npc.getTitle(), npc.getState());
                    g.setForegroundColor(hostile ? COL_NPC_HOST : COL_NPC_SAFE);
                    g.setBackgroundColor(BG);
                    g.putString(rc, row++, clip(npcLine, rw));
                }
                row++;
            }
        }

        // ── Event messages ─────────────────────────────────────────────────────
        if (row < H - 4) {
            row = drawSectionHeader(g, rc, row, "[ MESSAGES ]");

            // Word-wrap every stored message into display lines, tracking which
            // display lines belong to the most-recent message for colour purposes.
            String[] msgs = messageLog.toArray(new String[0]);
            List<String>  displayLines  = new ArrayList<>();
            List<Boolean> lineIsLatest  = new ArrayList<>();
            for (int i = 0; i < msgs.length; i++) {
                boolean latest = (i == msgs.length - 1);
                for (String line : wordWrap(msgs[i], rw)) {
                    displayLines.add(line);
                    lineIsLatest.add(latest);
                }
            }

            int available  = H - 4 - row;
            int startLine  = Math.max(0, displayLines.size() - available);
            for (int i = startLine; i < displayLines.size() && row < H - 4; i++) {
                g.setForegroundColor(lineIsLatest.get(i) ? COL_MSG : COL_MSG_DIM);
                g.setBackgroundColor(BG);
                g.putString(rc, row++, displayLines.get(i));
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // FULL-SCREEN PANELS  (Inventory / City Map / Combat Log)
    // ────────────────────────────────────────────────────────────────────────

    private void drawFullPanel(TextGraphics g, int W, int H) {
        // Outer border
        drawOuterBorder(g, W, H);
        drawTitleBar(g, W);
        drawSectionDivider(g, W, 2);

        int col = 2;
        int usableW = W - 4;

        switch (activePanel) {
            case INVENTORY   -> drawInventoryPanel(g, col, 3, usableW, H);
            case MAP         -> drawMapPanel(g, col, 3, usableW, H);
            case COMBAT_LOG  -> drawCombatLogPanel(g, col, 3, usableW, H);
            default          -> { }
        }

        // Footer hint
        g.setForegroundColor(DIM);
        g.setBackgroundColor(BG);
        g.putString(col, H - 2, "Press any key to close");
    }

    private void drawInventoryPanel(TextGraphics g, int c, int row, int w, int H) {
        g.setForegroundColor(COL_MENU_HEADER);
        g.setBackgroundColor(BG);
        g.enableModifiers(SGR.BOLD);
        int total = player.getInventory().distinctItemCount();
        g.putString(c, row++, "[ INVENTORY ]  "
                + total + " item type(s)  /  cargo capacity: " + player.getCargoCapacity());
        g.disableModifiers(SGR.BOLD);
        row++;

        if (total == 0) {
            g.setForegroundColor(DIM);
            g.putString(c, row, "Your inventory is empty.  [B] to buy from the Market.");
            return;
        }

        // Header row
        String hdr = String.format("  %-4s %-24s %-12s %8s %12s",
                "ID", "ITEM NAME", "TYPE", "QTY", "MKT PRICE");
        g.setForegroundColor(SECTION);
        g.putString(c, row++, clip(hdr, w));
        g.setForegroundColor(BORDER);
        g.putString(c, row++, "─".repeat(Math.min(w, 68)));

        for (var entry : player.getInventory().getItems().entrySet()) {
            if (row >= H - 4) break;
            int    id  = entry.getKey();
            long   qty = entry.getValue();
            String name = (shop != null && shop.getCatalog().getById(id) != null)
                    ? shop.getCatalog().getById(id).getName() : "Item #" + id;
            String type = (shop != null && shop.getCatalog().getById(id) != null)
                    ? shop.getCatalog().getById(id).getClass().getSimpleName() : "?";
            long   price = (shop != null) ? shop.getPrice(id) : 0;

            String line = String.format("  %-4d %-24s %-12s %8d %12s",
                    id, name, type, qty,
                    price > 0 ? "$" + String.format("%,d", price) : "—");
            g.setForegroundColor(COL_MENU_ITEM);
            g.setBackgroundColor(BG);
            g.putString(c, row++, clip(line, w));
        }
    }

    /** Fixed ASCII map of the city.  Player's location is highlighted. */
    private void drawMapPanel(TextGraphics g, int c, int row, int w, int H) {
        String loc = player.getCurrentLocationId();

        g.setForegroundColor(COL_MENU_HEADER);
        g.setBackgroundColor(BG);
        g.enableModifiers(SGR.BOLD);
        g.putString(c, row++, "[ CITY MAP ]   You are at: " + loc);
        g.disableModifiers(SGR.BOLD);
        row++;

        // Fixed layout string array – locations are bracketed for colour replacement
        String[][] mapLines = {
            { "                    ","BANK","                          " },
            { "                     |                                " },
            { " ","GYM"," ─────── ","CITY CENTER"," ─────── ","HOSPITAL"," " },
            { "  |              |           |              |         " },
            { "  |           ","ALLEY"," ─── ","MARKET"," ──────────────|  " },
            { "  |              |           |                        " },
            { "  |         ","POLICE STATION","  ","DRUG STORE","               " },
            { "  |                          |                        " },
            { "  └──────────────────────────┘                        " },
        };

        for (String[] parts : mapLines) {
            if (row >= H - 5) break;
            int colOffset = c;
            for (String part : parts) {
                String stripped = part.trim();
                boolean isLocName = world != null && world.hasLocation(stripped.replace(" ", "_"))
                        || isKnownLocationName(stripped);
                if (isLocName) {
                    boolean here = isPlayerHere(stripped, loc);
                    g.setForegroundColor(here ? FULL_COL : COL_LOCATION);
                    g.setBackgroundColor(here ? new TextColor.RGB(60, 40, 0) : BG);
                    g.enableModifiers(SGR.BOLD);
                    g.putString(colOffset, row, part);
                    g.disableModifiers(SGR.BOLD);
                    g.setBackgroundColor(BG);
                } else {
                    g.setForegroundColor(DIM);
                    g.setBackgroundColor(BG);
                    g.putString(colOffset, row, part);
                }
                colOffset += part.length();
            }
            row++;
        }

        row++;
        if (world != null) {
            Location current = world.getLocation(player.getCurrentLocationId());
            if (current != null) {
                g.setForegroundColor(VALUE);
                g.setBackgroundColor(BG);
                g.putString(c, row++, "Exits from " + current.getName() + ": "
                        + String.join("  |  ", current.getConnectedTo()));

                List<Npc> here = world.getNpcsAt(player.getCurrentLocationId());
                if (!here.isEmpty()) {
                    StringBuilder sb = new StringBuilder("NPCs here: ");
                    here.forEach(n -> sb.append(n.getName()).append(" [").append(n.getState()).append("]  "));
                    g.setForegroundColor(COL_NPC_HOST);
                    g.putString(c, row, clip(sb.toString(), w));
                }
            }
        }
    }

    private void drawCombatLogPanel(TextGraphics g, int c, int row, int w, int H) {
        g.setForegroundColor(COL_MENU_HEADER);
        g.setBackgroundColor(BG);
        g.enableModifiers(SGR.BOLD);
        g.putString(c, row++, "[ LAST COMBAT LOG ]");
        g.disableModifiers(SGR.BOLD);
        row++;

        if (lastCombatLog.isEmpty()) {
            g.setForegroundColor(DIM);
            g.putString(c, row, "No combat has occurred yet this session.");
            return;
        }

        int startIdx = Math.max(0, lastCombatLog.size() - (H - 8));
        for (int i = startIdx; i < lastCombatLog.size() && row < H - 4; i++) {
            String entry = lastCombatLog.get(i);
            boolean isCrit  = entry.contains("CRITICAL") || entry.contains("crit");
            boolean isWin   = entry.contains("defeated") || entry.contains("VICTORY");
            boolean isLoss  = entry.contains("failed") || entry.contains("DRAW");
            TextColor col   = isCrit ? COL_NPC_HOST
                            : isWin  ? FULL_COL
                            : isLoss ? COL_NERVE
                            : (i % 2 == 0 ? COL_MSG : COL_MSG_DIM);
            g.setForegroundColor(col);
            g.setBackgroundColor(BG);
            g.putString(c, row++, clip(entry, w));
        }
    }

    // ── Map helpers ───────────────────────────────────────────────────────────

    private static final java.util.Set<String> LOCATION_NAMES = java.util.Set.of(
            "BANK", "GYM", "CITY CENTER", "HOSPITAL",
            "ALLEY", "MARKET", "POLICE STATION", "DRUG STORE");

    private boolean isKnownLocationName(String s) {
        return LOCATION_NAMES.contains(s.toUpperCase().trim());
    }

    private boolean isPlayerHere(String displayName, String locId) {
        if (locId == null) return false;
        String norm = displayName.toUpperCase().trim().replace(" ", "_");
        return norm.equals(locId.toUpperCase())
                || displayName.toUpperCase().trim().equals(
                        locId.toUpperCase().replace("_", " "));
    }

    // ────────────────────────────────────────────────────────────────────────
    // MENU PANEL
    // ────────────────────────────────────────────────────────────────────────

    private int drawMenuPanel(TextGraphics g, int rc, int rw, int row, int H) {
        // Title
        g.setForegroundColor(COL_MENU_HEADER);
        g.setBackgroundColor(BG);
        g.enableModifiers(SGR.BOLD);
        g.putString(rc, row++, clip("[ " + menuTitle + " ]", rw));
        g.disableModifiers(SGR.BOLD);
        row++;

        // Items
        for (int i = 0; i < menuItems.size() && row < H - 5; i++) {
            g.setForegroundColor(COL_MENU_KEY);
            g.setBackgroundColor(BG);
            g.putString(rc, row, (i + 1) + ". ");
            g.setForegroundColor(COL_MENU_ITEM);
            g.putString(rc + 3, row, clip(menuItems.get(i), rw - 3));
            row++;
        }

        row++;
        g.setForegroundColor(DIM);
        g.setBackgroundColor(BG);
        g.putString(rc, row++, "Press 1-" + Math.min(9, menuItems.size()) + " to select  |  0 / ESC to close");

        return row;
    }

    // ────────────────────────────────────────────────────────────────────────
    // STATUS BAR  – timers row
    // ────────────────────────────────────────────────────────────────────────

    private void drawStatusBar(TextGraphics g, int W, int H) {
        int row = H - 2;

        double eTick  = regen.secondsUntilEnergyTick(player);
        double nTick  = regen.secondsUntilNerveTick(player);
        double hTrunc = regen.secondsUntilHappinessTruncation();
        long   upMins = (long)(totalSeconds / 60);
        long   upSecs = (long)(totalSeconds) % 60;

        // Status suffix: jail > travel > mission
        String statusSuffix;
        if (player.isInJail()) {
            long jailSecs = (player.getJailReleaseTimestamp() - System.currentTimeMillis()) / 1_000L;
            statusSuffix = "  |  *** JAILED: " + formatTime(Math.max(0, jailSecs)) + " ***";
        } else if (player.isTraveling()) {
            long flightSecs = (player.getTravelArrivalTimestamp() - System.currentTimeMillis()) / 1_000L;
            statusSuffix = "  |  IN FLIGHT -> " + player.getTravelDestinationId()
                           + " (" + formatTime(Math.max(0, flightSecs)) + ")";
        } else {
            String mission = (tutorialNpc != null)
                    ? tutorialNpc.getActiveObjectiveText(player) : null;
            statusSuffix = (mission != null) ? "  |  " + mission : "";
        }

        String bar = String.format(
            " Uptime %02d:%02d  |  E:%s  N:%s  Trunc:%s  |  @%s  |  [C]rime [J]ob [T]ravel [G]eorge%s  |  v0.6",
            upMins, upSecs,
            player.getEnergy() >= player.getNaturalMaxEnergy() ? "FULL" : formatTime(eTick),
            player.getNerve()  >= player.getNaturalNerveBar()  ? "FULL" : formatTime(nTick),
            formatTime(hTrunc),
            player.getCurrentLocationId(),
            statusSuffix);

        // Pad or clip to fit between the border columns
        int usable = W - 2;
        if (bar.length() < usable) bar = bar + " ".repeat(usable - bar.length());
        else if (bar.length() > usable) bar = bar.substring(0, usable);

        g.setForegroundColor(STATUS_FG);
        g.setBackgroundColor(STATUS_BG);
        g.putString(1, row, bar);
        g.setBackgroundColor(BG);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Utility helpers
    // ────────────────────────────────────────────────────────────────────────

    /** Formats seconds as "Xm Ys" or "Ys" if under a minute. */
    private static String formatTime(double totalSecs) {
        long s = Math.max(0, Math.round(totalSecs));
        long m = s / 60;
        long r = s % 60;
        return m > 0 ? String.format("%dm %02ds", m, r) : String.format("%ds", r);
    }

    /** Thousands-separator formatting for large numbers. */
    private static String formatNumber(long n) {
        return String.format("%,d", n);
    }

    /** Clips a string to the given maximum column width. */
    private static String clip(String s, int maxWidth) {
        return s.length() <= maxWidth ? s : s.substring(0, maxWidth);
    }

    /**
     * Wraps {@code text} into lines of at most {@code maxWidth} characters,
     * breaking at word boundaries where possible.
     *
     * Continuation lines are indented by the width of the timestamp prefix
     * ({@code "[HH:MM:SS] "} = 11 characters) so wrapped text aligns with
     * the start of the message body rather than the left edge of the panel.
     */
    private static List<String> wordWrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (maxWidth <= 0) { lines.add(text); return lines; }
        if (text.length() <= maxWidth) { lines.add(text); return lines; }

        // Length of "[HH:MM:SS] " prefix — continuation lines indent to match.
        final int INDENT = 11;
        final String pad = " ".repeat(Math.min(INDENT, maxWidth / 2));

        // First line uses the full width; subsequent lines are indented.
        String remaining = text;
        boolean first = true;
        while (!remaining.isEmpty()) {
            int limit = first ? maxWidth : Math.max(1, maxWidth - pad.length());
            if (remaining.length() <= limit) {
                lines.add(first ? remaining : pad + remaining);
                break;
            }
            int split = remaining.lastIndexOf(' ', limit);
            if (split <= 0) split = limit;
            lines.add(first ? remaining.substring(0, split)
                             : pad + remaining.substring(0, split));
            remaining = remaining.substring(split).stripLeading();
            first = false;
        }
        return lines;
    }
}
