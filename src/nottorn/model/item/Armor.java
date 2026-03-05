package nottorn.model.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An armor item.  armorRating is an additive bonus to the defender's
 * damage mitigation result (0.0 = no bonus, 0.50 = 50% extra mitigation).
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Armor extends Item {

    /** Flat mitigation bonus added on top of the stat-based calculation. */
    private double armorRating = 0.0;

    public double getArmorRating()          { return armorRating; }
    public void   setArmorRating(double v)  { this.armorRating = v; }
}
