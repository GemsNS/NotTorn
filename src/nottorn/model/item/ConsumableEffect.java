package nottorn.model.item;

/**
 * Describes what stat a {@link Consumable} modifies when used by the player.
 *
 * <p>The {@code Consumable.applyTo(Player)} method switches on this enum
 * to decide which field to increment.</p>
 *
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public enum ConsumableEffect {
    RESTORE_ENERGY,
    RESTORE_NERVE,
    RESTORE_HAPPINESS,
    RESTORE_HP,
    BOOST_STATS
}
