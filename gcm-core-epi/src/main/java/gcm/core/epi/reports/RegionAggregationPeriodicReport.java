package gcm.core.epi.reports;

import gcm.core.epi.propertytypes.FipsScope;
import plugins.regions.support.RegionId;
import plugins.reports.support.PeriodicReport;
import plugins.reports.support.ReportPeriod;

public abstract class RegionAggregationPeriodicReport extends PeriodicReport {

    private final FipsScope fipsScope;

    public RegionAggregationPeriodicReport(ReportPeriod reportPeriod, FipsScope fipsScope) {
        super(reportPeriod);
        this.fipsScope = fipsScope;
    }

    protected String getFipsString(RegionId regionId) {
        return fipsScope.getFipsSubCode(regionId).code();
    }

}
