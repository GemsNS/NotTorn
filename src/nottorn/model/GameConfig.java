package nottorn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Immutable game-balance configuration loaded from data/game_config.json.
 * All fields are public for direct read access; Jackson populates them
 * via its default no-arg constructor + field injection.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameConfig {

    // ── Energy ──────────────────────────────────────────────────────────────
    public int energyRegenIntervalSeconds         = 900;
    public int energyRegenIntervalDonatorSeconds  = 600;
    public int energyRegenAmount                  = 5;
    public int naturalMaxEnergy                   = 100;
    public int donatorMaxEnergy                   = 150;
    public int absoluteMaxEnergy                  = 1000;

    // ── Nerve ───────────────────────────────────────────────────────────────
    public int nerveRegenIntervalSeconds = 300;
    public int nerveRegenAmount          = 1;

    // ── Happiness ───────────────────────────────────────────────────────────
    public int absoluteMaxHappiness               = 99999;
    public int happinessTruncationIntervalMinutes = 15;

    // ── NNB step-function thresholds ────────────────────────────────────────
    public List<NnbThreshold> nnbThresholds;

    // ── Combat configuration ─────────────────────────────────────────────────
    public CombatConfig combat = new CombatConfig();

    // ── Shop configuration ────────────────────────────────────────────────────
    public ShopConfig shop = new ShopConfig();

    // ── Property definitions ─────────────────────────────────────────────────
    public List<PropertyConfig> properties;

    // ────────────────────────────────────────────────────────────────────────
    // Nested config types
    // ────────────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CombatConfig {
        public int    maxTurns              = 25;
        public double baseCritChance        = 0.12;
        public int    attackEnergyCost      = 25;
        public long   baseUnarmedDamage     = 50;
        public long   basePlayerHitPoints   = 1000;
        public double meritBonusPerLevel    = 0.03;
        public int    maxMeritLevel         = 10;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopConfig {
        public double priceElasticity       = 0.5;
        public double minPriceRatio         = 0.20;
        public double maxPriceRatio         = 5.00;
        public int    defaultReferenceSupply = 100;
        public double salesTaxRate          = 0.05;
        public double anonymousTaxRate      = 0.10;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NnbThreshold {
        public long ceRequired;
        public int  nnb;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertyConfig {
        public String tier;
        public long   baseCost;
        public int    baseHappiness;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Lookup helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Returns the base happiness for the given property tier string,
     * or the STARTER value if the tier is not found.
     */
    public int getBaseHappinessForTier(String tier) {
        if (properties == null) return 50;
        for (PropertyConfig p : properties) {
            if (p.tier.equalsIgnoreCase(tier)) return p.baseHappiness;
        }
        return 50;
    }

    /**
     * Returns the NNB value that corresponds to the supplied CE total,
     * using the last threshold whose ceRequired is <= ce.
     */
    public int getNnbForCe(long ce) {
        if (nnbThresholds == null || nnbThresholds.isEmpty()) return 10;
        int nnb = nnbThresholds.get(0).nnb;
        for (NnbThreshold t : nnbThresholds) {
            if (ce >= t.ceRequired) nnb = t.nnb;
        }
        return nnb;
    }
}
