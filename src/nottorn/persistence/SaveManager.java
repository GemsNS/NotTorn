package nottorn.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import nottorn.economy.ItemCatalog;
import nottorn.economy.Shop;
import nottorn.model.GameConfig;
import nottorn.model.Player;

import java.io.File;
import java.io.IOException;

/**
 * Thin wrapper around Jackson for reading game_config.json and
 * reading/writing savegame.json.
 *
 * Paths are relative to the working directory (the project root when run
 * from VS Code or the command line).
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class SaveManager {

    private static final String CONFIG_PATH = "data/game_config.json";
    private static final String SAVE_PATH   = "data/savegame.json";

    private final ObjectMapper mapper;

    public SaveManager() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    // ── Config ───────────────────────────────────────────────────────────────

    /**
     * Reads game_config.json.  Throws {@link IOException} if the file is
     * missing or malformed — a hard start-up error.
     */
    public GameConfig loadConfig() throws IOException {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            throw new IOException(
                "game_config.json not found at: " + file.getAbsolutePath());
        }
        return mapper.readValue(file, GameConfig.class);
    }

    // ── Save-game ────────────────────────────────────────────────────────────

    /**
     * Serialises the player to savegame.json.
     * Stamps {@code lastSavedTimestamp} with the current epoch-millis before
     * writing so the offline service can measure elapsed time on next boot.
     *
     * @throws IOException on any I/O failure
     */
    public void save(Player player) throws IOException {
        player.setLastSavedTimestamp(System.currentTimeMillis());
        File file = new File(SAVE_PATH);
        file.getParentFile().mkdirs();
        mapper.writeValue(file, player);
    }

    /**
     * Deserialises the player from savegame.json.
     *
     * @return the Player, or {@code null} if no save file exists yet
     * @throws IOException on a parse error (corrupt save = hard error)
     */
    public Player load() throws IOException {
        File file = new File(SAVE_PATH);
        if (!file.exists()) return null;
        return mapper.readValue(file, Player.class);
    }

    /** Returns {@code true} if a save file currently exists. */
    public boolean hasSave() {
        return new File(SAVE_PATH).exists();
    }

    // ── Shop ─────────────────────────────────────────────────────────────────

    /**
     * Builds a fully initialised Shop from the item catalog and config,
     * then overlays any previously saved supply state.
     */
    public Shop loadShop(GameConfig config) throws IOException {
        ItemCatalog catalog = ItemCatalog.load();
        GameConfig.ShopConfig sc = config.shop;
        Shop shop = new Shop(catalog,
                sc.priceElasticity, sc.minPriceRatio, sc.maxPriceRatio,
                sc.defaultReferenceSupply, sc.salesTaxRate);
        shop.loadState();
        return shop;
    }

    /**
     * Persists the shop's current supply state to {@code data/shop_state.json}.
     * Called by the auto-save loop and on clean exit.
     */
    public void saveShop(Shop shop) throws IOException {
        shop.saveState();
    }
}
