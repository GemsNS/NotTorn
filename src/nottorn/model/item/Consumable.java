package nottorn.model.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nottorn.model.Player;

/**
 * A single-use consumable item.
 *
 * {@link #applyTo(Player)} mutates the player state according to the effect
 * type and value, respecting all hard caps defined on the Player / spec:
 *  - RESTORE_ENERGY   : adds effectValue, capped at absoluteMaxEnergy (1,000)
 *  - RESTORE_HAPPINESS: adds effectValue, capped at absoluteMaxHappiness (99,999)
 *  - RESTORE_HP       : adds effectValue, capped at maxHitPoints
 *  - RESTORE_NERVE    : adds effectValue, capped at naturalNerveBar
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Consumable extends Item {

    private ConsumableEffect effect;
    private double           effectValue;

    // ── Getters / Setters ────────────────────────────────────────────────────

    public ConsumableEffect getEffect()              { return effect; }
    public void             setEffect(ConsumableEffect v) { this.effect = v; }

    public double           getEffectValue()         { return effectValue; }
    public void             setEffectValue(double v) { this.effectValue = v; }

    // ── Application ──────────────────────────────────────────────────────────

    /**
     * Applies this consumable's effect to the player.
     *
     * @return human-readable description of what changed
     */
    public String applyTo(Player player) {
        if (effect == null) return "No effect.";

        switch (effect) {
            case RESTORE_ENERGY: {
                double before = player.getEnergy();
                double after  = Math.min(player.getAbsoluteMaxEnergy(),
                                         before + effectValue);
                player.setEnergy(after);
                return String.format("Energy: +%.0f  (%.0f → %.0f / %d)",
                        after - before, before, after, player.getAbsoluteMaxEnergy());
            }
            case RESTORE_HAPPINESS: {
                double before = player.getHappiness();
                double after  = Math.min(player.getAbsoluteMaxHappiness(),
                                         before + effectValue);
                player.setHappiness(after);
                return String.format("Happiness: +%.0f  (%.0f → %.0f)",
                        after - before, before, after);
            }
            case RESTORE_HP: {
                long before = player.getCurrentHitPoints();
                long after  = Math.min(player.getMaxHitPoints(),
                                       before + (long) effectValue);
                player.setCurrentHitPoints(after);
                return String.format("HP: +%d  (%d → %d / %d)",
                        after - before, before, after, player.getMaxHitPoints());
            }
            case RESTORE_NERVE: {
                double before = player.getNerve();
                double after  = Math.min(player.getNaturalNerveBar(),
                                         before + effectValue);
                player.setNerve(after);
                return String.format("Nerve: +%.0f  (%.0f → %.0f / %d)",
                        after - before, before, after, player.getNaturalNerveBar());
            }
            default:
                return "Effect not yet implemented.";
        }
    }
}
