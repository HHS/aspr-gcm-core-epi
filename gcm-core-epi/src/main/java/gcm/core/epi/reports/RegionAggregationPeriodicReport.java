package gcm.core.epi.reports;

import gcm.core.epi.propertytypes.FipsScope;
import gcm.output.reports.commonreports.PeriodicReport;
import gcm.scenario.RegionId;
import gcm.simulation.ObservableEnvironment;

import java.util.Set;

public abstract class RegionAggregationPeriodicReport extends PeriodicReport {

    private FipsScope fipsScope = FipsScope.TRACT;

    protected String getFipsString(RegionId regionId) {
        return fipsScope.getFipsSubCode(regionId).code();
    }

    @Override
    public void init(ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        super.init(observableEnvironment, initialData);

        for (Object initialDatum : initialData) {
            if (initialDatum instanceof FipsScope) {
                this.fipsScope = (FipsScope) initialDatum;
                break;
            }
        }
    }

}
