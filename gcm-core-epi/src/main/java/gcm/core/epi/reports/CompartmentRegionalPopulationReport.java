package gcm.core.epi.reports;

import gcm.output.reports.PersonInfo;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.scenario.CompartmentId;
import gcm.scenario.PersonId;
import gcm.scenario.RegionId;
import gcm.simulation.ObservableEnvironment;
import gcm.util.annotations.Source;
import gcm.util.annotations.TestStatus;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A periodic Report that displays the number of people in each
 * region/compartment pair in a periodic manner. Only non-zero person counts are
 * reported. Adapted from CompartmentPopulationReport in GCM.
 * <p>
 * Fields
 * <p>
 * Region -- the region identifier via the region aggregation level
 * <p>
 * Compartment -- the compartment identifier
 * <p>
 * PersonCount -- the number of people in the region/compartment pair
 *
 * @author Shawn Hatch
 */
@Source(status = TestStatus.UNEXPECTED)
public final class CompartmentRegionalPopulationReport extends RegionAggregationPeriodicReport {

    /*
     * (region, compartment) pairs mapped to population counters
     */
    private final Map<String, Map<CompartmentId, Counter>> regionMap = new LinkedHashMap<>();
    /*
     * The header for the report
     */
    private ReportHeader reportHeader;

    /*
     * Returns the report header and builds it if it is null.
     */
    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeader = reportHeaderBuilder
                    .add("Region")
                    .add("Compartment")
                    .add("PersonCount")
                    .build();
        }
        return reportHeader;

    }

    /*
     * Decrement the number of people in the region/compartment pair by 1
     */
    private void decrement(final RegionId regionId, final CompartmentId compartmentId) {
        String fipsString = getFipsString(regionId);
        final Counter counter = regionMap.get(fipsString).get(compartmentId);
        counter.count--;
    }

    @Override
    protected void flush(ObservableEnvironment observableEnvironment) {
        final ReportItem.Builder reportItemBuilder = ReportItem.builder();
        /*
         * Report the population count for all region/compartment pairs that are
         * not empty
         */
        for (final String regionId : regionMap.keySet()) {
            final Map<CompartmentId, Counter> compartmentMap = regionMap.get(regionId);
            for (final CompartmentId compartmentId : compartmentMap.keySet()) {
                final Counter counter = compartmentMap.get(compartmentId);
                final int personCount = counter.count;
                if (personCount > 0) {
                    reportItemBuilder.setReportHeader(getReportHeader());
                    reportItemBuilder.setReportType(getClass());
                    reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                    reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());
                    buildTimeFields(reportItemBuilder);

                    reportItemBuilder.addValue(regionId);
                    reportItemBuilder.addValue(compartmentId.toString());
                    reportItemBuilder.addValue(personCount);
                    observableEnvironment.releaseOutputItem(reportItemBuilder.build());
                }
            }
        }
    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        final Set<StateChange> result = new LinkedHashSet<>();
        result.add(StateChange.PERSON_ADDITION);
        result.add(StateChange.PERSON_REMOVAL);
        result.add(StateChange.COMPARTMENT_ASSIGNMENT);
        result.add(StateChange.REGION_ASSIGNMENT);
        return result;
    }

    @Override
    public void handleRegionAssignment(ObservableEnvironment observableEnvironment, final PersonId personId, final RegionId sourceRegionId) {
        setCurrentReportingPeriod(observableEnvironment);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
        final RegionId destinationRegionId = observableEnvironment.getPersonRegion(personId);
        decrement(sourceRegionId, compartmentId);
        increment(destinationRegionId, compartmentId);
    }

    @Override
    public void handleCompartmentAssignment(ObservableEnvironment observableEnvironment, final PersonId personId, final CompartmentId sourceCompartmentId) {

        setCurrentReportingPeriod(observableEnvironment);
        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId destinationCompartmentId = observableEnvironment.getPersonCompartment(personId);
        decrement(regionId, sourceCompartmentId);
        increment(regionId, destinationCompartmentId);
    }

    @Override
    public void handlePersonAddition(ObservableEnvironment observableEnvironment, final PersonId personId) {
        setCurrentReportingPeriod(observableEnvironment);
        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
        increment(regionId, compartmentId);
    }

    @Override
    public void handlePersonRemoval(ObservableEnvironment observableEnvironment, PersonInfo personInfo) {
        setCurrentReportingPeriod(observableEnvironment);
        decrement(personInfo.getRegionId(), personInfo.getCompartmentId());
    }

    /*
     * Increment the number of people in the region/compartment pair by 1
     */
    private void increment(final RegionId regionId, final CompartmentId compartmentId) {
        String fipsString = getFipsString(regionId);
        final Counter counter = regionMap.get(fipsString).get(compartmentId);
        counter.count++;
    }

    @Override
    public void init(final ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        super.init(observableEnvironment, initialData);

        /*
         * Fill the region map with empty counters
         */
        final Set<CompartmentId> compartmentIds = observableEnvironment.getCompartmentIds();
        final Set<RegionId> regionIds = observableEnvironment.getRegionIds();
        for (final RegionId regionId : regionIds) {
            final Map<CompartmentId, Counter> compartmentMap = new LinkedHashMap<>();
            regionMap.put(getFipsString(regionId), compartmentMap);
            for (final CompartmentId compartmentId : compartmentIds) {
                final Counter counter = new Counter();
                compartmentMap.put(compartmentId, counter);
            }
        }

        setCurrentReportingPeriod(observableEnvironment);
        for (PersonId personId : observableEnvironment.getPeople()) {
            increment(observableEnvironment.getPersonRegion(personId), observableEnvironment.getPersonCompartment(personId));
        }
    }

    /*
     * Static class the represents the number of people in a
     * (region,compartment) pair
     */
    private static class Counter {
        int count;
    }

}