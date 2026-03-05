package nottorn.world;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The city graph plus all live NPC instances.
 *
 * Responsibilities:
 *  - Load locations from data/world.json
 *  - Instantiate Npc objects from NpcTemplates (data/npcs.json)
 *  - Provide graph queries (adjacency, reachable paths)
 *  - Dispatch per-tick NPC FSM updates via NpcBehaviour
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class WorldMap {

    private static final String WORLD_PATH = "data/world.json";
    private static final String NPCS_PATH  = "data/npcs.json";

    private final Map<String, Location> locations;
    private final Map<String, Npc>      npcs;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static WorldMap load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Locations
        LocationWrapper lw = mapper.readValue(new File(WORLD_PATH), LocationWrapper.class);
        Map<String, Location> locMap = new LinkedHashMap<>();
        if (lw.locations != null) {
            for (Location loc : lw.locations) locMap.put(loc.getId(), loc);
        }

        // NPCs
        NpcWrapper nw = mapper.readValue(new File(NPCS_PATH), NpcWrapper.class);
        Map<String, Npc> npcMap = new LinkedHashMap<>();
        if (nw.npcs != null) {
            for (NpcTemplate tpl : nw.npcs) {
                Npc npc = Npc.fromTemplate(tpl);
                npcMap.put(npc.getId(), npc);
            }
        }

        return new WorldMap(locMap, npcMap);
    }

    private WorldMap(Map<String, Location> locations, Map<String, Npc> npcs) {
        this.locations = locations;
        this.npcs      = npcs;
    }

    // ── Tick (called by GameEngine each update frame) ─────────────────────────

    /**
     * Advances all NPC FSMs by deltaSeconds.
     *
     * @param playerLocationId the location the player is currently in
     */
    public void tick(double deltaSeconds, String playerLocationId) {
        for (Npc npc : npcs.values()) {
            NpcBehaviour.tick(npc, this, playerLocationId, deltaSeconds);
        }
    }

    // ── Location graph queries ─────────────────────────────────────────────────

    public Location getLocation(String id)   { return locations.get(id); }
    public boolean  hasLocation(String id)   { return locations.containsKey(id); }

    public Collection<Location> allLocations() { return locations.values(); }

    public List<String> getConnections(String locationId) {
        Location loc = locations.get(locationId);
        return (loc == null) ? Collections.emptyList() : loc.getConnectedTo();
    }

    /**
     * Returns a random adjacent location ID, or the current one if isolated.
     */
    public String randomAdjacentLocation(String currentLocationId, Random rng) {
        List<String> conn = getConnections(currentLocationId);
        if (conn.isEmpty()) return currentLocationId;
        return conn.get(rng.nextInt(conn.size()));
    }

    // ── NPC queries ───────────────────────────────────────────────────────────

    public Npc              getNpc(String id)   { return npcs.get(id); }
    public Collection<Npc>  allNpcs()           { return npcs.values(); }

    public List<Npc> getNpcsAt(String locationId) {
        return npcs.values().stream()
                .filter(n -> locationId.equals(n.getCurrentLocationId()))
                .collect(Collectors.toList());
    }

    public List<Npc> getHostileNpcsAt(String locationId) {
        return getNpcsAt(locationId).stream()
                .filter(n -> !n.isIndestructible() && !n.isTutorial()
                          && n.getState() != NpcState.DEAD)
                .collect(Collectors.toList());
    }

    // ── NPC movement helper (called by NpcBehaviour) ──────────────────────────

    public void moveNpc(Npc npc, String targetLocationId) {
        if (hasLocation(targetLocationId)) {
            npc.setCurrentLocationId(targetLocationId);
        }
    }

    // ── Jackson wrappers ──────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LocationWrapper {
        public List<Location> locations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NpcWrapper {
        public List<NpcTemplate> npcs;
    }
}
