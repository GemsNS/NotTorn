package nottorn.jobs;

import nottorn.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all city-job progression logic.
 *
 * The job tick fires once every {@link #JOB_TICK_INTERVAL_MS} of real time.
 * On each tick it:
 *  1. Applies daily working stat gains for the player's current rank.
 *  2. Awards the daily income.
 *  3. Checks for auto-promotion to the next rank if stats qualify.
 *
 * Offline progression: {@link #tickIfDue} is called at startup with the current
 * timestamp; it will loop and award multiple missed ticks if the player was
 * offline for several days.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class JobService {

    /** 24 real-world hours between job ticks. */
    public static final long JOB_TICK_INTERVAL_MS = 86_400_000L;

    private final JobCatalog catalog;

    public JobService(JobCatalog catalog) {
        this.catalog = catalog;
    }

    // ── Enrollment ────────────────────────────────────────────────────────────

    /**
     * Attempts to enroll the player in the given career at their highest
     * qualifying rank.
     *
     * @return result message to show in the event log
     */
    public String enroll(Player player, String careerId) {
        JobCareer career = catalog.getCareer(careerId);
        if (career == null) return "Unknown career: " + careerId;

        int rank = catalog.highestQualifyingRank(careerId,
                player.getManualLabor(), player.getIntelligence(), player.getEndurance());

        if (rank < 0) {
            JobRank lowest = career.rankAt(0);
            return String.format("[Job] You don't meet the requirements for %s even at %s. "
                    + "(Need %d/%d/%d Man/Int/End)",
                    career.name, lowest.title,
                    lowest.reqManual, lowest.reqIntel, lowest.reqEndurance);
        }

        player.setActiveJobCareerId(careerId);
        player.setActiveJobRankIndex(rank);
        player.setLastJobTickTimestamp(System.currentTimeMillis());

        JobRank jr = career.rankAt(rank);
        return String.format("[Job] Enrolled in %s as %s. Income: $%,d/day.",
                career.name, jr.title, jr.dailyIncome);
    }

    /** Resigns the player from their current job. */
    public String resign(Player player) {
        if (player.getActiveJobCareerId() == null)
            return "[Job] You are not employed.";
        String old = player.getActiveJobCareerId();
        player.setActiveJobCareerId(null);
        player.setActiveJobRankIndex(0);
        return "[Job] Resigned from " + old + ".";
    }

    // ── Daily tick ────────────────────────────────────────────────────────────

    /**
     * Applies all pending job ticks since {@code player.getLastJobTickTimestamp()}.
     * Each tick grants working stats and income; auto-promotion is checked after
     * every tick.
     *
     * @param nowMs current epoch millis
     * @return list of log messages (one per tick applied, or empty if none due)
     */
    public List<String> tickIfDue(Player player, long nowMs) {
        List<String> messages = new ArrayList<>();
        if (player.getActiveJobCareerId() == null) return messages;

        long last = player.getLastJobTickTimestamp();
        if (last == 0) {
            player.setLastJobTickTimestamp(nowMs);
            return messages;
        }

        long elapsed = nowMs - last;
        int  ticks   = (int) Math.min(30, elapsed / JOB_TICK_INTERVAL_MS); // cap 30 days

        for (int i = 0; i < ticks; i++) {
            String msg = applyOneTick(player);
            if (msg != null) messages.add(msg);
        }

        if (ticks > 0) {
            player.setLastJobTickTimestamp(last + (long) ticks * JOB_TICK_INTERVAL_MS);
        }
        return messages;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String applyOneTick(Player player) {
        JobCareer career = catalog.getCareer(player.getActiveJobCareerId());
        if (career == null) return null;

        JobRank rank = career.rankAt(player.getActiveJobRankIndex());
        if (rank == null) return null;

        player.setManualLabor (player.getManualLabor()  + rank.gainManual);
        player.setIntelligence(player.getIntelligence() + rank.gainIntel);
        player.setEndurance   (player.getEndurance()    + rank.gainEndurance);
        player.setCash        (player.getCash()         + rank.dailyIncome);

        // Auto-promotion check
        int nextIdx = player.getActiveJobRankIndex() + 1;
        JobRank next = career.rankAt(nextIdx);
        if (next != null && next.qualifies(
                player.getManualLabor(), player.getIntelligence(), player.getEndurance())) {
            player.setActiveJobRankIndex(nextIdx);
            return String.format("[Job] Daily tick: +%d/%d/%d working, +$%,d. PROMOTED to %s!",
                    rank.gainManual, rank.gainIntel, rank.gainEndurance,
                    rank.dailyIncome, next.title);
        }

        return String.format("[Job] Daily tick (%s — %s): +%d/%d/%d working, +$%,d.",
                career.name, rank.title,
                rank.gainManual, rank.gainIntel, rank.gainEndurance,
                rank.dailyIncome);
    }
}
