package gcm.core.epi.reports;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsScope;
import nucleus.ReportContext;
import plugins.globals.datacontainers.GlobalDataView;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;

import java.util.HashMap;
import java.util.Map;

public class PopulationReport {

    private final FipsScope fipsScope;

    public PopulationReport(FipsScope fipsScope) {
        this.fipsScope = fipsScope;
    }

    public void init(ReportContext reportContext) {
        GlobalDataView globalDataView = reportContext.getDataView(GlobalDataView.class).get();

        // Count population by FIPS code and age group
        final Map<FipsCode, Map<AgeGroup, Counter>> counters = new HashMap<>();
        PopulationDescription populationDescription = globalDataView.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);

        Counter personIndexCounter = new Counter();
        populationDescription.regionByPersonId().forEach(
                regionId -> {
                    FipsCode fipsCode = fipsScope.getFipsSubCode(regionId);
                    AgeGroup ageGroup = populationDescription.ageGroupPartition().getAgeGroupFromIndex(
                            populationDescription.ageGroupIndexByPersonId().get(personIndexCounter.count));
                    Map<AgeGroup, Counter> populationByAge = counters.computeIfAbsent(fipsCode, key -> new HashMap<>());
                    populationByAge.computeIfAbsent(ageGroup, key -> new Counter()).count++;
                    personIndexCounter.count++;
                }
        );

        // Release report items
        final ReportItem.Builder reportItemBuilder = ReportItem.builder();
        ReportHeader reportHeader = ReportHeader.builder()
                .add("Region")
                .add("AgeGroup")
                .add("Population")
                .build();
        counters.forEach((fipsCode, populationByAge) -> populationByAge.forEach((ageGroup, population) -> {
            reportItemBuilder.setReportHeader(reportHeader);
            reportItemBuilder.setReportId(reportContext.getCurrentReportId());
            reportItemBuilder.addValue(fipsCode.code());
            reportItemBuilder.addValue(ageGroup.name());
            reportItemBuilder.addValue(population.count);
            reportContext.releaseOutput(reportItemBuilder.build());
        }));
    }

    private final static class Counter {
        int count;
    }
}
