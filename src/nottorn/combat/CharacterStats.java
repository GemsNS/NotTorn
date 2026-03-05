package nottorn.combat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * The four primary Battle Stats from the spec.
 *
 * Because the game has no reset and stats compound indefinitely, all four
 * base values are stored as {@link BigInteger} to prevent arithmetic overflow
 * as veteran characters accumulate stats into the trillions.
 *
 * Merit upgrades apply a multiplicative bonus of +3% per level (max 10 levels
 * = +30%).  Effective stats are returned as {@link BigDecimal} for use in the
 * {@link CombatCalculator}'s BigDecimal division arithmetic.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterStats {

    private static final double MERIT_BONUS_PER_LEVEL = 0.03;
    private static final int    MAX_MERIT_LEVEL        = 10;
    private static final int    CALCULATION_SCALE      = 10;

    // ── Base stats (BigInteger for infinite scaling) ──────────────────────────
    private BigInteger strength  = BigInteger.valueOf(1_000_000L);
    private BigInteger defense   = BigInteger.valueOf(1_000_000L);
    private BigInteger speed     = BigInteger.valueOf(1_000_000L);
    private BigInteger dexterity = BigInteger.valueOf(1_000_000L);

    // ── Merit levels (0–10, each level = +3% multiplicative) ─────────────────
    private int strengthMeritLevel  = 0;
    private int defenseMeritLevel   = 0;
    private int speedMeritLevel     = 0;
    private int dexterityMeritLevel = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // Effective stat accessors  (base * merit multiplier)
    // ─────────────────────────────────────────────────────────────────────────

    public BigDecimal getEffectiveStrength() {
        return applyMerit(strength, strengthMeritLevel);
    }

    public BigDecimal getEffectiveDefense() {
        return applyMerit(defense, defenseMeritLevel);
    }

    public BigDecimal getEffectiveSpeed() {
        return applyMerit(speed, speedMeritLevel);
    }

    public BigDecimal getEffectiveDexterity() {
        return applyMerit(dexterity, dexterityMeritLevel);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Merit management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempt to increase a stat's merit level by one.
     * @return true if the level was incremented, false if already maxed.
     */
    public boolean incrementStrengthMerit()  { return incrementMerit("str"); }
    public boolean incrementDefenseMerit()   { return incrementMerit("def"); }
    public boolean incrementSpeedMerit()     { return incrementMerit("spd"); }
    public boolean incrementDexterityMerit() { return incrementMerit("dex"); }

    private boolean incrementMerit(String stat) {
        switch (stat) {
            case "str": if (strengthMeritLevel  < MAX_MERIT_LEVEL) { strengthMeritLevel++;  return true; } break;
            case "def": if (defenseMeritLevel   < MAX_MERIT_LEVEL) { defenseMeritLevel++;   return true; } break;
            case "spd": if (speedMeritLevel     < MAX_MERIT_LEVEL) { speedMeritLevel++;     return true; } break;
            case "dex": if (dexterityMeritLevel < MAX_MERIT_LEVEL) { dexterityMeritLevel++; return true; } break;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static BigDecimal applyMerit(BigInteger base, int meritLevel) {
        double multiplier = 1.0 + (meritLevel * MERIT_BONUS_PER_LEVEL);
        return new BigDecimal(base)
               .multiply(BigDecimal.valueOf(multiplier))
               .setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters / Setters  (Jackson needs these for serialization)
    // ─────────────────────────────────────────────────────────────────────────

    public BigInteger getStrength()            { return strength; }
    public void       setStrength(BigInteger v){ this.strength = v; }

    public BigInteger getDefense()             { return defense; }
    public void       setDefense(BigInteger v) { this.defense = v; }

    public BigInteger getSpeed()               { return speed; }
    public void       setSpeed(BigInteger v)   { this.speed = v; }

    public BigInteger getDexterity()              { return dexterity; }
    public void       setDexterity(BigInteger v)  { this.dexterity = v; }

    public int  getStrengthMeritLevel()                  { return strengthMeritLevel; }
    public void setStrengthMeritLevel(int v)             { this.strengthMeritLevel = clampMerit(v); }

    public int  getDefenseMeritLevel()                   { return defenseMeritLevel; }
    public void setDefenseMeritLevel(int v)              { this.defenseMeritLevel = clampMerit(v); }

    public int  getSpeedMeritLevel()                     { return speedMeritLevel; }
    public void setSpeedMeritLevel(int v)                { this.speedMeritLevel = clampMerit(v); }

    public int  getDexterityMeritLevel()                 { return dexterityMeritLevel; }
    public void setDexterityMeritLevel(int v)            { this.dexterityMeritLevel = clampMerit(v); }

    private static int clampMerit(int v) {
        return Math.max(0, Math.min(MAX_MERIT_LEVEL, v));
    }
}
