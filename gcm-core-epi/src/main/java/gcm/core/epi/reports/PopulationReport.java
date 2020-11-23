package gcm.core.epi.reports;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsScope;
import gcm.output.reports.AbstractReport;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.simulation.ObservableEnvironment;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PopulationReport extends AbstractReport {

    private FipsScope fipsScope = FipsScope.TRACT;

    @Override
    public void init(ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        for (Object initialDatum : initialData) {
            if (initialDatum instanceof FipsScope) {
                this.fipsScope = (FipsScope) initialDatum;
                break;
            }
        }

        // Count population by FIPS code and age group
        final Map<FipsCode, Map<AgeGroup, Counter>> counters = new HashMap<>();
        PopulationDescription populationDescription = observableEnvironment.getGlobalPropertyValue(
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
            reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
            reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());
            reportItemBuilder.addValue(fipsCode.code());
            reportItemBuilder.addValue(ageGroup.name());
            reportItemBuilder.addValue(population.count);
            observableEnvironment.releaseOutputItem(reportItemBuilder.build());
        }));
    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        // Only produces output at the beginning of the simulation so no observation
        return new LinkedHashSet<>();
    }

    private final static class Counter {
        int count;
    }
}
