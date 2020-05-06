package gcm.core.epi.reports;

import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.Resource;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.scenario.*;
import gcm.simulation.ObservableEnvironment;

import java.util.*;

public class IncidenceReport extends RegionAggregationPeriodicReport {

    private final Map<String, Map<CounterType, Counter>> regionCounterMap = new LinkedHashMap<>();
    private ReportHeader reportHeader;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.ReportHeaderBuilder reportHeaderBuilder = new ReportHeader.ReportHeaderBuilder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeaderBuilder.add("Region");
            reportHeaderBuilder.add("NewInfections");
            reportHeaderBuilder.add("NewCases");
            reportHeaderBuilder.add("NewHospitalizationsWithBeds");
            reportHeaderBuilder.add("NewHospitalizationsWithoutBeds");
            reportHeaderBuilder.add("NewDeaths");
            reportHeader = reportHeaderBuilder.build();
        }
        return reportHeader;
    }

    @Override
    public void handlePersonPropertyValueAssignment(ObservableEnvironment observableEnvironment, PersonId personId, PersonPropertyId personPropertyId, Object oldPersonPropertyValue) {
        if (personPropertyId == PersonProperty.IS_SYMPTOMATIC) {
            boolean isSymptomatic = observableEnvironment.getPersonPropertyValue(personId, PersonProperty.IS_SYMPTOMATIC);
            // Only count new assignments of IS_SYMPTOMATIC (even though re-assignment would likely indicate a modeling error)
            if (isSymptomatic & !(boolean) oldPersonPropertyValue) {
                setCurrentReportingPeriod(observableEnvironment);
                RegionId regionId = observableEnvironment.getPersonRegion(personId);
                regionCounterMap.get(getFipsString(regionId)).get(CounterType.CASES).count++;
            }
        } else if (personPropertyId == PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED) {
            setCurrentReportingPeriod(observableEnvironment);
            RegionId regionId = observableEnvironment.getPersonRegion(personId);
            regionCounterMap.get(getFipsString(regionId)).get(CounterType.HOSPITALIZATIONS_WITHOUT_BED).count++;
        } else if (personPropertyId == PersonProperty.IS_DEAD) {
            setCurrentReportingPeriod(observableEnvironment);
            RegionId regionId = observableEnvironment.getPersonRegion(personId);
            regionCounterMap.get(getFipsString(regionId)).get(CounterType.DEATHS).count++;
        }
    }

    @Override
    public void handleCompartmentAssignment(ObservableEnvironment observableEnvironment, PersonId personId, CompartmentId sourceCompartmentId) {
        if (sourceCompartmentId == Compartment.SUSCEPTIBLE) {
            CompartmentId targetCompartmentId = observableEnvironment.getPersonCompartment(personId);
            if (targetCompartmentId == Compartment.INFECTED) {
                setCurrentReportingPeriod(observableEnvironment);
                RegionId regionId = observableEnvironment.getPersonRegion(personId);
                regionCounterMap.get(getFipsString(regionId)).get(CounterType.INFECTIONS).count++;
            }
        }
    }

    @Override
    public void handleRegionResourceTransferToPerson(ObservableEnvironment observableEnvironment, PersonId personId, ResourceId resourceId, long amount) {
        if (resourceId == Resource.HOSPITAL_BED) {
            setCurrentReportingPeriod(observableEnvironment);
            RegionId regionId = observableEnvironment.getPersonRegion(personId);
            regionCounterMap.get(getFipsString(regionId)).get(CounterType.HOSPITALIZATIONS_WITH_BED).count++;
        }
    }

    @Override
    protected void flush(ObservableEnvironment observableEnvironment) {

        final ReportItem.ReportItemBuilder reportItemBuilder = new ReportItem.ReportItemBuilder();

        for (String regionId : regionCounterMap.keySet()) {
            Map<CounterType, Counter> counterMap = regionCounterMap.get(regionId);
            int infections = counterMap.get(CounterType.INFECTIONS).count;
            int cases = counterMap.get(CounterType.CASES).count;
            int hospitalizationsWithBed = counterMap.get(CounterType.HOSPITALIZATIONS_WITH_BED).count;
            int hospitalizationsWithoutBed = counterMap.get(CounterType.HOSPITALIZATIONS_WITHOUT_BED).count;
            int deaths = counterMap.get(CounterType.DEATHS).count;
            if (infections > 0 | cases > 0 | hospitalizationsWithBed > 0 | hospitalizationsWithoutBed > 0 | deaths > 0) {
                reportItemBuilder.setReportHeader(getReportHeader());
                reportItemBuilder.setReportType(getClass());
                reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());

                buildTimeFields(reportItemBuilder);
                reportItemBuilder.addValue(regionId);
                reportItemBuilder.addValue(infections);
                reportItemBuilder.addValue(cases);
                reportItemBuilder.addValue(hospitalizationsWithBed);
                reportItemBuilder.addValue(hospitalizationsWithoutBed);
                reportItemBuilder.addValue(deaths);

                observableEnvironment.releaseOutputItem(reportItemBuilder.build());

                // Reset counters
                for (CounterType counterType : CounterType.values()) {
                    counterMap.get(counterType).count = 0;
                }
            }
        }

    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        final Set<StateChange> result = new LinkedHashSet<>();
        // Changes to PersonProperty.IS_SYMPTOMATIC or PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED flags
        result.add(StateChange.PERSON_PROPERTY_VALUE_ASSIGNMENT);
        // Moves from Compartment.SUSCEPTIBLE to Compartment.INFECTED
        result.add(StateChange.COMPARTMENT_ASSIGNMENT);
        // People receiving HOSPITAL_BED resources
        result.add(StateChange.REGION_RESOURCE_TRANSFER_TO_PERSON);
        return result;
    }

    @Override
    public void init(ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        super.init(observableEnvironment, initialData);


        // Initialize regionCounterMap
        for (RegionId regionId : observableEnvironment.getRegionIds()) {
            Map<CounterType, Counter> counterMap = new EnumMap<>(CounterType.class);
            for (CounterType counterType : CounterType.values()) {
                counterMap.put(counterType, new Counter());
            }
            regionCounterMap.put(getFipsString(regionId), counterMap);
        }

    }

    /*
        The different types of events
     */
    private enum CounterType {
        INFECTIONS,
        CASES,
        HOSPITALIZATIONS_WITH_BED,
        HOSPITALIZATIONS_WITHOUT_BED,
        DEATHS
    }

    /*
        A counter for people in a region that met a given criterion (such as being new infections or cases)
     */
    private final static class Counter {
        int count;
    }
}
