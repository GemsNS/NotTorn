package nottorn.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable-read record of a completed combat encounter.
 *
 * The engine appends string entries during the fight; callers read them
 * back through {@link #getEntries()} for display in the Renderer event log.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class CombatLog {

    private final List<String> entries = new ArrayList<>();

    private boolean       attackerWon;
    private CombatOutcome outcome      = CombatOutcome.LEAVE;
    private long          totalDamageByAttacker = 0L;
    private long          totalDamageByDefender = 0L;
    private int           turnsElapsed = 0;
    private String        attackerName;
    private String        defenderName;

    // ─────────────────────────────────────────────────────────────────────────
    // Called by CombatEngine during resolution
    // ─────────────────────────────────────────────────────────────────────────

    public void addEntry(String line)              { entries.add(line); }
    public void addDamageByAttacker(long dmg)      { totalDamageByAttacker += dmg; }
    public void addDamageByDefender(long dmg)      { totalDamageByDefender += dmg; }

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────

    public List<String> getEntries()           { return Collections.unmodifiableList(entries); }

    public boolean       isAttackerWon()       { return attackerWon; }
    public void          setAttackerWon(boolean v) { this.attackerWon = v; }

    public CombatOutcome getOutcome()          { return outcome; }
    public void          setOutcome(CombatOutcome v) { this.outcome = v; }

    public long getTotalDamageByAttacker()     { return totalDamageByAttacker; }
    public long getTotalDamageByDefender()     { return totalDamageByDefender; }

    public int  getTurnsElapsed()              { return turnsElapsed; }
    public void setTurnsElapsed(int v)         { this.turnsElapsed = v; }

    public String getAttackerName()            { return attackerName; }
    public void   setAttackerName(String v)    { this.attackerName = v; }

    public String getDefenderName()            { return defenderName; }
    public void   setDefenderName(String v)    { this.defenderName = v; }

    /** Convenience: returns the last N entries (for scrolling event log). */
    public List<String> tail(int n) {
        int size  = entries.size();
        int start = Math.max(0, size - n);
        return Collections.unmodifiableList(entries.subList(start, size));
    }
}
