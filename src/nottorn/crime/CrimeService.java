package nottorn.crime;

import nottorn.engine.RegenerationEngine;
import nottorn.model.Player;

import java.util.Random;

/**
 * Resolves a single crime attempt.
 *
 * Success formula:
 *   finalSuccessRate = min(0.95, baseSuccessRate + CE_BONUS)
 *   CE_BONUS         = min(0.20, crimeExperience / CE_SCALE)
 *
 * This means a player with 1 000 CE gets +4% to every crime; at 5 000 CE
 * they cap out at the +20% bonus tier.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class CrimeService {

    private static final double CE_SCALE       = 5_000.0;
    private static final double MAX_CE_BONUS   = 0.20;
    private static final double HARD_FAIL_CAP  = 0.95;

    private final Random rng = new Random();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Attempts to commit the given crime.
     *
     * @return a message string describing the outcome
     */
    public String commit(Player player, CrimeDefinition crime,
                         RegenerationEngine regenEngine) {

        // Validate preconditions
        if (player.isInJail()) {
            long remaining = (player.getJailReleaseTimestamp() - System.currentTimeMillis()) / 1000;
            return String.format("[Crime] You are in jail. Release in %ds.", remaining);
        }
        if (player.isTraveling()) {
            return "[Crime] You cannot commit crimes while traveling.";
        }
        if (player.getNerve() < crime.nerveCost) {
            return String.format("[Crime] Not enough nerve. Need %d, have %.0f.",
                    crime.nerveCost, player.getNerve());
        }

        // Deduct nerve
        player.setNerve(player.getNerve() - crime.nerveCost);

        // Resolve success
        double ceBonus      = Math.min(MAX_CE_BONUS, player.getCrimeExperience() / CE_SCALE);
        double successRate  = Math.min(HARD_FAIL_CAP, crime.baseSuccessRate + ceBonus);
        boolean success     = rng.nextDouble() < successRate;

        if (success) {
            return resolveSuccess(player, crime, regenEngine);
        } else {
            return resolveFailure(player, crime, regenEngine);
        }
    }

    /**
     * Returns the effective success rate for a player on a given crime
     * (used by the menu to show preview odds).
     */
    public double effectiveSuccessRate(Player player, CrimeDefinition crime) {
        double ceBonus = Math.min(MAX_CE_BONUS, player.getCrimeExperience() / CE_SCALE);
        return Math.min(HARD_FAIL_CAP, crime.baseSuccessRate + ceBonus);
    }

    // ── Outcome resolution ────────────────────────────────────────────────────

    private String resolveSuccess(Player player, CrimeDefinition crime,
                                  RegenerationEngine regenEngine) {
        long reward = crime.minReward + (long)(rng.nextDouble() * (crime.maxReward - crime.minReward));
        player.setCash(player.getCash() + reward);
        player.setCrimeExperience(player.getCrimeExperience() + crime.ceGain);
        regenEngine.recalculateNnb(player);

        return String.format("[Crime] SUCCESS — %s. +$%,d  CE+%d  (total CE: %d)",
                crime.name, reward, crime.ceGain, player.getCrimeExperience());
    }

    private String resolveFailure(Player player, CrimeDefinition crime,
                                  RegenerationEngine regenEngine) {
        long cePenalty = Math.min(crime.cePenalty, player.getCrimeExperience());
        player.setCrimeExperience(player.getCrimeExperience() - cePenalty);
        regenEngine.recalculateNnb(player);

        // Jail chance: proportional to nerve cost
        if (crime.maxJailSeconds > 0 && rng.nextDouble() < 0.40) {
            int jailSecs = crime.minJailSeconds
                    + rng.nextInt(Math.max(1, crime.maxJailSeconds - crime.minJailSeconds));
            long releaseMs = System.currentTimeMillis() + (long) jailSecs * 1_000L;
            player.setJailReleaseTimestamp(releaseMs);
            return String.format("[Crime] FAILED — %s. CE-%d  JAILED for %ds!",
                    crime.name, cePenalty, jailSecs);
        }

        return String.format("[Crime] FAILED — %s. CE-%d",
                crime.name, cePenalty);
    }
}
