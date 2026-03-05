package nottorn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight item bag stored on the Player.
 *
 * Maps item IDs (from items.json) to held quantities.  All items are
 * treated as stackable for Phase 4; per-instance weapon-experience
 * tracking is a Phase 7 enhancement.
 *
 * The bulk-purchase cap of 10,000 items per transaction (spec) is
 * enforced by the Shop, not here.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Inventory {

    private Map<Integer, Long> items = new HashMap<>();

    // ── Mutation ─────────────────────────────────────────────────────────────

    /** Adds qty units of itemId. qty must be positive. */
    public void add(int itemId, long qty) {
        if (qty <= 0) return;
        items.merge(itemId, qty, Long::sum);
    }

    /**
     * Removes qty units of itemId.
     *
     * @return true if the player had enough stock; false (no-op) otherwise
     */
    public boolean remove(int itemId, long qty) {
        if (qty <= 0) return true;
        long held = items.getOrDefault(itemId, 0L);
        if (held < qty) return false;
        long remaining = held - qty;
        if (remaining == 0) items.remove(itemId);
        else                items.put(itemId, remaining);
        return true;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public long getQuantity(int itemId) {
        return items.getOrDefault(itemId, 0L);
    }

    public boolean has(int itemId, long qty) {
        return getQuantity(itemId) >= qty;
    }

    /** Total number of distinct item types currently held. */
    public int distinctItemCount() {
        return items.size();
    }

    /** Read-only view of the underlying map. */
    public Map<Integer, Long> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /** Required by Jackson for deserialization. */
    public void setItems(Map<Integer, Long> items) {
        this.items = items == null ? new HashMap<>() : new HashMap<>(items);
    }
}
