package nottorn.engine;

import nottorn.model.GameConfig;
import nottorn.model.Player;

/**
 * Handles all server-tick regen and the 15-minute Happiness truncation.
 *
 * Called every game-loop iteration with the delta since the last frame.
 * Internally it maintains fractional-second accumulators (stored on the
 * Player so they survive save/load) and fires discrete ticks once enough
 * real time has passed.
 *
 * Key rules (from spec):
 *  - Energy: +5 every 900 s (standard) / 600 s (donator).
 *            Regen PAUSES if energy >= naturalMaxEnergy (consumable over-cap).
 *  - Nerve:  +1 every 300 s.
 *            Regen PAUSES if nerve >= naturalNerveBar.
 *  - Happiness truncation: any happiness > propertyMaxHappiness is hard-reset
 *            to propertyMaxHappiness at the first game-loop tick that occurs
 *            after a wall-clock XX:00 / XX:15 / XX:30 / XX:45 boundary.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class RegenerationEngine {

    private final GameConfig config;

    public RegenerationEngine(GameConfig config) {
        this.config = config;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Master tick.  Called by {@link GameEngine} once per update frame.
     *
     * @param player       mutable player state
     * @param deltaSeconds seconds elapsed since the previous update frame
     */
    public void tick(Player player, double deltaSeconds) {
        tickEnergy(player, deltaSeconds);
        tickNerve(player, deltaSeconds);
        checkHappinessTruncation(player);
    }

    /**
     * Recalculates the player's Natural Nerve Bar from their current CE
     * using the step-function table in game_config.json.
     * Call this at startup and after every crime that changes CE.
     */
    public void recalculateNnb(Player player) {
        int nnb = config.getNnbForCe(player.getCrimeExperience());
        player.setNaturalNerveBar(nnb);
        // Clamp current nerve to new NNB if it somehow exceeds it
        if (player.getNerve() > nnb) {
            player.setNerve(nnb);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Timer helpers for the UI (seconds until next tick)
    // ────────────────────────────────────────────────────────────────────────

    /** Seconds until the next +5 energy tick, or 0 if at/above natural max. */
    public double secondsUntilEnergyTick(Player player) {
        if (player.getEnergy() >= player.getNaturalMaxEnergy()) return 0;
        double interval = energyInterval(player);
        return Math.max(0, interval - player.getEnergyAccumulatedSeconds());
    }

    /** Seconds until the next +1 nerve tick, or 0 if at/above NNB. */
    public double secondsUntilNerveTick(Player player) {
        if (player.getNerve() >= player.getNaturalNerveBar()) return 0;
        return Math.max(0, config.nerveRegenIntervalSeconds
                           - player.getNerveAccumulatedSeconds());
    }

    /** Seconds until the next XX:00/15/30/45 happiness truncation boundary. */
    public double secondsUntilHappinessTruncation() {
        long now       = System.currentTimeMillis();
        long periodMs  = (long) config.happinessTruncationIntervalMinutes * 60_000L;
        long nextBound = ((now / periodMs) + 1L) * periodMs;
        return (nextBound - now) / 1_000.0;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private tick logic
    // ────────────────────────────────────────────────────────────────────────

    private void tickEnergy(Player player, double deltaSeconds) {
        // Regen pauses while over the natural cap (consumable over-cap state)
        if (player.getEnergy() >= player.getNaturalMaxEnergy()) {
            player.setEnergyAccumulatedSeconds(0);
            return;
        }

        double interval = energyInterval(player);
        player.setEnergyAccumulatedSeconds(
                player.getEnergyAccumulatedSeconds() + deltaSeconds);

        while (player.getEnergyAccumulatedSeconds() >= interval
               && player.getEnergy() < player.getNaturalMaxEnergy()) {
            player.setEnergyAccumulatedSeconds(
                    player.getEnergyAccumulatedSeconds() - interval);
            double next = Math.min(
                    player.getNaturalMaxEnergy(),
                    player.getEnergy() + config.energyRegenAmount);
            player.setEnergy(next);
        }

        // If we just capped out, reset so the UI shows "FULL" immediately
        if (player.getEnergy() >= player.getNaturalMaxEnergy()) {
            player.setEnergyAccumulatedSeconds(0);
        }
    }

    private void tickNerve(Player player, double deltaSeconds) {
        if (player.getNerve() >= player.getNaturalNerveBar()) {
            player.setNerveAccumulatedSeconds(0);
            return;
        }

        player.setNerveAccumulatedSeconds(
                player.getNerveAccumulatedSeconds() + deltaSeconds);

        while (player.getNerveAccumulatedSeconds() >= config.nerveRegenIntervalSeconds
               && player.getNerve() < player.getNaturalNerveBar()) {
            player.setNerveAccumulatedSeconds(
                    player.getNerveAccumulatedSeconds() - config.nerveRegenIntervalSeconds);
            double next = Math.min(
                    player.getNaturalNerveBar(),
                    player.getNerve() + config.nerveRegenAmount);
            player.setNerve(next);
        }

        if (player.getNerve() >= player.getNaturalNerveBar()) {
            player.setNerveAccumulatedSeconds(0);
        }
    }

    /**
     * Fires at the first game-loop tick that crosses a wall-clock 15-minute
     * boundary.  Truncates any over-capped happiness back to the property
     * baseline and records the boundary so we don't fire again until the next.
     */
    private void checkHappinessTruncation(Player player) {
        long now      = System.currentTimeMillis();
        long periodMs = (long) config.happinessTruncationIntervalMinutes * 60_000L;
        long lastBnd  = (now / periodMs) * periodMs;

        if (lastBnd > player.getLastHappinessTruncationTimestamp()) {
            if (player.getHappiness() > player.getPropertyMaxHappiness()) {
                player.setHappiness(player.getPropertyMaxHappiness());
            }
            player.setLastHappinessTruncationTimestamp(lastBnd);
        }
    }

    // ────────────────────────────────────────────────────────────────────────

    private double energyInterval(Player player) {
        return player.isDonator()
               ? config.energyRegenIntervalDonatorSeconds
               : config.energyRegenIntervalSeconds;
    }
}
