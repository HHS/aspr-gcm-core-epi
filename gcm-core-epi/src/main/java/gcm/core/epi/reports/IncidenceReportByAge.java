package gcm.core.epi.reports;

import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.FipsScope;
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
import plugins.reports.support.ReportPeriod;
import plugins.resources.events.observation.PersonResourceChangeObservationEvent;

import java.util.*;

public class IncidenceReportByAge extends RegionAggregationPeriodicReport {

    private final Map<String, Map<AgeGroup, Map<VariantId, Map<CounterType, Counter>>>> counterMap = new LinkedHashMap<>();
    private final ReportHeader reportHeader;

    public IncidenceReportByAge(ReportPeriod reportPeriod, FipsScope fipsScope) {
        super(reportPeriod, fipsScope);
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

    private AgeGroup getPersonAgeGroup(PersonId personId) {
        PopulationDescription populationDescription =  globalDataView.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);
        Integer ageGroupIndex = personPropertyDataView.getPersonPropertyValue(personId, PersonProperty.AGE_GROUP_INDEX);
        return populationDescription.ageGroupPartition().getAgeGroupFromIndex(ageGroupIndex);
    }

    GlobalDataView globalDataView;
    RegionDataView regionDataView;
    RegionLocationDataView regionLocationDataView;
    PersonPropertyDataView personPropertyDataView;

    @Override
    public void init(ReportContext reportContext) {
        super.init(reportContext);

        reportContext.subscribe(PersonPropertyChangeObservationEvent.getEventLabelByProperty(reportContext, PersonProperty.IS_SYMPTOMATIC),
                this::handlePersonPropertyChangeObservationEvent);
        reportContext.subscribe(PersonPropertyChangeObservationEvent.getEventLabelByProperty(reportContext, PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED),
                this::handlePersonPropertyChangeObservationEvent);
        reportContext.subscribe(PersonPropertyChangeObservationEvent.getEventLabelByProperty(reportContext, PersonProperty.IS_DEAD),
                this::handlePersonPropertyChangeObservationEvent);
        reportContext.subscribe(PersonCompartmentChangeObservationEvent.getEventLabelByArrivalComparment(reportContext, Compartment.INFECTED),
                this::handlePersonCompartmentChangeObservationEvent);
        reportContext.subscribe(PersonResourceChangeObservationEvent.getEventLabelByResource(reportContext,
                Resource.HOSPITAL_BED), this::handlePersonResourceChangeObservationEvent);

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

    private void handlePersonPropertyChangeObservationEvent(ReportContext context, PersonPropertyChangeObservationEvent personPropertyChangeObservationEvent) {
        PersonPropertyId personPropertyId = personPropertyChangeObservationEvent.getPersonPropertyId();
        PersonId personId = personPropertyChangeObservationEvent.getPersonId();
        if (personPropertyId == PersonProperty.IS_SYMPTOMATIC) {
            boolean isSymptomatic = (boolean) personPropertyChangeObservationEvent.getCurrentPropertyValue();
            // Only count new assignments of IS_SYMPTOMATIC (even though re-assignment would likely indicate a modeling error)
            if (isSymptomatic & !(boolean) personPropertyChangeObservationEvent.getPreviousPropertyValue()) {
                getCounter(personId, CounterType.CASES).count++;
            }
        } else if (personPropertyId == PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED) {
            getCounter(personId, CounterType.HOSPITALIZATIONS_WITHOUT_BED).count++;
        } else if (personPropertyId == PersonProperty.IS_DEAD) {
            getCounter(personId, CounterType.DEATHS).count++;
        }
    }

    private void handlePersonCompartmentChangeObservationEvent(ReportContext context, PersonCompartmentChangeObservationEvent personCompartmentChangeObservationEvent) {
        if (personCompartmentChangeObservationEvent.getPreviousCompartmentId() == Compartment.SUSCEPTIBLE &&
                personCompartmentChangeObservationEvent.getCurrentCompartmentId() == Compartment.INFECTED) {
            getCounter(personCompartmentChangeObservationEvent.getPersonId(), CounterType.INFECTIONS).count++;
        }
    }

    private void handlePersonResourceChangeObservationEvent(ReportContext context, PersonResourceChangeObservationEvent personResourceChangeObservationEvent) {
        if (personResourceChangeObservationEvent.getResourceId() == Resource.HOSPITAL_BED &&
                personResourceChangeObservationEvent.getCurrentResourceLevel() > 0) {
            getCounter(personResourceChangeObservationEvent.getPersonId(), CounterType.HOSPITALIZATIONS_WITH_BED).count++;
        }
    }

    @Override
    protected void flush(ReportContext reportContext) {

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
                            reportItemBuilder.setReportHeader(reportHeader);
                            reportItemBuilder.setReportId(reportContext.getCurrentReportId());
                            fillTimeFields(reportItemBuilder);

                            reportItemBuilder.addValue(regionId);
                            reportItemBuilder.addValue(ageGroup.toString());
                            reportItemBuilder.addValue(variantId.id());
                            reportItemBuilder.addValue(counterType.toString());
                            reportItemBuilder.addValue(count);

                            reportContext.releaseOutput(reportItemBuilder.build());
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
