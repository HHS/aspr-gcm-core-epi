package gcm.core.epi.reports;

import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.variants.VariantId;
import gcm.core.epi.variants.VariantsDescription;
import nucleus.ReportContext;
import plugins.compartments.events.observation.PersonCompartmentChangeObservationEvent;
import plugins.globals.datacontainers.GlobalDataView;
import plugins.people.support.PersonId;
import plugins.personproperties.datacontainers.PersonPropertyDataView;
import plugins.personproperties.events.observation.PersonPropertyChangeObservationEvent;
import plugins.personproperties.support.PersonPropertyId;
import plugins.regions.datacontainers.RegionDataView;
import plugins.regions.datacontainers.RegionLocationDataView;
import plugins.regions.support.RegionId;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;
import plugins.resources.events.observation.PersonResourceChangeObservationEvent;

import java.util.*;

public class IncidenceReportByAge extends RegionAggregationPeriodicReport {

    private final Map<String, Map<AgeGroup, Map<VariantId, Map<CounterType, Counter>>>> counterMap = new LinkedHashMap<>();
    private ReportHeader reportHeader;

    private AgeGroup getPersonAgeGroup(PersonId personId) {
        PopulationDescription populationDescription =  globalDataView.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);
        Integer ageGroupIndex = personPropertyDataView.getPersonPropertyValue(personId, PersonProperty.AGE_GROUP_INDEX);
        return populationDescription.ageGroupPartition().getAgeGroupFromIndex(ageGroupIndex);
    }

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeader = reportHeaderBuilder
                    .add("Region")
                    .add("AgeGroup")
                    .add("Variant")
                    .add("Metric")
                    .add("Incidence")
                    .build();
        }
        return reportHeader;
    }

    GlobalDataView globalDataView;
    RegionDataView regionDataView;
    RegionLocationDataView regionLocationDataView;
    PersonPropertyDataView personPropertyDataView;

    @Override
    public void init(ReportContext reportContext) {
        super.init(reportContext);

        // Changes to PersonProperty.IS_SYMPTOMATIC or PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED flags
        reportContext.subscribeToEvent(PersonPropertyChangeObservationEvent.class);
        reportContext.subscribeToEvent(PersonCompartmentChangeObservationEvent.class);
        reportContext.subscribeToEvent(PersonResourceChangeObservationEvent.class);

        setConsumer(PersonPropertyChangeObservationEvent.class, this::handlePersonPropertyChangeObservationEvent);
        setConsumer(PersonCompartmentChangeObservationEvent.class, this::handlePersonCompartmentChangeObservationEvent);
        setConsumer(PersonResourceChangeObservationEvent.class, this::handlePersonResourceChangeObservationEvent);

        globalDataView = reportContext.getDataView(GlobalDataView.class).get();
        regionDataView = reportContext.getDataView(RegionDataView.class).get();
        regionLocationDataView = reportContext.getDataView(RegionLocationDataView.class).get();
        personPropertyDataView = reportContext.getDataView(PersonPropertyDataView.class).get();
    }

    private Counter getCounter(PersonId personId, CounterType counterType) {
        RegionId regionId = regionLocationDataView.getPersonRegion(personId);
        AgeGroup ageGroup = getPersonAgeGroup(personId);
        VariantsDescription variantsDescription = globalDataView.getGlobalPropertyValue(GlobalProperty.VARIANTS_DESCRIPTION);
        // Handle case that infection index has not been set yet from initial seeding
        int strainIndex = Math.max(personPropertyDataView.getPersonPropertyValue(personId,
                PersonProperty.PRIOR_INFECTION_STRAIN_INDEX_1), 0);
        VariantId variantId = variantsDescription.variantIdList().get(strainIndex);
        return counterMap
                .computeIfAbsent(getFipsString(regionId), x -> new HashMap<>())
                .computeIfAbsent(ageGroup, x -> new HashMap<>())
                .computeIfAbsent(variantId, x -> new EnumMap<>(CounterType.class))
                .computeIfAbsent(counterType, x -> new Counter());
    }

    private void handlePersonPropertyChangeObservationEvent(PersonPropertyChangeObservationEvent personPropertyChangeObservationEvent) {
        PersonPropertyId personPropertyId = personPropertyChangeObservationEvent.getPersonPropertyId();
        PersonId personId = personPropertyChangeObservationEvent.getPersonId();
        if (personPropertyId == PersonProperty.IS_SYMPTOMATIC) {
            boolean isSymptomatic = (boolean) personPropertyChangeObservationEvent.getCurrentPropertyValue();
            // Only count new assignments of IS_SYMPTOMATIC (even though re-assignment would likely indicate a modeling error)
            if (isSymptomatic & !(boolean) personPropertyChangeObservationEvent.getPreviousPropertyValue()) {
                RegionId regionId = regionLocationDataView.getPersonRegion(personId);
                getCounter(personId, CounterType.CASES).count++;
            }
        } else if (personPropertyId == PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED) {
            RegionId regionId = regionLocationDataView.getPersonRegion(personId);
            getCounter(personId, CounterType.HOSPITALIZATIONS_WITHOUT_BED).count++;
        } else if (personPropertyId == PersonProperty.IS_DEAD) {
            RegionId regionId = regionLocationDataView.getPersonRegion(personId);
            getCounter(personId, CounterType.DEATHS).count++;
        }
    }

    private void handlePersonCompartmentChangeObservationEvent(PersonCompartmentChangeObservationEvent personCompartmentChangeObservationEvent) {
        if (personCompartmentChangeObservationEvent.getPreviousCompartmentId() == Compartment.SUSCEPTIBLE &&
                personCompartmentChangeObservationEvent.getCurrentCompartmentId() == Compartment.INFECTED) {
            RegionId regionId = regionLocationDataView.getPersonRegion(personCompartmentChangeObservationEvent.getPersonId());
            getCounter(personCompartmentChangeObservationEvent.getPersonId(), CounterType.INFECTIONS).count++;
        }
    }

    private void handlePersonResourceChangeObservationEvent(PersonResourceChangeObservationEvent personResourceChangeObservationEvent) {
        if (personResourceChangeObservationEvent.getResourceId() == Resource.HOSPITAL_BED &&
                personResourceChangeObservationEvent.getCurrentResourceLevel() > 0) {
            getCounter(personResourceChangeObservationEvent.getPersonId(), CounterType.HOSPITALIZATIONS_WITH_BED).count++;
        }
    }

    @Override
    protected void flush() {

        final ReportItem.Builder reportItemBuilder = ReportItem.builder();

        for (String regionId : counterMap.keySet()) {
            Map<AgeGroup, Map<VariantId, Map<CounterType, Counter>>> ageGroupCounterMap = counterMap.get(regionId);
            for (AgeGroup ageGroup : ageGroupCounterMap.keySet()) {
                Map<VariantId, Map<CounterType, Counter>> variantCounterMap = ageGroupCounterMap.get(ageGroup);
                for (VariantId variantId : variantCounterMap.keySet()) {
                    Map<CounterType, Counter> counters = variantCounterMap.get(variantId);
                    for (CounterType counterType : counters.keySet()) {
                        int count = counters.get(counterType).count;
                        if (count > 0) {
                            reportItemBuilder.setReportHeader(getReportHeader());
                            reportItemBuilder.setReportType(getClass());
                            buildTimeFields(reportItemBuilder);

                            reportItemBuilder.addValue(regionId);
                            reportItemBuilder.addValue(ageGroup.toString());
                            reportItemBuilder.addValue(variantId.id());
                            reportItemBuilder.addValue(counterType.toString());
                            reportItemBuilder.addValue(count);

                            releaseOutputItem(reportItemBuilder.build());
                            counters.get(counterType).count = 0;
                        }
                    }
                }
            }
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
