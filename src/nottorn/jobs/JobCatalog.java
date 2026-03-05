package nottorn.jobs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Loads all career definitions from data/jobs.json and exposes lookup methods.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class JobCatalog {

    private static final String JOBS_PATH = "data/jobs.json";

    private final Map<String, JobCareer> careers;
    private final List<JobCareer>        orderedCareers;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static JobCatalog load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper w = mapper.readValue(new File(JOBS_PATH), Wrapper.class);
        Map<String, JobCareer> map = new LinkedHashMap<>();
        if (w.careers != null) {
            for (JobCareer c : w.careers) map.put(c.id, c);
        }
        return new JobCatalog(map);
    }

    private JobCatalog(Map<String, JobCareer> careers) {
        this.careers        = careers;
        this.orderedCareers = new ArrayList<>(careers.values());
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public JobCareer        getCareer(String careerId) { return careers.get(careerId); }
    public List<JobCareer>  allCareers()               { return orderedCareers; }

    /**
     * Returns the highest rank in the given career that the player's stats qualify for.
     * Returns -1 if the player doesn't meet even the lowest rank.
     */
    public int highestQualifyingRank(String careerId, long manual, long intel, long endurance) {
        JobCareer c = careers.get(careerId);
        if (c == null) return -1;
        return c.highestQualifyingIndex(manual, intel, endurance);
    }

    // ── Jackson wrapper ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Wrapper {
        public List<JobCareer> careers;
    }
}
