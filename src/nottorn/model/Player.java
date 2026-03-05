package nottorn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nottorn.combat.CharacterStats;

import java.util.HashSet;
import java.util.Set;

/**
 * Central player POJO.  Every field is serialised to/from savegame.json by
 * Jackson.  No game logic lives here — this is purely a data container.
 *
 * Design note: energy and nerve are stored as {@code double} so that
 * fractional tick accumulation can be persisted without rounding errors.
 * They are always displayed as floored integers in the UI.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {

    // ── Identity ─────────────────────────────────────────────────────────────
    private String name  = "Player";
    private int    level = 1;
    private long   fiatBalance = 1_000L;

    // ── Energy ───────────────────────────────────────────────────────────────
    /** Current energy level; may exceed naturalMaxEnergy via consumables. */
    private double energy                    = 100.0;
    /** Passive regen ceiling (100 standard, 150 donator). */
    private int    naturalMaxEnergy          = 100;
    /** Hard consumable cap (1 000). */
    private int    absoluteMaxEnergy         = 1_000;
    private boolean donator                  = false;
    /** Fractional seconds accrued toward the next +5 energy tick. */
    private double energyAccumulatedSeconds  = 0.0;

    // ── Nerve ────────────────────────────────────────────────────────────────
    private double nerve                    = 10.0;
    /** Natural Nerve Bar – scales with Crime Experience (CE). */
    private int    naturalNerveBar          = 10;
    /** Hidden backend metric that drives NNB step increases. */
    private long   crimeExperience          = 0L;
    private double nerveAccumulatedSeconds  = 0.0;

    // ── Happiness ────────────────────────────────────────────────────────────
    /** Current happiness; consumables can push this to 99 999. */
    private double happiness                        = 50.0;
    /** Derived from the player's current property tier. */
    private int    propertyMaxHappiness             = 50;
    /** Hard ceiling for consumable stacking. */
    private int    absoluteMaxHappiness             = 99_999;
    /**
     * Epoch-millis of the last XX:00/15/30/45 truncation boundary processed.
     * Used to skip re-processing the same boundary on the next render tick.
     */
    private long   lastHappinessTruncationTimestamp = 0L;

    // ── Working stats ────────────────────────────────────────────────────────
    private long manualLabor  = 0L;
    private long intelligence = 0L;
    private long endurance    = 0L;

    // ── Property ─────────────────────────────────────────────────────────────
    private String propertyTier = "STARTER";

    // ── Persistence ──────────────────────────────────────────────────────────
    // ── Inventory ────────────────────────────────────────────────────────────
    /** All items currently held by this player. */
    private Inventory inventory = new Inventory();

    // ── Battle stats ─────────────────────────────────────────────────────────
    /** BigInteger-backed Strength/Defense/Speed/Dexterity with merit multipliers. */
    private CharacterStats battleStats = new CharacterStats();

    /** Maximum hit-points (grows with level/items in later phases). */
    private long maxHitPoints     = 1_000L;
    /** Current hit-points — restored by medical items and hospital stays. */
    private long currentHitPoints = 1_000L;

    /** Flat critical hit chance bonus from merits / education (added to base 12%). */
    private double critChanceBonus = 0.0;

    // ── City job ─────────────────────────────────────────────────────────────
    /** Career id (ARMY / MEDICAL / EDUCATION / LAW), null = unemployed. */
    private String activeJobCareerId     = null;
    /** 0-based rank index within the active career. */
    private int    activeJobRankIndex    = 0;
    /** Epoch-millis of the last successful job tick (daily stat + income award). */
    private long   lastJobTickTimestamp  = 0L;

    // ── Jail / travel ─────────────────────────────────────────────────────────
    /** Epoch-millis when jail expires; 0 = not in jail. */
    private long    jailReleaseTimestamp    = 0L;
    /** True while the player is in flight. */
    private boolean traveling               = false;
    /** ID of the destination being traveled to (null when not traveling). */
    private String  travelDestinationId     = null;
    /** Epoch-millis when the current flight lands. */
    private long    travelArrivalTimestamp  = 0L;
    /** Base item carrying capacity for travel (expandable via suitcases etc.). */
    private int     cargoCapacity           = 5;

    // ── World / Tutorial ─────────────────────────────────────────────────────
    /** Location the player is currently in (matches Location.id from world.json). */
    private String      currentLocationId   = "CITY_CENTER";
    /** All location IDs the player has visited at least once (mission tracking). */
    private Set<String> visitedLocations    = new HashSet<>();
    /** Index into the ordered tutorial mission list (0 = first mission). */
    private int         missionIndex        = 0;

    // ── Persistence ──────────────────────────────────────────────────────────
    /** Epoch-millis written by SaveManager on each save; read by offline service at startup. */
    private long lastSavedTimestamp = 0L;

    // ════════════════════════════════════════════════════════════════════════
    // Getters and Setters
    // ════════════════════════════════════════════════════════════════════════

    public String getName()                  { return name; }
    public void   setName(String v)          { this.name = v; }

    public int  getLevel()                   { return level; }
    public void setLevel(int v)              { this.level = v; }

    public long getFiatBalance()             { return fiatBalance; }
    public void setFiatBalance(long v)       { this.fiatBalance = v; }

    // Energy
    public double getEnergy()                        { return energy; }
    public void   setEnergy(double v)                { this.energy = v; }

    public int  getNaturalMaxEnergy()                { return naturalMaxEnergy; }
    public void setNaturalMaxEnergy(int v)           { this.naturalMaxEnergy = v; }

    public int  getAbsoluteMaxEnergy()               { return absoluteMaxEnergy; }
    public void setAbsoluteMaxEnergy(int v)          { this.absoluteMaxEnergy = v; }

    public boolean isDonator()                       { return donator; }
    public void    setDonator(boolean v)             { this.donator = v; }

    public double getEnergyAccumulatedSeconds()      { return energyAccumulatedSeconds; }
    public void   setEnergyAccumulatedSeconds(double v) { this.energyAccumulatedSeconds = v; }

    // Nerve
    public double getNerve()                         { return nerve; }
    public void   setNerve(double v)                 { this.nerve = v; }

    public int  getNaturalNerveBar()                 { return naturalNerveBar; }
    public void setNaturalNerveBar(int v)            { this.naturalNerveBar = v; }

    public long getCrimeExperience()                 { return crimeExperience; }
    public void setCrimeExperience(long v)           { this.crimeExperience = v; }

    public double getNerveAccumulatedSeconds()       { return nerveAccumulatedSeconds; }
    public void   setNerveAccumulatedSeconds(double v) { this.nerveAccumulatedSeconds = v; }

    // Happiness
    public double getHappiness()                     { return happiness; }
    public void   setHappiness(double v)             { this.happiness = v; }

    public int  getPropertyMaxHappiness()            { return propertyMaxHappiness; }
    public void setPropertyMaxHappiness(int v)       { this.propertyMaxHappiness = v; }

    public int  getAbsoluteMaxHappiness()            { return absoluteMaxHappiness; }
    public void setAbsoluteMaxHappiness(int v)       { this.absoluteMaxHappiness = v; }

    public long getLastHappinessTruncationTimestamp()       { return lastHappinessTruncationTimestamp; }
    public void setLastHappinessTruncationTimestamp(long v) { this.lastHappinessTruncationTimestamp = v; }

    // Working stats
    public long getManualLabor()                     { return manualLabor; }
    public void setManualLabor(long v)               { this.manualLabor = v; }

    public long getIntelligence()                    { return intelligence; }
    public void setIntelligence(long v)              { this.intelligence = v; }

    public long getEndurance()                       { return endurance; }
    public void setEndurance(long v)                 { this.endurance = v; }

    // Property
    public String getPropertyTier()                  { return propertyTier; }
    public void   setPropertyTier(String v)          { this.propertyTier = v; }

    // Inventory
    public Inventory getInventory()              { return inventory; }
    public void      setInventory(Inventory v)   { this.inventory = v == null ? new Inventory() : v; }

    // Battle stats
    public CharacterStats getBattleStats()            { return battleStats; }
    public void           setBattleStats(CharacterStats v) { this.battleStats = v; }

    public long getMaxHitPoints()                     { return maxHitPoints; }
    public void setMaxHitPoints(long v)               { this.maxHitPoints = v; }

    public long getCurrentHitPoints()                 { return currentHitPoints; }
    public void setCurrentHitPoints(long v)           { this.currentHitPoints = Math.max(0, v); }

    public double getCritChanceBonus()                { return critChanceBonus; }
    public void   setCritChanceBonus(double v)        { this.critChanceBonus = v; }

    /** Total crit chance = base (from config) + bonus from merits/education. */
    public double getTotalCritChance(double baseCritChance) {
        return Math.min(1.0, baseCritChance + critChanceBonus);
    }

    // City job
    public String getActiveJobCareerId()                 { return activeJobCareerId; }
    public void   setActiveJobCareerId(String v)         { this.activeJobCareerId = v; }

    public int  getActiveJobRankIndex()                  { return activeJobRankIndex; }
    public void setActiveJobRankIndex(int v)             { this.activeJobRankIndex = v; }

    public long getLastJobTickTimestamp()                { return lastJobTickTimestamp; }
    public void setLastJobTickTimestamp(long v)          { this.lastJobTickTimestamp = v; }

    // Jail / travel
    public long    getJailReleaseTimestamp()             { return jailReleaseTimestamp; }
    public void    setJailReleaseTimestamp(long v)       { this.jailReleaseTimestamp = v; }
    public boolean isInJail() {
        return jailReleaseTimestamp > 0 && System.currentTimeMillis() < jailReleaseTimestamp;
    }

    public boolean isTraveling()                         { return traveling; }
    public void    setTraveling(boolean v)               { this.traveling = v; }

    public String  getTravelDestinationId()              { return travelDestinationId; }
    public void    setTravelDestinationId(String v)      { this.travelDestinationId = v; }

    public long    getTravelArrivalTimestamp()           { return travelArrivalTimestamp; }
    public void    setTravelArrivalTimestamp(long v)     { this.travelArrivalTimestamp = v; }

    public int     getCargoCapacity()                    { return cargoCapacity; }
    public void    setCargoCapacity(int v)               { this.cargoCapacity = v; }

    // World / Tutorial
    public String      getCurrentLocationId()          { return currentLocationId; }
    public void        setCurrentLocationId(String v)  {
        this.currentLocationId = v;
        if (v != null) visitedLocations.add(v);
    }

    public Set<String> getVisitedLocations()           { return visitedLocations; }
    public void        setVisitedLocations(Set<String> v) {
        this.visitedLocations = (v == null) ? new HashSet<>() : v;
    }

    public int  getMissionIndex()                      { return missionIndex; }
    public void setMissionIndex(int v)                 { this.missionIndex = v; }

    /** Convenience alias for fiatBalance (used by TutorialNpc and economy layer). */
    public long getCash()                              { return fiatBalance; }
    public void setCash(long v)                        { this.fiatBalance = v; }

    // Persistence
    public long getLastSavedTimestamp()              { return lastSavedTimestamp; }
    public void setLastSavedTimestamp(long v)        { this.lastSavedTimestamp = v; }
}
