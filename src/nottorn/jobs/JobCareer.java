package nottorn.jobs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * A career path (Army, Medical, Education, Law) containing ordered {@link JobRank}s.
 * Loaded from data/jobs.json.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobCareer {

    public String        id    = "";
    public String        name  = "";
    public List<JobRank> ranks = new ArrayList<>();

    /** Returns the rank at index, or null if out of range. */
    public JobRank rankAt(int index) {
        return (index >= 0 && index < ranks.size()) ? ranks.get(index) : null;
    }

    /** Returns the maximum rank index (0-based). */
    public int maxRankIndex() {
        return ranks.size() - 1;
    }

    /** Returns the highest rank index the player qualifies for. */
    public int highestQualifyingIndex(long manual, long intel, long endurance) {
        int best = -1;
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).qualifies(manual, intel, endurance)) best = i;
        }
        return best;
    }

    @Override
    public String toString() {
        return name + " [" + id + "] (" + ranks.size() + " ranks)";
    }
}
