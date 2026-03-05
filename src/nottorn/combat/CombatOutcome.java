package nottorn.combat;

/**
 * The four post-victory outcomes the attacker can select, each applying a
 * different mechanical penalty to the defeated defender.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public enum CombatOutcome {

    /**
     * Minimal hospital time (15–30 min) for the defender.
     * Yields maximum experience for the attacker.
     */
    LEAVE("Leave", "Defender hospitalized 15-30 min. Max XP gain."),

    /**
     * Defender hospitalized 30–45 min; attacker steals 5–15% of unbanked cash.
     */
    MUG("Mug", "Defender hospitalized 30-45 min. Steal 5-15% cash."),

    /**
     * Maximum hospitalization (3–3.5 hr). Minimum XP for attacker.
     * Primary faction-war denial tool.
     */
    HOSPITALIZE("Hospitalize", "Defender hospitalized 3-3.5 hr. Min XP gain."),

    /**
     * Restricted to Detective Agency employees (3-star+).
     * Sends defender to jail; attacker receives a cash bounty.
     */
    ARREST("Arrest", "Defender jailed. Attacker earns cash bounty. [Requires Detective job]");

    // ─────────────────────────────────────────────────────────────────────────

    public final String displayName;
    public final String description;

    CombatOutcome(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
