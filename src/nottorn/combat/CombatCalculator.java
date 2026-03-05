package nottorn.combat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Stateless resolver for all combat mathematics.
 *
 * ── Hit Chance ────────────────────────────────────────────────────────────────
 * The spec supplies 11 empirical benchmarks and describes a "logarithmic curve."
 * Rather than guessing a closed-form algebraic expression, we store the spec
 * values directly in a table and linearly interpolate on the log(speed/dex)
 * axis.  This guarantees exact spec values at every listed data point and a
 * smooth, monotonic curve between them.
 *
 * Hard boundaries (spec-derived):
 *   speed ≤ dex / 64  →  0 %
 *   speed ≥ dex × 64  →  100 %
 *
 * ── Damage Mitigation ─────────────────────────────────────────────────────────
 * Same table-interpolation approach using the spec's 11 mitigation data points.
 *
 *   defense ≤ strength / 32   →  0 %
 *   defense ≥ strength × 14   →  100 %
 *
 * ── Damage Resolution ─────────────────────────────────────────────────────────
 * Zone multipliers (3.5×, 2.0×, 1.0×, 0.7×) are ONLY applied on a critical
 * hit.  A non-critical hit deals base damage regardless of which zone was
 * struck.  This matches the spec's description of crits as the driver of
 * high-variance outcomes.
 *
 *   finalDamage = baseDamage × (crit ? hitbox.multiplier : 1.0) × (1 − mit)
 *   Minimum 1 HP damage on any connecting hit.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public final class CombatCalculator {

    // ── Spec data: log(speed/dex) → hit chance ────────────────────────────────
    // Derived from spec Table 1 (baseline defender dex = 10,000,000)
    private static final double[][] HIT_TABLE = {
        { Math.log(1.0 / 64.0),  0.0000 },   // 156,250 / 10M = 0%
        { Math.log(0.1),         0.1093 },   // 1M / 10M
        { Math.log(0.5),         0.3326 },   // 5M / 10M
        { Math.log(1.0),         0.5000 },   // equal stats
        { Math.log(2.0),         0.6674 },
        { Math.log(3.0),         0.7415 },
        { Math.log(4.0),         0.7857 },
        { Math.log(5.0),         0.8159 },
        { Math.log(7.0),         0.8554 },
        { Math.log(10.0),        0.8907 },
        { Math.log(64.0),        1.0000 },   // 640M / 10M = 100%
    };

    // ── Spec data: log(defense/strength) → mitigation ─────────────────────────
    // Derived from spec Table 2 (baseline attacker strength = 10,000,000)
    private static final double[][] MIT_TABLE = {
        { Math.log(1.0 / 32.0),  0.0000 },   // 312,500 / 10M = 0%
        { Math.log(0.125),       0.2000 },   // 1.25M / 10M
        { Math.log(0.5),         0.4000 },   // 5M / 10M
        { Math.log(1.0),         0.5000 },   // equal stats
        { Math.log(2.0),         0.6314 },
        { Math.log(3.0),         0.7081 },
        { Math.log(4.0),         0.7626 },
        { Math.log(5.0),         0.8049 },
        { Math.log(7.0),         0.8687 },
        { Math.log(10.0),        0.9363 },
        { Math.log(14.0),        1.0000 },   // 140M / 10M = 100%
    };

    private static final int RATIO_SCALE = 15; // decimal places for BigDecimal ratio

    private CombatCalculator() { }

    // ─────────────────────────────────────────────────────────────────────────
    // Hit chance
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns hit probability in [0.0, 1.0].
     * Benchmark (spec): equal 10M stats → exactly 0.5000.
     */
    public static double calculateHitChance(BigDecimal attackerSpeed,
                                            BigDecimal defenderDexterity) {
        if (attackerSpeed.signum() <= 0)      return 0.0;
        if (defenderDexterity.signum() <= 0)  return 1.0;

        double ratio = attackerSpeed
                .divide(defenderDexterity, RATIO_SCALE, RoundingMode.HALF_UP)
                .doubleValue();

        if (ratio <= 1.0 / 64.0) return 0.0;
        if (ratio >= 64.0)        return 1.0;

        return interpolate(HIT_TABLE, Math.log(ratio));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Damage mitigation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns damage fraction absorbed by the defender, in [0.0, 1.0].
     * Benchmark (spec): equal 10M stats → exactly 0.5000.
     */
    public static double calculateDamageMitigation(BigDecimal defenderDefense,
                                                   BigDecimal attackerStrength) {
        if (defenderDefense.signum() <= 0)    return 0.0;
        if (attackerStrength.signum() <= 0)   return 1.0;

        double ratio = defenderDefense
                .divide(attackerStrength, RATIO_SCALE, RoundingMode.HALF_UP)
                .doubleValue();

        if (ratio <= 1.0 / 32.0) return 0.0;
        if (ratio >= 14.0)        return 1.0;

        return interpolate(MIT_TABLE, Math.log(ratio));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Critical hit
    // ─────────────────────────────────────────────────────────────────────────

    /** @param critChance probability in [0.0, 1.0]; spec base = 0.12 */
    public static boolean isCriticalHit(double critChance, Random rng) {
        return rng.nextDouble() < critChance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Damage resolution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calculates final HP damage.
     *
     * Zone multipliers (3.5×/2.0×/1.0×/0.7×) are applied ONLY on critical
     * hits; a normal hit uses a flat 1.0× multiplier regardless of zone.
     *
     * @param baseDamage weapon/unarmed base damage
     * @param mitigation fraction absorbed (0.0–1.0)
     * @param hitbox     the zone struck
     * @param isCrit     whether this was a critical hit
     * @return final damage (minimum 1)
     */
    public static long calculateDamage(long   baseDamage,
                                       double mitigation,
                                       Hitbox hitbox,
                                       boolean isCrit) {
        double zoneMultiplier = isCrit ? hitbox.damageMultiplier : 1.0;
        double raw            = baseDamage * zoneMultiplier;
        double absorbed       = raw * mitigation;
        return Math.max(1L, Math.round(raw - absorbed));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Table interpolation (package-private for testing)
    // ─────────────────────────────────────────────────────────────────────────

    static double interpolate(double[][] table, double x) {
        // Below minimum → clamp to first entry
        if (x <= table[0][0]) return table[0][1];
        // Above maximum → clamp to last entry
        if (x >= table[table.length - 1][0]) return table[table.length - 1][1];

        for (int i = 0; i < table.length - 1; i++) {
            if (x >= table[i][0] && x <= table[i + 1][0]) {
                double span = table[i + 1][0] - table[i][0];
                double t    = (x - table[i][0]) / span;          // 0…1 within segment
                return table[i][1] + t * (table[i + 1][1] - table[i][1]);
            }
        }
        return 0.5; // unreachable
    }
}
