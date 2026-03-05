package nottorn.world;

import nottorn.combat.CharacterStats;

import java.math.BigInteger;

/**
 * A live NPC instance in the world.  Created from an {@link NpcTemplate}
 * at startup; its FSM state is mutated by {@link NpcBehaviour} every tick.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class Npc {

    private final String id;
    private final String name;
    private final String title;
    private final String homeLocationId;
    private final boolean indestructible;
    private final boolean tutorial;
    private final double  patrolInterval;   // seconds between patrol moves
    private final double  moveInterval;     // seconds per single step while patrolling
    private final double  respawnTime;
    private final double  chaseTimeout;
    private final double  critChance;

    private String         currentLocationId;
    private NpcState       state;
    private long           maxHp;
    private long           currentHp;
    private CharacterStats stats;

    // FSM timers (accumulate delta-time)
    private double stateTimer      = 0;   // time in current state
    private double moveTimer       = 0;   // time since last step

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Npc fromTemplate(NpcTemplate t) {
        CharacterStats cs = new CharacterStats();
        cs.setStrength (BigInteger.valueOf(t.strength));
        cs.setDefense  (BigInteger.valueOf(t.defense));
        cs.setSpeed    (BigInteger.valueOf(t.speed));
        cs.setDexterity(BigInteger.valueOf(t.dexterity));

        NpcState initial = NpcState.valueOf(t.initialState.toUpperCase());

        return new Npc(t.id, t.name, t.title, t.homeLocationId,
                       t.indestructible, t.isTutorial,
                       t.patrolIntervalSeconds, t.moveIntervalSeconds,
                       t.respawnTimeSeconds, t.chaseTimeoutSeconds,
                       t.critChance,
                       t.homeLocationId, initial, t.maxHp, cs);
    }

    private Npc(String id, String name, String title, String homeLocationId,
                boolean indestructible, boolean tutorial,
                double patrolInterval, double moveInterval,
                double respawnTime, double chaseTimeout, double critChance,
                String startLocation, NpcState startState,
                long maxHp, CharacterStats stats) {
        this.id                 = id;
        this.name               = name;
        this.title              = title;
        this.homeLocationId     = homeLocationId;
        this.indestructible     = indestructible;
        this.tutorial           = tutorial;
        this.patrolInterval     = patrolInterval;
        this.moveInterval       = moveInterval;
        this.respawnTime        = respawnTime;
        this.chaseTimeout       = chaseTimeout;
        this.critChance         = critChance;
        this.currentLocationId  = startLocation;
        this.state              = startState;
        this.maxHp              = maxHp;
        this.currentHp          = maxHp;
        this.stats              = stats;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String        getId()                    { return id; }
    public String        getName()                  { return name; }
    public String        getTitle()                 { return title; }
    public String        getHomeLocationId()        { return homeLocationId; }
    public boolean       isIndestructible()         { return indestructible; }
    public boolean       isTutorial()               { return tutorial; }
    public double        getPatrolInterval()        { return patrolInterval; }
    public double        getMoveInterval()          { return moveInterval; }
    public double        getRespawnTime()           { return respawnTime; }
    public double        getChaseTimeout()          { return chaseTimeout; }
    public double        getCritChance()            { return critChance; }

    public String        getCurrentLocationId()     { return currentLocationId; }
    public void          setCurrentLocationId(String v) { this.currentLocationId = v; }

    public NpcState      getState()                 { return state; }
    public void          setState(NpcState v)       { this.state = v; stateTimer = 0; moveTimer = 0; }

    public long          getMaxHp()                 { return maxHp; }
    public long          getCurrentHp()             { return currentHp; }
    public void          setCurrentHp(long v)       { this.currentHp = Math.max(0, Math.min(maxHp, v)); }
    public boolean       isAlive()                  { return currentHp > 0; }

    public CharacterStats getStats()                { return stats; }

    public double        getStateTimer()            { return stateTimer; }
    public void          addStateTimer(double d)    { stateTimer += d; }

    public double        getMoveTimer()             { return moveTimer; }
    public void          addMoveTimer(double d)     { moveTimer += d; }
    public void          resetMoveTimer()           { moveTimer = 0; }

    /** Fully restores HP and moves the NPC back to its home location. */
    public void respawn() {
        currentHp         = maxHp;
        currentLocationId = homeLocationId;
        stateTimer        = 0;
        moveTimer         = 0;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) [%s] @ %s HP:%d/%d",
                name, title, state, currentLocationId, currentHp, maxHp);
    }
}
