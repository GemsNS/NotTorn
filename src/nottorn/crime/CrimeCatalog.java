package nottorn.crime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Loads all crime definitions from data/crimes.json.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class CrimeCatalog {

    private static final String CRIMES_PATH = "data/crimes.json";

    private final List<CrimeDefinition>        crimes;
    private final Map<String, CrimeDefinition> byId;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static CrimeCatalog load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper w = mapper.readValue(new File(CRIMES_PATH), Wrapper.class);
        List<CrimeDefinition> list = (w.crimes != null) ? w.crimes : new ArrayList<>();
        Map<String, CrimeDefinition> map = new LinkedHashMap<>();
        for (CrimeDefinition c : list) map.put(c.id, c);
        return new CrimeCatalog(list, map);
    }

    private CrimeCatalog(List<CrimeDefinition> crimes, Map<String, CrimeDefinition> byId) {
        this.crimes = crimes;
        this.byId   = byId;
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public List<CrimeDefinition>  allCrimes()       { return crimes; }
    public CrimeDefinition        get(String id)    { return byId.get(id); }
    public CrimeDefinition        get(int index)    {
        return (index >= 0 && index < crimes.size()) ? crimes.get(index) : null;
    }
    public int                    size()            { return crimes.size(); }

    // ── Jackson wrapper ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Wrapper {
        public List<CrimeDefinition> crimes;
    }
}
