package nottorn.combat;

import java.util.Random;

/**
 * The 14 targetable body regions defined by the spec.
 *
 * Each entry carries:
 *  - damageMultiplier  : applied to base damage on a hit to this zone
 *  - weight            : relative frequency this zone is targeted (sums to 100)
 *
 * Weight distribution is realistic: torso zones are hit most often,
 * vital zones (head/heart/throat) are rare but devastating.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public enum Hitbox {

    // Vital zones – 3.5x damage
    HEAD     (3.5,  5),
    THROAT   (3.5,  3),
    HEART    (3.5,  2),

    // Torso zones – 2.0x damage
    CHEST    (2.0, 15),
    STOMACH  (2.0, 12),

    // Limb zones – 1.0x damage
    LEFT_ARM (1.0,  8),
    RIGHT_ARM(1.0,  8),
    LEFT_LEG (1.0,  8),
    RIGHT_LEG(1.0,  8),

    // Extremity zones – 0.7x damage (glancing blow)
    LEFT_HAND (0.7,  7),
    RIGHT_HAND(0.7,  7),
    LEFT_FOOT (0.7,  7),
    RIGHT_FOOT(0.7,  7),

    // Groin – 1.5x (painful, not immediately lethal)
    GROIN    (1.5,  3);

    // ─────────────────────────────────────────────────────────────────────────

    public final double damageMultiplier;
    public final int    weight;

    Hitbox(double damageMultiplier, int weight) {
        this.damageMultiplier = damageMultiplier;
        this.weight           = weight;
    }

    // Pre-built cumulative weight table for O(n) weighted selection
    private static final int[]   CUMULATIVE;
    private static final Hitbox[] VALUES = values();
    private static final int     TOTAL_WEIGHT;

    static {
        CUMULATIVE = new int[VALUES.length];
        int running = 0;
        for (int i = 0; i < VALUES.length; i++) {
            running += VALUES[i].weight;
            CUMULATIVE[i] = running;
        }
        TOTAL_WEIGHT = running;
    }

    /** Returns a randomly selected hitbox according to the weight distribution. */
    public static Hitbox random(Random rng) {
        int roll = rng.nextInt(TOTAL_WEIGHT);
        for (int i = 0; i < CUMULATIVE.length; i++) {
            if (roll < CUMULATIVE[i]) return VALUES[i];
        }
        return CHEST; // fallback (unreachable)
    }

    /** True if this zone triggers the 3.5× critical-hit multiplier from the spec. */
    public boolean isVital() {
        return this == HEAD || this == THROAT || this == HEART;
    }
}
