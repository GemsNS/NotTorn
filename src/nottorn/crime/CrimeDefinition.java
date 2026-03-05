package nottorn.crime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Static definition of a crime type, loaded from data/crimes.json.
 *
 * Actual success resolution is handled by {@link CrimeService}, which
 * applies player-specific modifiers (CE bonus, job perks, etc.) on top
 * of the base rates here.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrimeDefinition {

    public String id               = "";
    public String name             = "";
    public int    nerveCost        = 1;
    public double baseSuccessRate  = 0.5;
    public long   minReward        = 0;
    public long   maxReward        = 100;
    public long   ceGain           = 1;
    public long   cePenalty        = 1;
    public int    minJailSeconds   = 0;
    public int    maxJailSeconds   = 0;

    /** One-line summary shown in the crime selection menu. */
    public String menuLine(double playerSuccessRate) {
        return String.format("%-16s  (%d nerve)  $%,d-$%,d  CE+%d  %.0f%% success",
                name, nerveCost, minReward, maxReward, ceGain,
                Math.min(1.0, playerSuccessRate) * 100);
    }
}
