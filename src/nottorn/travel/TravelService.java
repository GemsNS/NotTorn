package nottorn.travel;

import nottorn.model.Player;

/**
 * Manages the player travel state machine.
 *
 * Constraints (from spec):
 *  - Player must not be in jail or already traveling.
 *  - Player must have enough cash for the ticket.
 *  - During travel the player is locked out of crimes and combat.
 *  - On arrival the player's location updates to the destination (string ID).
 *  - Return flight always goes back to CITY_CENTER with the same duration.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class TravelService {

    private static final String HOME_LOCATION = "CITY_CENTER";

    private final TravelCatalog catalog;

    public TravelService(TravelCatalog catalog) {
        this.catalog = catalog;
    }

    // ── Departure ─────────────────────────────────────────────────────────────

    /**
     * Starts a trip to the given destination.
     *
     * @return result message for the event log
     */
    public String depart(Player player, String destinationId, long nowMs) {
        if (player.isInJail())    return "[Travel] Cannot travel while in jail.";
        if (player.isTraveling()) return "[Travel] You are already en route to "
                                         + player.getTravelDestinationId() + ".";

        Destination dest = catalog.get(destinationId);
        if (dest == null) return "[Travel] Unknown destination: " + destinationId;

        if (player.getCash() < dest.ticketCost)
            return String.format("[Travel] Need $%,d for the %s ticket. You have $%,d.",
                    dest.ticketCost, dest.name, player.getCash());

        player.setCash(player.getCash() - dest.ticketCost);
        player.setTraveling(true);
        player.setTravelDestinationId(destinationId);
        player.setTravelArrivalTimestamp(nowMs + dest.flightTimeMillis());
        player.setCurrentLocationId("IN_FLIGHT");

        return String.format("[Travel] Departed for %s. Flight: %d min. ETA: %s. Ticket: -$%,d.",
                dest.name, dest.flightTimeMinutes,
                etaString(player.getTravelArrivalTimestamp()), dest.ticketCost);
    }

    // ── Arrival polling (called every game tick) ──────────────────────────────

    /**
     * Checks whether the player has arrived.  If so, updates their location
     * and clears travel state.
     *
     * @return arrival message, or null if not yet arrived
     */
    public String checkArrival(Player player, long nowMs) {
        if (!player.isTraveling()) return null;
        if (nowMs < player.getTravelArrivalTimestamp()) return null;

        String destId = player.getTravelDestinationId();
        Destination dest = catalog.get(destId);
        String destName = (dest != null) ? dest.name : destId;

        player.setTraveling(false);
        player.setTravelDestinationId(null);
        player.setCurrentLocationId(destId);

        boolean returningHome = HOME_LOCATION.equals(destId);
        if (returningHome) {
            return "[Travel] Landed back in Torn City. Welcome home!";
        }
        return "[Travel] Arrived in " + destName + "!  Press [T] or [R] to book your return flight.";
    }

    /** Starts the return journey back to Torn City. */
    public String returnHome(Player player, long nowMs) {
        if (!player.isTraveling() && !isAbroad(player))
            return "[Travel] You are already in Torn City.";
        if (player.isInJail())
            return "[Travel] Cannot travel while in jail.";
        if (player.isTraveling())
            return "[Travel] You are already in flight.";

        String currentDest = player.getCurrentLocationId();
        Destination dest = catalog.get(currentDest);
        long flightMs = (dest != null) ? dest.flightTimeMillis() : 26L * 60_000L;

        player.setTraveling(true);
        player.setTravelDestinationId(HOME_LOCATION);
        player.setTravelArrivalTimestamp(nowMs + flightMs);
        player.setCurrentLocationId("IN_FLIGHT");

        return String.format("[Travel] Return flight to Torn City. ETA: %s.",
                etaString(player.getTravelArrivalTimestamp()));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isAbroad(Player player) {
        String loc = player.getCurrentLocationId();
        return loc != null && !loc.equals(HOME_LOCATION) && !loc.equals("IN_FLIGHT")
               && catalog.get(loc) != null;
    }

    public TravelCatalog getCatalog() { return catalog; }

    /** Remaining flight seconds, or 0 if not traveling. */
    public long secondsRemaining(Player player, long nowMs) {
        if (!player.isTraveling()) return 0;
        return Math.max(0, (player.getTravelArrivalTimestamp() - nowMs) / 1_000L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String etaString(long arrivalMs) {
        long remaining = Math.max(0, (arrivalMs - System.currentTimeMillis()) / 1_000L);
        long m = remaining / 60, s = remaining % 60;
        return m > 0 ? m + "m " + s + "s" : s + "s";
    }
}
