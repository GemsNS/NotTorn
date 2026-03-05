package nottorn.world;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * A named node in the world-map graph, loaded from data/world.json.
 * Connections are stored as String IDs so new locations can be added
 * from JSON without recompiling.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {

    private String       id;
    private String       name;
    private String       description;
    private List<String> connectedTo = new ArrayList<>();

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String       getId()                    { return id; }
    public void         setId(String v)            { this.id = v; }

    public String       getName()                  { return name; }
    public void         setName(String v)          { this.name = v; }

    public String       getDescription()           { return description; }
    public void         setDescription(String v)   { this.description = v; }

    public List<String> getConnectedTo()           { return connectedTo; }
    public void         setConnectedTo(List<String> v) { this.connectedTo = v; }

    public boolean      isConnectedTo(String locId) {
        return connectedTo.contains(locId);
    }

    @Override
    public String toString() {
        return name + " [" + id + "]";
    }
}
