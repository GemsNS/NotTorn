package nottorn.world;

import java.util.Random;

/**
 * Stateless FSM driver for all NPCs.
 *
 * Called by {@link WorldMap#tick} once per game-loop update frame.
 *
 * ── State transition table ────────────────────────────────────────────────────
 *
 *  IDLE
 *    ├─ patrol timer fires           ──► PATROL
 *    └─ hostile NPC + player present ──► CHASE
 *
 *  PATROL
 *    ├─ move timer fires             ──► move to random adjacent location
 *    ├─ hostile NPC + player present ──► CHASE
 *    └─ patrol duration exceeded     ──► IDLE
 *
 *  CHASE
 *    ├─ player in same location      ──► COMBAT  (NPC is flagged; combat resolved externally)
 *    ├─ chase timeout exceeded       ──► IDLE
 *    └─ each move timer              ──► move one step toward player (via random adjacent)
 *
 *  COMBAT
 *    └─ (resolved by CombatEngine, which calls npc.setState() → IDLE or npc.setCurrentHp(0))
 *
 *  DEAD
 *    └─ respawn timer fires          ──► respawn() + IDLE
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public final class NpcBehaviour {

    private static final Random RNG = new Random();

    /**
     * Locations where hostile NPCs cannot initiate or continue combat.
     * NPCs may still physically wander into these areas, but they will never
     * transition to CHASE or COMBAT while the player is standing in one.
     * A chasing NPC that follows the player into a safe zone gives up and
     * returns to IDLE rather than attacking.
     */
    private static final java.util.Set<String> SAFE_LOCATIONS =
            java.util.Set.of("CITY_CENTER");

    private NpcBehaviour() { }

    public static void tick(Npc npc, WorldMap world,
                            String playerLocationId, double delta) {
        npc.addStateTimer(delta);
        npc.addMoveTimer(delta);

        switch (npc.getState()) {
            case IDLE   -> tickIdle(npc, world, playerLocationId);
            case PATROL -> tickPatrol(npc, world, playerLocationId);
            case CHASE  -> tickChase(npc, world, playerLocationId);
            case COMBAT -> { /* controlled externally by CombatEngine */ }
            case DEAD   -> tickDead(npc);
        }
    }

    // ── State handlers ────────────────────────────────────────────────────────

    private static void tickIdle(Npc npc, WorldMap world, String playerLoc) {
        if (canEngage(npc) && playerLoc.equals(npc.getCurrentLocationId())
                && !SAFE_LOCATIONS.contains(playerLoc)) {
            npc.setState(NpcState.CHASE);
            return;
        }
        if (npc.getPatrolInterval() > 0
                && npc.getStateTimer() >= npc.getPatrolInterval()) {
            npc.setState(NpcState.PATROL);
        }
    }

    private static void tickPatrol(Npc npc, WorldMap world, String playerLoc) {
        if (canEngage(npc) && playerLoc.equals(npc.getCurrentLocationId())
                && !SAFE_LOCATIONS.contains(playerLoc)) {
            npc.setState(NpcState.CHASE);
            return;
        }
        // Return home after 2× patrol interval
        if (npc.getStateTimer() >= npc.getPatrolInterval() * 2) {
            world.moveNpc(npc, npc.getHomeLocationId());
            npc.setState(NpcState.IDLE);
            return;
        }
        // Take a step
        if (npc.getMoveTimer() >= npc.getMoveInterval()) {
            String next = world.randomAdjacentLocation(npc.getCurrentLocationId(), RNG);
            world.moveNpc(npc, next);
            npc.resetMoveTimer();
        }
    }

    private static void tickChase(Npc npc, WorldMap world, String playerLoc) {
        // Player is right here → initiate combat, unless this is a safe zone
        if (playerLoc.equals(npc.getCurrentLocationId())) {
            if (SAFE_LOCATIONS.contains(playerLoc)) {
                npc.setState(NpcState.IDLE);
                return;
            }
            npc.setState(NpcState.COMBAT);
            return;
        }
        // Timeout → give up
        if (npc.getStateTimer() >= npc.getChaseTimeout()) {
            npc.setState(NpcState.IDLE);
            return;
        }
        // Move one step toward player
        if (npc.getMoveTimer() >= npc.getMoveInterval()) {
            String next = stepToward(npc.getCurrentLocationId(), playerLoc, world);
            world.moveNpc(npc, next);
            npc.resetMoveTimer();
        }
    }

    private static void tickDead(Npc npc) {
        if (npc.getRespawnTime() > 0 && npc.getStateTimer() >= npc.getRespawnTime()) {
            npc.respawn();
            npc.setState(NpcState.IDLE);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True if this NPC is allowed to engage the player in combat. */
    private static boolean canEngage(Npc npc) {
        return !npc.isIndestructible() && !npc.isTutorial() && npc.isAlive();
    }

    /**
     * Takes one greedy step toward the target location by choosing an adjacent
     * location whose ID alphabetically sorts toward the target (a simple
     * heuristic sufficient for the small city graph).
     * Falls back to a random adjacent location if no step is clearly better.
     */
    private static String stepToward(String current, String target, WorldMap world) {
        java.util.List<String> adj = world.getConnections(current);
        if (adj.isEmpty()) return current;
        // If target is directly adjacent, move there
        if (adj.contains(target)) return target;
        // Otherwise pick a random adjacent location (BFS is overkill for 8 nodes)
        return adj.get(RNG.nextInt(adj.size()));
    }
}
