package gcm.core.epi.reports;

import gcm.core.epi.propertytypes.FipsScope;
import nucleus.ReportContext;
import plugins.personproperties.support.PersonPropertyId;
import plugins.regions.support.RegionId;
import plugins.reports.support.PeriodicReport;

import java.util.Set;

public abstract class RegionAggregationPeriodicReport extends PeriodicReport {

    private FipsScope fipsScope = FipsScope.TRACT;

    protected String getFipsString(RegionId regionId) {
        return fipsScope.getFipsSubCode(regionId).code();
    }

    @Override
    public void setInitializingData(Set<Object> initialData) {
        for (Object initialDatum : initialData) {
            if (initialDatum instanceof FipsScope) {
                this.fipsScope = (FipsScope) initialDatum;
                break;
            }
        }
    }

}
