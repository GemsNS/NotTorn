package nottorn.engine;

import nottorn.model.GameConfig;
import nottorn.model.Player;

/**
 * Applied exactly once at game startup.
 *
 * Calculates how much real-world time elapsed since the last save, then
 * instantly awards all energy and nerve ticks the player would have
 * accumulated had the game been running the whole time.
 *
 * Design:
 *  - Energy / Nerve: the stored {@code *AccumulatedSeconds} fields already
 *    carry any fractional residual from the last session.  We add offline
 *    elapsed seconds to those residuals and process all full ticks at once,
 *    respecting natural caps.  The remaining fractional seconds are written
 *    back so the live regen engine picks up seamlessly.
 *
 *  - Happiness: since the player cannot consume items while offline, happiness
 *    can only decrease (via truncation).  If one or more 15-minute boundaries
 *    passed while offline and happiness exceeded the property cap, we apply the
 *    single terminal truncation (repeated truncations are idempotent).
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class OfflineProgressionService {

    private final GameConfig config;

    public OfflineProgressionService(GameConfig config) {
        this.config = config;
    }

    /**
     * Applies offline progression and returns a human-readable summary
     * suitable for display in the message log.
     *
     * @param player the freshly loaded player (mutated in-place)
     * @return summary string; empty string if no time has elapsed
     */
    public String apply(Player player) {
        long now       = System.currentTimeMillis();
        long lastSaved = player.getLastSavedTimestamp();

        // Brand-new player: nothing to catch up
        if (lastSaved == 0L) {
            player.setLastSavedTimestamp(now);
            initHappinessTruncationTimestamp(player, now);
            return "Welcome to NotTorn!  Your journey begins.";
        }

        long elapsedMs = now - lastSaved;
        if (elapsedMs <= 0) return "";

        double elapsedSec = elapsedMs / 1_000.0;

        double energyGained = applyEnergyOffline(player, elapsedSec);
        double nerveGained  = applyNerveOffline(player, elapsedSec);
        boolean truncated   = applyHappinessTruncation(player, now);

        return buildSummary(elapsedMs, energyGained, nerveGained, truncated);
    }

    // ────────────────────────────────────────────────────────────────────────

    private double applyEnergyOffline(Player player, double elapsedSec) {
        // If already over the natural cap (consumable state), regen was paused
        if (player.getEnergy() >= player.getNaturalMaxEnergy()) return 0;

        double interval = player.isDonator()
                          ? config.energyRegenIntervalDonatorSeconds
                          : config.energyRegenIntervalSeconds;

        double totalAccum = player.getEnergyAccumulatedSeconds() + elapsedSec;
        long   ticks      = (long) (totalAccum / interval);

        double before   = player.getEnergy();
        double newEnergy = Math.min(
                player.getNaturalMaxEnergy(),
                before + ticks * config.energyRegenAmount);
        player.setEnergy(newEnergy);

        // If we hit the cap, no point carrying a residual — regen will restart from 0
        double residual = (newEnergy >= player.getNaturalMaxEnergy())
                          ? 0
                          : totalAccum % interval;
        player.setEnergyAccumulatedSeconds(residual);

        return newEnergy - before;
    }

    private double applyNerveOffline(Player player, double elapsedSec) {
        if (player.getNerve() >= player.getNaturalNerveBar()) return 0;

        double totalAccum = player.getNerveAccumulatedSeconds() + elapsedSec;
        long   ticks      = (long) (totalAccum / config.nerveRegenIntervalSeconds);

        double before   = player.getNerve();
        double newNerve = Math.min(
                player.getNaturalNerveBar(),
                before + ticks * config.nerveRegenAmount);
        player.setNerve(newNerve);

        double residual = (newNerve >= player.getNaturalNerveBar())
                          ? 0
                          : totalAccum % config.nerveRegenIntervalSeconds;
        player.setNerveAccumulatedSeconds(residual);

        return newNerve - before;
    }

    private boolean applyHappinessTruncation(Player player, long now) {
        long periodMs  = (long) config.happinessTruncationIntervalMinutes * 60_000L;
        long lastBound = (now / periodMs) * periodMs;

        if (lastBound > player.getLastHappinessTruncationTimestamp()) {
            player.setLastHappinessTruncationTimestamp(lastBound);
            if (player.getHappiness() > player.getPropertyMaxHappiness()) {
                player.setHappiness(player.getPropertyMaxHappiness());
                return true;
            }
        }
        return false;
    }

    /** Ensures the truncation timestamp is primed on a first-ever load. */
    private void initHappinessTruncationTimestamp(Player player, long now) {
        long periodMs  = (long) config.happinessTruncationIntervalMinutes * 60_000L;
        player.setLastHappinessTruncationTimestamp((now / periodMs) * periodMs);
    }

    private String buildSummary(long elapsedMs, double energy, double nerve,
                                boolean truncated) {
        long hours   = elapsedMs / 3_600_000L;
        long minutes = (elapsedMs % 3_600_000L) / 60_000L;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Offline %dh %dm — ", hours, minutes));

        boolean anything = false;
        if (energy > 0)  { sb.append(String.format("+%.0f Energy  ", energy));  anything = true; }
        if (nerve  > 0)  { sb.append(String.format("+%.0f Nerve  ",  nerve));   anything = true; }
        if (truncated)   { sb.append("Happiness truncated  ");                  anything = true; }
        if (!anything)   { sb.append("no new ticks"); }

        return sb.toString().stripTrailing();
    }
}
