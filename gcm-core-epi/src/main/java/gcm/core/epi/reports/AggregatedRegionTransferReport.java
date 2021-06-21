package gcm.core.epi.reports;

import gcm.core.epi.propertytypes.FipsScope;
import nucleus.ReportContext;
import plugins.compartments.datacontainers.CompartmentDataView;
import plugins.compartments.datacontainers.CompartmentLocationDataView;
import plugins.compartments.support.CompartmentId;
import plugins.people.datacontainers.PersonDataView;
import plugins.people.events.observation.PersonCreationObservationEvent;
import plugins.people.support.PersonId;
import plugins.regions.datacontainers.RegionDataView;
import plugins.regions.datacontainers.RegionLocationDataView;
import plugins.regions.events.observation.PersonRegionChangeObservationEvent;
import plugins.regions.support.RegionId;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;
import plugins.reports.support.ReportPeriod;
import util.annotations.Source;
import util.annotations.TestStatus;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A periodic Report that displays the number of times a person transferred from
 * one region to another within a compartment. Only non-zero transfers are
 * reported. Adapted from RegionTransferReport in GCM.
 * <p>
 * <p>
 * Fields
 * <p>
 * Compartment -- the compartment identifier
 * <p>
 * SourceRegion -- the source region identifier
 * <p>
 * DestinationRegion -- the destination region property identifier
 * <p>
 * Transfers -- the number of transfers from the source region to the
 * destination region for people in the compartment
 *
 * @author Shawn Hatch
 */
@Source(status = TestStatus.UNEXPECTED)
public final class AggregatedRegionTransferReport extends RegionAggregationPeriodicReport {

    /*
     * A mapping from a (Compartment, Region, Region) tuple to a count of the
     * number of transfers.
     */
    private final Map<CompartmentId, Map<String, Map<String, Counter>>> compartmentMap = new LinkedHashMap<>();
    /*
     * The derived header for this report
     */
    private ReportHeader reportHeader;

    public AggregatedRegionTransferReport(ReportPeriod reportPeriod, FipsScope fipsScope) {
        super(reportPeriod, fipsScope);
    }

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeader = reportHeaderBuilder
                    .add("Compartment")
                    .add("SourceRegion")
                    .add("DestinationRegion")
                    .add("Transfers")
                    .build();
        }
        return reportHeader;
    }

    @Override
    protected void flush(ReportContext reportContext) {

        final ReportItem.Builder reportItemBuilder = ReportItem.builder();

        for (final CompartmentId compartmentId : compartmentMap.keySet()) {
            final Map<String, Map<String, Counter>> sourceRegionMap = compartmentMap.get(compartmentId);
            for (final String sourceRegionId : sourceRegionMap.keySet()) {
                final Map<String, Counter> destinationRegionMap = sourceRegionMap.get(sourceRegionId);
                for (final String destinationRegionId : destinationRegionMap.keySet()) {
                    final Counter counter = destinationRegionMap.get(destinationRegionId);
                    if (counter.count > 0) {
                        reportItemBuilder.setReportHeader(getReportHeader());
                        reportItemBuilder.setReportId(reportContext.getCurrentReportId());
                        fillTimeFields(reportItemBuilder);

                        reportItemBuilder.addValue(compartmentId.toString());
                        reportItemBuilder.addValue(sourceRegionId);
                        reportItemBuilder.addValue(destinationRegionId);
                        reportItemBuilder.addValue(counter.count);
                        reportContext.releaseOutput(reportItemBuilder.build());
                        counter.count = 0;
                    }
                }
            }
        }
    }

    private void handlePersonCreationObservationEvent(ReportContext context, PersonCreationObservationEvent personCreationObservationEvent) {
        PersonId personId = personCreationObservationEvent.getPersonId();
        final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
        final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
        increment(compartmentId, regionId, regionId);
    }


    private void handlePersonRegionChangeObservationEvent(ReportContext context, PersonRegionChangeObservationEvent personRegionChangeObservationEvent) {
        PersonId personId = personRegionChangeObservationEvent.getPersonId();
        RegionId previousRegionId = personRegionChangeObservationEvent.getPreviousRegionId();
        RegionId currentRegionId = personRegionChangeObservationEvent.getCurrentRegionId();
        final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
        increment(compartmentId, previousRegionId, currentRegionId);
    }

    /*
     * Increments the number of region transfers for the give tuple
     */
    private void increment(final CompartmentId compartmentId, final RegionId sourceRegionId, final RegionId destinationRegionId) {
        final Counter counter = compartmentMap.get(compartmentId).get(getFipsString(sourceRegionId)).get(getFipsString(destinationRegionId));
        counter.count++;
    }

    private CompartmentLocationDataView compartmentLocationDataView;
    private RegionLocationDataView regionLocationDataView;

    @Override
    public void init(final ReportContext reportContext) {
        super.init(reportContext);

        reportContext.subscribe(PersonCreationObservationEvent.class, this::handlePersonCreationObservationEvent);
        reportContext.subscribe(PersonRegionChangeObservationEvent.class, this::handlePersonRegionChangeObservationEvent);

        PersonDataView personDataView = reportContext.getDataView(PersonDataView.class).get();
        compartmentLocationDataView = reportContext.getDataView(CompartmentLocationDataView.class).get();
        regionLocationDataView = reportContext.getDataView(RegionLocationDataView.class).get();
        CompartmentDataView compartmentDataView = reportContext.getDataView(CompartmentDataView.class).get();
        RegionDataView regionDataView = reportContext.getDataView(RegionDataView.class).get();

        final Set<CompartmentId> compartmentIds = compartmentDataView.getCompartmentIds();
        Set<String> regionIds = regionDataView.getRegionIds().stream().map(this::getFipsString).collect(Collectors.toCollection(LinkedHashSet::new));

        /*
         * Fill the compartment map with empty counters
         */
        for (final CompartmentId compartmentId : compartmentIds) {
            final Map<String, Map<String, Counter>> sourceRegionMap = new LinkedHashMap<>();
            compartmentMap.put(compartmentId, sourceRegionMap);
            for (final String sourceRegionId : regionIds) {
                final Map<String, Counter> destinationRegionMap = new LinkedHashMap<>();
                sourceRegionMap.put(sourceRegionId, destinationRegionMap);
                for (final String destinationRegionId : regionIds) {
                    final Counter counter = new Counter();
                    destinationRegionMap.put(destinationRegionId, counter);
                }
            }
        }

        for (PersonId personId : personDataView.getPeople()) {
            final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
            final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
            increment(compartmentId, regionId, regionId);
        }
    }

    /*
     *
     * A counter of the number of people transferring between regions.
     *
     */
    private static class Counter {
        int count;
    }
}