package nottorn.economy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import nottorn.model.item.Item;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and indexes all item templates from data/items.json.
 *
 * This is a read-only singleton; item definitions never change at runtime.
 * Jackson uses the @JsonTypeInfo / @JsonSubTypes on Item to instantiate the
 * correct concrete subclass (Weapon / Armor / Consumable).
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class ItemCatalog {

    private static final String ITEMS_PATH = "data/items.json";

    private final Map<Integer, Item> byId;

    private ItemCatalog(Map<Integer, Item> byId) {
        this.byId = Collections.unmodifiableMap(byId);
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Reads items.json and returns a populated catalog.
     *
     * @throws IOException if the file is missing or contains malformed JSON
     */
    public static ItemCatalog load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper w = mapper.readValue(new File(ITEMS_PATH), Wrapper.class);

        Map<Integer, Item> map = new HashMap<>();
        if (w.items != null) {
            for (Item item : w.items) {
                map.put(item.getId(), item);
            }
        }
        return new ItemCatalog(map);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Item getById(int id) {
        return byId.get(id);
    }

    public boolean contains(int id) {
        return byId.containsKey(id);
    }

    public Map<Integer, Item> all() {
        return byId;
    }

    public int size() {
        return byId.size();
    }

    // ── Jackson wrapper ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Wrapper {
        public List<Item> items;
    }
}
