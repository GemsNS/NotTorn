package nottorn.model.item;

/**
 * Rarity tier for items — controls spawn rates and NPC loot-table weights.
 *
 * <p>Higher rarity items appear less frequently in the city dump and on
 * enemy drop tables, and typically command higher prices on the Item Market.</p>
 *
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public enum ItemRarity {
    COMMON,
    UNCOMMON,
    LIMITED,
    RARE,
    VERY_RARE,
    EXTREMELY_RARE
}
