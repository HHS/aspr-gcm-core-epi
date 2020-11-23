package gcm.core.epi.reports;

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
 * A periodic Report that displays the number of times a person transferred from
 * one compartment to another within a region. Only non-zero transfer counts are
 * reported. Adapted from CompartmentTransferReport in GCM.
 * <p>
 * <p>
 * Fields
 * <p>
 * Region -- the region identifier
 * <p>
 * SourceCompartment -- the source compartment identifier
 * <p>
 * DestinationCompartment -- the destination compartment property identifier
 * <p>
 * Transfers -- the number of transfers from the source compartment to the
 * destination compartment for people in the region
 *
 * @author Shawn Hatch
 */
@Source(status = TestStatus.UNEXPECTED)
public final class CompartmentRegionalTransferReport extends RegionAggregationPeriodicReport {

    /*
     * Map of counters
     */
    private final Map<String, Map<CompartmentId, Map<CompartmentId, Counter>>> regionMap = new LinkedHashMap<>();
    private ReportHeader reportHeader;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeader = reportHeaderBuilder
                    .add("Region")
                    .add("SourceCompartment")
                    .add("DestinationCompartment")
                    .add("Transfers")
                    .build();
        }
        return reportHeader;
    }

    @Override
    protected void flush(ObservableEnvironment observableEnvironment) {
        final ReportItem.Builder reportItemBuilder = ReportItem.builder();

        for (final String regionId : regionMap.keySet()) {
            final Map<CompartmentId, Map<CompartmentId, Counter>> sourceCompartmentMap = regionMap.get(regionId);
            for (final CompartmentId sourceCompartmentId : sourceCompartmentMap.keySet()) {
                final Map<CompartmentId, Counter> destinationCompartmentMap = sourceCompartmentMap.get(sourceCompartmentId);
                for (final CompartmentId destinationCompartmentId : destinationCompartmentMap.keySet()) {
                    final Counter counter = destinationCompartmentMap.get(destinationCompartmentId);
                    if (counter.count > 0) {
                        reportItemBuilder.setReportHeader(getReportHeader());
                        reportItemBuilder.setReportType(getClass());
                        reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                        reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());
                        buildTimeFields(reportItemBuilder);

                        reportItemBuilder.addValue(regionId);
                        reportItemBuilder.addValue(sourceCompartmentId.toString());
                        reportItemBuilder.addValue(destinationCompartmentId.toString());
                        reportItemBuilder.addValue(counter.count);

                        observableEnvironment.releaseOutputItem(reportItemBuilder.build());
                        counter.count = 0;
                    }
                }
            }
        }
    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        final Set<StateChange> result = new LinkedHashSet<>();
        result.add(StateChange.COMPARTMENT_ASSIGNMENT);
        result.add(StateChange.PERSON_ADDITION);
        return result;
    }

    @Override
    public void handleCompartmentAssignment(ObservableEnvironment observableEnvironment, final PersonId personId, final CompartmentId sourceCompartmentId) {
        setCurrentReportingPeriod(observableEnvironment);
        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId destinationCompartmentId = observableEnvironment.getPersonCompartment(personId);
        increment(regionId, sourceCompartmentId, destinationCompartmentId);
    }

    @Override
    public void handlePersonAddition(ObservableEnvironment observableEnvironment, final PersonId personId) {
        setCurrentReportingPeriod(observableEnvironment);
        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
        increment(regionId, compartmentId, compartmentId);
    }

    private void increment(final RegionId regionId, final CompartmentId sourceCompartmentId, final CompartmentId destinationCompartmentId) {
        final Counter counter = regionMap.get(getFipsString(regionId)).get(sourceCompartmentId).get(destinationCompartmentId);
        counter.count++;
    }

    @Override
    public void init(final ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        super.init(observableEnvironment, initialData);

        final Set<CompartmentId> compartmentIds = observableEnvironment.getCompartmentIds();
        final Set<RegionId> regionIds = observableEnvironment.getRegionIds();

        /*
         * Fill the region map with empty counters
         */
        for (final RegionId regionId : regionIds) {
            final Map<CompartmentId, Map<CompartmentId, Counter>> sourceCompartmentMap = new LinkedHashMap<>();
            regionMap.put(getFipsString(regionId), sourceCompartmentMap);
            for (final CompartmentId sourceCompartmentId : compartmentIds) {
                final Map<CompartmentId, Counter> destinationCompartmentMap = new LinkedHashMap<>();
                sourceCompartmentMap.put(sourceCompartmentId, destinationCompartmentMap);
                for (final CompartmentId destinationCompartmentId : compartmentIds) {
                    final Counter counter = new Counter();
                    destinationCompartmentMap.put(destinationCompartmentId, counter);
                }
            }
        }
        setCurrentReportingPeriod(observableEnvironment);
        for (PersonId personId : observableEnvironment.getPeople()) {
            final RegionId regionId = observableEnvironment.getPersonRegion(personId);
            final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
            increment(regionId, compartmentId, compartmentId);
        }
    }

    /*
     * Static class that counts the number of transfers between two compartment
     * within a region
     */
    private static class Counter {
        int count;
    }

}