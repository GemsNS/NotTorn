package nottorn.economy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import nottorn.model.Inventory;
import nottorn.model.Player;
import nottorn.model.item.Item;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The global Item Market — the primary fiat-to-item exchange.
 *
 * ── Design ────────────────────────────────────────────────────────────────────
 * Each item defined in items.json gets a ShopEntry tracking live supply and
 * the derived current price.  Prices inflate as supply drops (player buying)
 * and deflate as supply rises (player selling).
 *
 * ── Tax rules (from spec) ─────────────────────────────────────────────────────
 *   Standard purchase : seller pays 5% tax on proceeds  (applied on sell-back)
 *   Buy price         : no tax (buyer pays market price directly)
 *
 * ── Transaction limits ────────────────────────────────────────────────────────
 *   Maximum 10,000 items per single transaction to prevent integer overflow.
 *
 * ── Persistence ───────────────────────────────────────────────────────────────
 *   Supply state is saved to/loaded from data/shop_state.json so price
 *   history persists across game sessions.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class Shop {

    private static final String STATE_PATH  = "data/shop_state.json";
    private static final int    MAX_PER_TXN = 10_000;

    private final ItemCatalog   catalog;
    private final double        elasticity;
    private final double        minPriceRatio;
    private final double        maxPriceRatio;
    private final int           referenceSupply;
    private final double        salesTaxRate;

    /** Live supply/price entries, keyed by item ID. */
    private final Map<Integer, ShopEntry> entries = new LinkedHashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public Shop(ItemCatalog catalog,
                double elasticity, double minPriceRatio, double maxPriceRatio,
                int referenceSupply, double salesTaxRate) {
        this.catalog        = catalog;
        this.elasticity     = elasticity;
        this.minPriceRatio  = minPriceRatio;
        this.maxPriceRatio  = maxPriceRatio;
        this.referenceSupply = referenceSupply;
        this.salesTaxRate   = salesTaxRate;

        // Build entries from catalog (default supply for every item)
        for (Item item : catalog.all().values()) {
            ShopEntry e = new ShopEntry();
            e.setItemId(item.getId());
            e.setBasePrice(item.getBasePrice());
            e.setReferenceSupply(referenceSupply);
            e.setCurrentSupply(referenceSupply);
            e.applyConfig(elasticity, minPriceRatio, maxPriceRatio);
            entries.put(item.getId(), e);
        }
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    /**
     * Player buys qty units of itemId.
     *
     * @return result message; starts with "OK:" on success, "ERR:" on failure
     */
    public String buy(int itemId, int qty, Player player) {
        if (qty < 1 || qty > MAX_PER_TXN)
            return "ERR: Quantity must be 1–" + MAX_PER_TXN + ".";

        ShopEntry entry = entries.get(itemId);
        if (entry == null) return "ERR: Item not found in shop.";
        if (entry.getCurrentSupply() < qty)
            return String.format("ERR: Only %d units in stock.", entry.getCurrentSupply());

        long unitPrice = entry.getCurrentPrice();
        long totalCost = unitPrice * qty;

        if (player.getFiatBalance() < totalCost)
            return String.format("ERR: Insufficient funds. Need $%,d, have $%,d.",
                    totalCost, player.getFiatBalance());

        // Commit
        player.setFiatBalance(player.getFiatBalance() - totalCost);
        player.getInventory().add(itemId, qty);
        entry.decreaseSupply(qty);

        Item item = catalog.getById(itemId);
        return String.format("OK: Bought %dx %s for $%,d (unit $%,d). Supply now %d.",
                qty, item.getName(), totalCost, unitPrice, entry.getCurrentSupply());
    }

    /**
     * Player sells qty units of itemId back to the shop.
     * A 5% sales tax is deducted from the proceeds.
     *
     * @return result message
     */
    public String sell(int itemId, int qty, Player player) {
        if (qty < 1 || qty > MAX_PER_TXN)
            return "ERR: Quantity must be 1–" + MAX_PER_TXN + ".";

        if (!player.getInventory().has(itemId, qty))
            return String.format("ERR: You only have %d of that item.",
                    player.getInventory().getQuantity(itemId));

        ShopEntry entry = entries.get(itemId);
        if (entry == null) return "ERR: Shop does not buy this item.";

        long unitPrice    = entry.getBuybackPrice();
        long grossRevenue = unitPrice * qty;
        long tax          = (long)(grossRevenue * salesTaxRate);
        long netRevenue   = grossRevenue - tax;

        // Commit
        player.getInventory().remove(itemId, qty);
        player.setFiatBalance(player.getFiatBalance() + netRevenue);
        entry.increaseSupply(qty);

        Item item = catalog.getById(itemId);
        return String.format("OK: Sold %dx %s for $%,d (tax $%,d). Supply now %d.",
                qty, item.getName(), netRevenue, tax, entry.getCurrentSupply());
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public long getPrice(int itemId) {
        ShopEntry e = entries.get(itemId);
        return (e == null) ? 0L : e.getCurrentPrice();
    }

    public int getSupply(int itemId) {
        ShopEntry e = entries.get(itemId);
        return (e == null) ? 0 : e.getCurrentSupply();
    }

    /** Returns all entries sorted by item ID for UI listing. */
    public List<ShopEntry> getListing() {
        List<ShopEntry> list = new ArrayList<>(entries.values());
        list.sort(Comparator.comparingInt(ShopEntry::getItemId));
        return list;
    }

    public ItemCatalog getCatalog() { return catalog; }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void saveState() throws IOException {
        ObjectMapper m = new ObjectMapper();
        m.enable(SerializationFeature.INDENT_OUTPUT);
        File f = new File(STATE_PATH);
        f.getParentFile().mkdirs();
        m.writeValue(f, new StateWrapper(new ArrayList<>(entries.values())));
    }

    /**
     * Overlays persisted supply values onto this Shop's entries.
     * Any item ID in the file that no longer exists in the catalog is ignored.
     */
    public void loadState() throws IOException {
        File f = new File(STATE_PATH);
        if (!f.exists()) return;
        ObjectMapper m  = new ObjectMapper();
        StateWrapper sw = m.readValue(f, StateWrapper.class);
        if (sw.entries == null) return;
        for (ShopEntry saved : sw.entries) {
            ShopEntry live = entries.get(saved.getItemId());
            if (live != null) {
                live.setCurrentSupply(saved.getCurrentSupply());
                live.applyConfig(elasticity, minPriceRatio, maxPriceRatio);
            }
        }
    }

    // ── Jackson wrapper ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StateWrapper {
        public List<ShopEntry> entries;
        public StateWrapper() { }
        public StateWrapper(List<ShopEntry> e) { this.entries = e; }
    }
}
