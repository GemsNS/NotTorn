package nottorn.world;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import nottorn.model.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the ordered tutorial mission chain delivered by "George".
 *
 * Responsibilities:
 *  - Load all {@link TutorialMission} definitions from data/missions.json
 *  - Evaluate whether the player's active mission objective is satisfied
 *  - Award fiat rewards and advance the mission index on completion
 *  - Provide UI-facing dialogue strings at any point in the chain
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class TutorialNpc {

    private static final String MISSIONS_PATH = "data/missions.json";

    private final List<TutorialMission> missions;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static TutorialNpc load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper w = mapper.readValue(new File(MISSIONS_PATH), Wrapper.class);
        List<TutorialMission> list = (w.missions != null) ? w.missions : new ArrayList<>();
        return new TutorialNpc(list);
    }

    private TutorialNpc(List<TutorialMission> missions) {
        this.missions = missions;
    }

    // ── Core mission logic ────────────────────────────────────────────────────

    /**
     * Checks whether the active mission objective is satisfied, awards the
     * reward if so, advances the chain, and returns a dialogue line to display.
     *
     * Should be called when the player "talks" to George in CITY_CENTER.
     *
     * @return the dialogue string to show in the event log
     */
    public String interact(Player player) {
        int idx = player.getMissionIndex();
        if (idx >= missions.size()) {
            return "George: 'You've done it all, kid. The city is yours.'";
        }

        TutorialMission m = missions.get(idx);

        if (isObjectiveMet(m, player)) {
            // Award reward
            player.setCash(player.getCash() + m.getRewardFiat());
            player.setMissionIndex(idx + 1);
            return m.getCompletionDialogue() + " [+$" + m.getRewardFiat() + "]";
        } else {
            return m.getDialogue() + "  | Objective: " + m.getObjectiveDescription();
        }
    }

    /**
     * Automatically checks if the active mission was silently completed
     * (e.g. player visited a location without explicitly talking to George).
     * Returns a completion dialogue if just completed, null otherwise.
     */
    public String checkSilentCompletion(Player player) {
        int idx = player.getMissionIndex();
        if (idx >= missions.size()) return null;

        TutorialMission m = missions.get(idx);
        if (isObjectiveMet(m, player)) {
            player.setCash(player.getCash() + m.getRewardFiat());
            player.setMissionIndex(idx + 1);
            return "[Mission Complete] " + m.getTitle()
                   + "  " + m.getCompletionDialogue()
                   + " [+$" + m.getRewardFiat() + "]";
        }
        return null;
    }

    /** Returns the active mission's short objective text, or null if all done. */
    public String getActiveObjectiveText(Player player) {
        int idx = player.getMissionIndex();
        if (idx >= missions.size()) return null;
        TutorialMission m = missions.get(idx);
        return "[" + m.getTitle() + "] " + m.getObjectiveDescription();
    }

    // ── Objective evaluation ──────────────────────────────────────────────────

    private boolean isObjectiveMet(TutorialMission m, Player player) {
        if ("VISIT_LOCATION".equalsIgnoreCase(m.getObjectiveType())) {
            return player.getVisitedLocations().contains(m.getObjectiveTarget());
        }
        // Additional types (BUY_ITEM, TRAIN_STAT, COMMIT_CRIME) added in Phase 6
        return false;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public List<TutorialMission> getMissions() { return missions; }

    public TutorialMission getActiveMission(Player player) {
        int idx = player.getMissionIndex();
        return (idx < missions.size()) ? missions.get(idx) : null;
    }

    // ── Jackson wrapper ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Wrapper {
        public List<TutorialMission> missions;
    }
}
