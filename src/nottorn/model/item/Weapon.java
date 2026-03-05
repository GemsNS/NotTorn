package nottorn.model.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A weapon item.  Base damage feeds into CombatCalculator.calculateDamage()
 * in place of the hard-coded unarmed value.  Accuracy is a multiplier on the
 * attacker's effective Speed when determining hit chance (Phase 3+).
 *
 * weaponExperience scales with usage per-player (tracked in inventory in
 * a later phase; stored here as a template default = 0).
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Weapon extends Item {

    private int            baseDamage;
    private double         accuracy       = 1.0;
    private WeaponCategory weaponCategory = WeaponCategory.MELEE;
    private int            weaponExperience = 0;

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int            getBaseDamage()                { return baseDamage; }
    public void           setBaseDamage(int v)           { this.baseDamage = v; }

    public double         getAccuracy()                  { return accuracy; }
    public void           setAccuracy(double v)          { this.accuracy = v; }

    public WeaponCategory getWeaponCategory()            { return weaponCategory; }
    public void           setWeaponCategory(WeaponCategory v) { this.weaponCategory = v; }

    public int            getWeaponExperience()          { return weaponExperience; }
    public void           setWeaponExperience(int v)     { this.weaponExperience = v; }
}
