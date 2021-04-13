package gcm.core.epi.reports;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsScope;
import nucleus.ReportContext;
import plugins.globals.datacontainers.GlobalDataView;
import plugins.reports.support.AbstractReport;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PopulationReport extends AbstractReport {

    private FipsScope fipsScope = FipsScope.TRACT;

    @Override
    public void setInitializingData(Set<Object> initialData) {
        for (Object initialDatum : initialData) {
            if (initialDatum instanceof FipsScope) {
                this.fipsScope = (FipsScope) initialDatum;
                break;
            }
        }
    }

    @Override
    public void init(ReportContext reportContext) {
        super.init(reportContext);

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
            reportItemBuilder.setReportType(getClass());
            reportItemBuilder.addValue(fipsCode.code());
            reportItemBuilder.addValue(ageGroup.name());
            reportItemBuilder.addValue(population.count);
            releaseOutputItem(reportItemBuilder.build());
        }));
    }

    private final static class Counter {
        int count;
    }
}
