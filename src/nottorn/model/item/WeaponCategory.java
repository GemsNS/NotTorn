package nottorn.model.item;

/**
 * Broad classification of a weapon's form-factor and intended use.
 *
 * <p>Used by the combat engine and shop filtering logic to group weapons
 * and apply category-specific merit bonuses (e.g. Heavy Artillery, Shotguns).</p>
 *
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public enum WeaponCategory {
    MELEE,
    PRIMARY,    // rifles, shotguns
    SECONDARY   // pistols, sub-guns
}
