package nottorn.world;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single tutorial mission, loaded from data/missions.json.
 *
 * Missions are ordered and linear: completing m01 unlocks m02, etc.
 * All Phase 5 objectives are VISIT_LOCATION; additional types (BUY_ITEM,
 * TRAIN_STAT, COMMIT_CRIME) are reserved for Phase 6.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TutorialMission {

    private String id;
    private String title;
    private String giverNpcId;
    private String dialogue;               // shown when player talks to giver
    private String objectiveDescription;   // short "go do X" text
    private String objectiveType;          // VISIT_LOCATION | BUY_ITEM | ...
    private String objectiveTarget;        // location ID, item ID, etc.
    private long   rewardFiat;
    private String completionDialogue;     // shown when objective is met

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getId()                        { return id; }
    public void   setId(String v)                { this.id = v; }

    public String getTitle()                     { return title; }
    public void   setTitle(String v)             { this.title = v; }

    public String getGiverNpcId()                { return giverNpcId; }
    public void   setGiverNpcId(String v)        { this.giverNpcId = v; }

    public String getDialogue()                  { return dialogue; }
    public void   setDialogue(String v)          { this.dialogue = v; }

    public String getObjectiveDescription()      { return objectiveDescription; }
    public void   setObjectiveDescription(String v) { this.objectiveDescription = v; }

    public String getObjectiveType()             { return objectiveType; }
    public void   setObjectiveType(String v)     { this.objectiveType = v; }

    public String getObjectiveTarget()           { return objectiveTarget; }
    public void   setObjectiveTarget(String v)   { this.objectiveTarget = v; }

    public long   getRewardFiat()                { return rewardFiat; }
    public void   setRewardFiat(long v)          { this.rewardFiat = v; }

    public String getCompletionDialogue()        { return completionDialogue; }
    public void   setCompletionDialogue(String v){ this.completionDialogue = v; }
}
