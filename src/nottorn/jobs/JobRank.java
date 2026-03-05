package nottorn.jobs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One rank within a {@link JobCareer}, loaded from data/jobs.json.
 *
 * The player is automatically promoted when all three working stat
 * requirements are met and their job-tick fires.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobRank {

    public String title         = "";
    public long   reqManual     = 0;
    public long   reqIntel      = 0;
    public long   reqEndurance  = 0;

    public long   gainManual    = 0;
    public long   gainIntel     = 0;
    public long   gainEndurance = 0;

    public long   dailyIncome   = 0;
    public String jobSpecial    = "";

    /** True if the player's current working stats meet this rank's requirements. */
    public boolean qualifies(long manual, long intel, long endurance) {
        return manual >= reqManual && intel >= reqIntel && endurance >= reqEndurance;
    }

    @Override
    public String toString() {
        return String.format("%s  [$%,d/day  +%d/%d/%d working]",
                title, dailyIncome, gainManual, gainIntel, gainEndurance);
    }
}
