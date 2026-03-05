package nottorn.combat;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Orchestrates a full combat encounter between two named combatants.
 *
 * Rules (from spec):
 *  - Maximum 25 turns per encounter.
 *  - Each turn: aggressor attacks first, then defender counter-attacks.
 *  - A turn is skipped for the defender if the defender's HP drops to 0
 *    mid-turn (attacker wins on that turn).
 *  - 12% base critical hit chance; boosted by merit / weapon attachments.
 *  - Hitbox selected by weighted random from the 14 regions.
 *  - Critical hits to vital zones (Head/Throat/Heart) apply 3.5× multiplier.
 *  - Critical hits to non-vital zones apply 2.0× multiplier.
 *  - If neither combatant is dead after 25 turns the encounter is a DRAW
 *    (treated as a failed attack for the aggressor).
 *
 * Usage:
 * <pre>
 *   CombatEngine engine = new CombatEngine(config.combat.baseCritChance,
 *                                          config.combat.baseUnarmedDamage);
 *   CombatLog log = engine.fight("Player", playerStats, 1000,
 *                                "Thug",   npcStats,    500);
 * </pre>
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class CombatEngine {

    public static final int MAX_TURNS = 25;

    private final double baseCritChance;
    private final long   baseUnarmedDamage;
    private final Random rng;

    public CombatEngine(double baseCritChance, long baseUnarmedDamage) {
        this.baseCritChance    = baseCritChance;
        this.baseUnarmedDamage = baseUnarmedDamage;
        this.rng               = new Random();
    }

    public double getBaseCritChance() { return baseCritChance; }

    // ─────────────────────────────────────────────────────────────────────────
    // Main entry point
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves a full combat encounter and returns the complete log.
     *
     * @param attackerName    display name of the aggressor
     * @param attackerStats   battle stats of the aggressor
     * @param attackerHp      starting hit-points of the aggressor
     * @param attackerCrit    total crit chance override for aggressor (0.0–1.0)
     * @param defenderName    display name of the defender
     * @param defenderStats   battle stats of the defender
     * @param defenderHp      starting hit-points of the defender
     * @param defenderCrit    total crit chance override for defender (0.0–1.0)
     */
    public CombatLog fight(String attackerName, CharacterStats attackerStats, long attackerHp, double attackerCrit,
                           String defenderName, CharacterStats defenderStats, long defenderHp, double defenderCrit) {

        CombatLog log = new CombatLog();
        log.setAttackerName(attackerName);
        log.setDefenderName(defenderName);

        log.addEntry(String.format("=== COMBAT: %s vs %s ===", attackerName, defenderName));
        log.addEntry(String.format("  %s HP: %d  |  %s HP: %d",
                attackerName, attackerHp, defenderName, defenderHp));
        log.addEntry("");

        long atkHp = attackerHp;
        long defHp = defenderHp;
        int  turn  = 0;

        while (turn < MAX_TURNS && atkHp > 0 && defHp > 0) {
            turn++;
            log.addEntry(String.format("-- Turn %d --", turn));

            // Aggressor attacks defender
            long dmgToDefender = resolveSingleAttack(log,
                    attackerName, attackerStats, attackerCrit,
                    defenderName, defenderStats);
            defHp -= dmgToDefender;
            log.addDamageByAttacker(dmgToDefender);
            log.addEntry(String.format("  %s HP remaining: %d", defenderName, Math.max(0, defHp)));

            if (defHp <= 0) break; // defender KO'd — skip counter-attack

            // Defender counter-attacks aggressor
            long dmgToAttacker = resolveSingleAttack(log,
                    defenderName, defenderStats, defenderCrit,
                    attackerName, attackerStats);
            atkHp -= dmgToAttacker;
            log.addDamageByDefender(dmgToAttacker);
            log.addEntry(String.format("  %s HP remaining: %d", attackerName, Math.max(0, atkHp)));
        }

        log.setTurnsElapsed(turn);
        log.addEntry("");

        if (defHp <= 0) {
            log.setAttackerWon(true);
            log.setOutcome(CombatOutcome.LEAVE); // default; caller may change
            log.addEntry(String.format(">>> %s WINS in %d turn(s)! <<<", attackerName, turn));
            log.addEntry(String.format("    Total damage dealt: %d", log.getTotalDamageByAttacker()));
        } else if (atkHp <= 0) {
            log.setAttackerWon(false);
            log.setOutcome(null);
            log.addEntry(String.format(">>> %s WINS in %d turn(s)! <<<", defenderName, turn));
        } else {
            log.setAttackerWon(false);
            log.setOutcome(null);
            log.addEntry(String.format(">>> DRAW — combat timed out after %d turns. <<<", MAX_TURNS));
        }

        return log;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Single-attack resolution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves one attack from attacker onto defender and appends events to log.
     *
     * @return HP damage dealt (0 if the attack misses)
     */
    private long resolveSingleAttack(CombatLog log,
                                     String attackerName, CharacterStats attackerStats, double attackerCrit,
                                     String defenderName, CharacterStats defenderStats) {

        BigDecimal atkSpd = attackerStats.getEffectiveSpeed();
        BigDecimal defDex = defenderStats.getEffectiveDexterity();
        double hitChance  = CombatCalculator.calculateHitChance(atkSpd, defDex);

        boolean hit = rng.nextDouble() < hitChance;

        if (!hit) {
            log.addEntry(String.format("  %s attacks %s... MISS  (hit%%=%.1f%%)",
                    attackerName, defenderName, hitChance * 100));
            return 0L;
        }

        // Hit — resolve hitbox, crit, mitigation, damage
        Hitbox  hitbox      = Hitbox.random(rng);
        boolean isCrit      = CombatCalculator.isCriticalHit(attackerCrit, rng);
        double  mitigation  = CombatCalculator.calculateDamageMitigation(
                defenderStats.getEffectiveDefense(),
                attackerStats.getEffectiveStrength());
        long    damage      = CombatCalculator.calculateDamage(
                baseUnarmedDamage, mitigation, hitbox, isCrit);

        String critTag = isCrit
                ? (hitbox.isVital() ? " [CRITICAL VITAL x3.5!]" : " [CRIT x2.0]")
                : "";
        log.addEntry(String.format("  %s hits %s's %s%s  — %d dmg  (mit=%.1f%%)",
                attackerName, defenderName,
                hitbox.name().replace('_', ' '),
                critTag, damage,
                mitigation * 100));

        return damage;
    }
}
