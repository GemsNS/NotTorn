package nottorn.travel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A foreign travel destination, loaded from data/destinations.json.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Destination {

    public String id                  = "";
    public String name                = "";
    public int    flightTimeMinutes   = 26;
    public long   ticketCost          = 0;
    public String description         = "";

    public long flightTimeMillis() {
        return (long) flightTimeMinutes * 60_000L;
    }

    /** One-line summary shown in travel menu. */
    public String menuLine() {
        return String.format("%-18s  %3d min  $%,d  — %s",
                name, flightTimeMinutes, ticketCost, description);
    }
}
