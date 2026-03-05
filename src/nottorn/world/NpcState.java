package nottorn.world;

/**
 * FSM states for all non-player characters.
 *
 * Transitions (driven by NpcBehaviour.tick):
 *   IDLE    ──(patrol timer)──► PATROL
 *   PATROL  ──(player nearby)─► CHASE
 *   IDLE    ──(player nearby)─► CHASE   (non-tutorial, non-indestructible only)
 *   CHASE   ──(player in range)► COMBAT
 *   CHASE   ──(lost player)───► IDLE
 *   COMBAT  ──(resolved by CombatEngine)─► IDLE | DEAD
 *   DEAD    ──(respawn timer)──► IDLE
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public enum NpcState {
    IDLE,
    PATROL,
    CHASE,
    COMBAT,
    DEAD
}
