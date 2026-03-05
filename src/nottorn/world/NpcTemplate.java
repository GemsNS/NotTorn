package nottorn.world;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw JSON template for an NPC, loaded from data/npcs.json.
 * Converted to a live {@link Npc} by {@link Npc#fromTemplate(NpcTemplate)}.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NpcTemplate {

    public String  id;
    public String  name;
    public String  title;
    public String  homeLocationId;
    public boolean isTutorial      = false;
    public boolean indestructible  = false;
    public String  initialState    = "IDLE";

    public long    maxHp           = 500;
    public long    strength        = 1_000_000;
    public long    defense         = 1_000_000;
    public long    speed           = 1_000_000;
    public long    dexterity       = 1_000_000;

    public double  patrolIntervalSeconds = 30;
    public double  moveIntervalSeconds   = 15;
    public double  respawnTimeSeconds    = 180;
    public double  chaseTimeoutSeconds   = 60;
    public double  critChance            = 0.12;
}
