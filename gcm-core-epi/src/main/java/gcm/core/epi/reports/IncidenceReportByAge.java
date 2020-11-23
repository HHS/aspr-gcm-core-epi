package gcm.core.epi.reports;

import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.scenario.*;
import gcm.simulation.ObservableEnvironment;

import java.util.*;

public class IncidenceReportByAge extends RegionAggregationPeriodicReport {

    private final Map<String, Map<AgeGroup, Map<CounterType, Counter>>> counterMap = new LinkedHashMap<>();
    private ReportHeader reportHeader;

    private static AgeGroup getPersonAgeGroup(ObservableEnvironment observableEnvironment, PersonId personId) {
        PopulationDescription populationDescription = observableEnvironment.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);
        Integer ageGroupIndex = observableEnvironment.getPersonPropertyValue(personId, PersonProperty.AGE_GROUP_INDEX);
        return populationDescription.ageGroupPartition().getAgeGroupFromIndex(ageGroupIndex);
    }

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeader = reportHeaderBuilder
                    .add("Region")
                    .add("AgeGroup")
                    .add("Metric")
                    .add("Incidence")
                    .build();
        }
        return reportHeader;
    }

    private Counter getCounter(ObservableEnvironment observableEnvironment, PersonId personId, CounterType counterType) {
        RegionId regionId = observableEnvironment.getPersonRegion(personId);
        AgeGroup ageGroup = getPersonAgeGroup(observableEnvironment, personId);
        return counterMap
                .computeIfAbsent(getFipsString(regionId), x -> new HashMap<>())
                .computeIfAbsent(ageGroup, x -> new EnumMap<>(CounterType.class))
                .computeIfAbsent(counterType, x -> new Counter());
    }

    @Override
    public void handlePersonPropertyValueAssignment(ObservableEnvironment observableEnvironment, PersonId personId, PersonPropertyId personPropertyId, Object oldPersonPropertyValue) {
        if (personPropertyId == PersonProperty.IS_SYMPTOMATIC) {
            boolean isSymptomatic = observableEnvironment.getPersonPropertyValue(personId, PersonProperty.IS_SYMPTOMATIC);
            // Only count new assignments of IS_SYMPTOMATIC (even though re-assignment would likely indicate a modeling error)
            if (isSymptomatic & !(boolean) oldPersonPropertyValue) {
                setCurrentReportingPeriod(observableEnvironment);
                getCounter(observableEnvironment, personId, CounterType.CASES).count++;
            }
        } else if (personPropertyId == PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED) {
            setCurrentReportingPeriod(observableEnvironment);
            getCounter(observableEnvironment, personId, CounterType.HOSPITALIZATIONS_WITHOUT_BED).count++;
        } else if (personPropertyId == PersonProperty.IS_DEAD) {
            setCurrentReportingPeriod(observableEnvironment);
            getCounter(observableEnvironment, personId, CounterType.DEATHS).count++;
        }
    }

    @Override
    public void handleCompartmentAssignment(ObservableEnvironment observableEnvironment, PersonId personId, CompartmentId sourceCompartmentId) {
        if (sourceCompartmentId == Compartment.SUSCEPTIBLE) {
            CompartmentId targetCompartmentId = observableEnvironment.getPersonCompartment(personId);
            if (targetCompartmentId == Compartment.INFECTED) {
                setCurrentReportingPeriod(observableEnvironment);
                getCounter(observableEnvironment, personId, CounterType.INFECTIONS).count++;
            }
        }
    }

    @Override
    public void handleRegionResourceTransferToPerson(ObservableEnvironment observableEnvironment, PersonId personId, ResourceId resourceId, long amount) {
        if (resourceId == Resource.HOSPITAL_BED) {
            setCurrentReportingPeriod(observableEnvironment);
            getCounter(observableEnvironment, personId, CounterType.HOSPITALIZATIONS_WITH_BED).count++;
        }
    }

    @Override
    protected void flush(ObservableEnvironment observableEnvironment) {

        final ReportItem.Builder reportItemBuilder = ReportItem.builder();

        for (String regionId : counterMap.keySet()) {
            Map<AgeGroup, Map<CounterType, Counter>> ageGroupCounterMap = counterMap.get(regionId);
            for (AgeGroup ageGroup : ageGroupCounterMap.keySet()) {
                Map<CounterType, Counter> counters = ageGroupCounterMap.get(ageGroup);
                for (CounterType counterType : counters.keySet()) {
                    int count = counters.get(counterType).count;
                    if (count > 0) {
                        reportItemBuilder.setReportHeader(getReportHeader());
                        reportItemBuilder.setReportType(getClass());
                        reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                        reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());
                        buildTimeFields(reportItemBuilder);

                        reportItemBuilder.addValue(regionId);
                        reportItemBuilder.addValue(ageGroup.toString());
                        reportItemBuilder.addValue(counterType.toString());
                        reportItemBuilder.addValue(count);

                        observableEnvironment.releaseOutputItem(reportItemBuilder.build());
                        counters.get(counterType).count = 0;
                    }
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
