package nottorn.engine;

import nottorn.crime.CrimeCatalog;
import nottorn.crime.CrimeService;
import nottorn.economy.Shop;
import nottorn.jobs.JobCatalog;
import nottorn.jobs.JobService;
import nottorn.travel.TravelCatalog;
import nottorn.travel.TravelService;
import nottorn.world.TutorialNpc;
import nottorn.world.WorldMap;

/**
 * Bundle of all Phase 5-7 services passed to {@link GameEngine} as one object.
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
public class GameServices {

    public final WorldMap      world;
    public final TutorialNpc   tutorialNpc;
    public final JobCatalog    jobCatalog;
    public final JobService    jobService;
    public final CrimeCatalog  crimeCatalog;
    public final CrimeService  crimeService;
    public final TravelCatalog travelCatalog;
    public final TravelService travelService;
    public final Shop          shop;

    public GameServices(WorldMap world, TutorialNpc tutorialNpc,
                        JobCatalog jobCatalog,
                        CrimeCatalog crimeCatalog,
                        TravelCatalog travelCatalog,
                        Shop shop) {
        this.world         = world;
        this.tutorialNpc   = tutorialNpc;
        this.jobCatalog    = jobCatalog;
        this.jobService    = new JobService(jobCatalog);
        this.crimeCatalog  = crimeCatalog;
        this.crimeService  = new CrimeService();
        this.travelCatalog = travelCatalog;
        this.travelService = new TravelService(travelCatalog);
        this.shop          = shop;
    }
}
