package nottorn.travel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Loads all foreign destinations from data/destinations.json.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class TravelCatalog {

    private static final String DEST_PATH = "data/destinations.json";

    private final List<Destination>        destinations;
    private final Map<String, Destination> byId;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static TravelCatalog load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper w = mapper.readValue(new File(DEST_PATH), Wrapper.class);
        List<Destination> list = (w.destinations != null) ? w.destinations : new ArrayList<>();
        Map<String, Destination> map = new LinkedHashMap<>();
        for (Destination d : list) map.put(d.id, d);
        return new TravelCatalog(list, map);
    }

    private TravelCatalog(List<Destination> destinations, Map<String, Destination> byId) {
        this.destinations = destinations;
        this.byId         = byId;
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public List<Destination>  allDestinations()  { return destinations; }
    public Destination        get(String id)     { return byId.get(id); }
    public Destination        get(int index)     {
        return (index >= 0 && index < destinations.size()) ? destinations.get(index) : null;
    }
    public int                size()             { return destinations.size(); }

    // ── Jackson wrapper ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Wrapper {
        public List<Destination> destinations;
    }
}
