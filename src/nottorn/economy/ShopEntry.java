package nottorn.economy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single listing in the Shop's live catalog.
 *
 * Persisted to data/shop_state.json so supply levels survive game restarts.
 *
 * ── Price formula (supply-and-demand) ────────────────────────────────────────
 *
 *   currentPrice = basePrice × (referenceSupply / currentSupply) ^ elasticity
 *
 * With elasticity = 0.5 (square-root):
 *   supply = 100% (full)      → price = basePrice × 1.00
 *   supply =  50%             → price = basePrice × 1.41
 *   supply =  25%             → price = basePrice × 2.00
 *   supply =  10%             → price = basePrice × 3.16
 *
 * Hard clamps: [basePrice × minRatio, basePrice × maxRatio]
 * Default: [20% of base, 500% of base]
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopEntry {

    private int  itemId;
    private long basePrice;
    private int  referenceSupply = 100;
    private int  currentSupply   = 100;

    // Config pulled from GameConfig.shop at Shop construction
    private transient double elasticity   = 0.5;
    private transient double minRatio     = 0.20;
    private transient double maxRatio     = 5.00;

    // ── Price calculation ─────────────────────────────────────────────────────

    public long getCurrentPrice() {
        if (currentSupply <= 0) {
            return (long)(basePrice * maxRatio); // out of stock → ceiling price
        }
        double ratio      = (double) referenceSupply / currentSupply;
        double multiplier = Math.pow(ratio, elasticity);
        double price      = basePrice * multiplier;
        double min        = basePrice * minRatio;
        double max        = basePrice * maxRatio;
        return (long) Math.max(min, Math.min(max, price));
    }

    /** Returns the price the shop will pay when buying back 1 unit (no tax). */
    public long getBuybackPrice() {
        return (long)(getCurrentPrice() * 0.50); // shop pays 50% of current sell price
    }

    // ── Supply mutation ───────────────────────────────────────────────────────

    /** Called when a player purchases qty units. */
    public void decreaseSupply(int qty) {
        currentSupply = Math.max(0, currentSupply - qty);
    }

    /** Called when a player sells qty units back. */
    public void increaseSupply(int qty) {
        currentSupply = Math.min(referenceSupply * 3, currentSupply + qty);
    }

    // ── Config injection (called by Shop constructor) ─────────────────────────

    public void applyConfig(double elasticity, double minRatio, double maxRatio) {
        this.elasticity = elasticity;
        this.minRatio   = minRatio;
        this.maxRatio   = maxRatio;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int  getItemId()              { return itemId; }
    public void setItemId(int v)         { this.itemId = v; }

    public long getBasePrice()           { return basePrice; }
    public void setBasePrice(long v)     { this.basePrice = v; }

    public int  getReferenceSupply()     { return referenceSupply; }
    public void setReferenceSupply(int v){ this.referenceSupply = v; }

    public int  getCurrentSupply()       { return currentSupply; }
    public void setCurrentSupply(int v)  { this.currentSupply = v; }
}
