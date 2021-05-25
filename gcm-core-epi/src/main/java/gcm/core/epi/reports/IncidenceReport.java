package gcm.core.epi.reports;

import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.propertytypes.FipsScope;
import nucleus.ReportContext;
import plugins.compartments.events.observation.PersonCompartmentChangeObservationEvent;
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

public class IncidenceReport extends RegionAggregationPeriodicReport {

    private final Map<String, Map<CounterType, Counter>> regionCounterMap = new LinkedHashMap<>();
    private final ReportHeader reportHeader;

    public IncidenceReport(ReportPeriod reportPeriod, FipsScope fipsScope) {
        super(reportPeriod, fipsScope);
        ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
        addTimeFieldHeaders(reportHeaderBuilder);
        reportHeader = reportHeaderBuilder
                .add("Region")
                .add("NewInfections")
                .add("NewCases")
                .add("NewHospitalizationsWithBeds")
                .add("NewHospitalizationsWithoutBeds")
                .add("NewDeaths")
                .build();
    }

    private void handlePersonPropertyChangeObservationEvent(ReportContext context, PersonPropertyChangeObservationEvent personPropertyChangeObservationEvent) {
        PersonPropertyId personPropertyId = personPropertyChangeObservationEvent.getPersonPropertyId();
        PersonId personId = personPropertyChangeObservationEvent.getPersonId();
        if (personPropertyId == PersonProperty.IS_SYMPTOMATIC) {
            boolean isSymptomatic = (boolean) personPropertyChangeObservationEvent.getCurrentPropertyValue();
            // Only count new assignments of IS_SYMPTOMATIC (even though re-assignment would likely indicate a modeling error)
            if (isSymptomatic & !(boolean) personPropertyChangeObservationEvent.getPreviousPropertyValue()) {
                RegionId regionId = regionLocationDataView.getPersonRegion(personId);
                regionCounterMap.get(getFipsString(regionId)).get(CounterType.CASES).count++;
            }
        } else if (personPropertyId == PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED) {
            RegionId regionId = regionLocationDataView.getPersonRegion(personId);
            regionCounterMap.get(getFipsString(regionId)).get(CounterType.HOSPITALIZATIONS_WITHOUT_BED).count++;
        } else if (personPropertyId == PersonProperty.IS_DEAD) {
            RegionId regionId = regionLocationDataView.getPersonRegion(personId);
            regionCounterMap.get(getFipsString(regionId)).get(CounterType.DEATHS).count++;
        }
    }

    private void handlePersonCompartmentChangeObservationEvent(ReportContext context, PersonCompartmentChangeObservationEvent personCompartmentChangeObservationEvent) {
        if (personCompartmentChangeObservationEvent.getPreviousCompartmentId() == Compartment.SUSCEPTIBLE &&
                personCompartmentChangeObservationEvent.getCurrentCompartmentId() == Compartment.INFECTED) {
            RegionId regionId = regionLocationDataView.getPersonRegion(personCompartmentChangeObservationEvent.getPersonId());
            regionCounterMap.get(getFipsString(regionId)).get(CounterType.INFECTIONS).count++;
        }
    }

    private void handlePersonResourceChangeObservationEvent(ReportContext reportContext, PersonResourceChangeObservationEvent personResourceChangeObservationEvent) {
        if (personResourceChangeObservationEvent.getResourceId() == Resource.HOSPITAL_BED &&
                personResourceChangeObservationEvent.getCurrentResourceLevel() > 0) {
            RegionId regionId = regionLocationDataView.getPersonRegion(personResourceChangeObservationEvent.getPersonId());
            regionCounterMap.get(getFipsString(regionId)).get(CounterType.HOSPITALIZATIONS_WITH_BED).count++;
        }
    }

    @Override
    protected void flush(ReportContext reportContext) {

        final ReportItem.Builder reportItemBuilder = ReportItem.builder();

        for (String regionId : regionCounterMap.keySet()) {
            Map<CounterType, Counter> counterMap = regionCounterMap.get(regionId);
            int infections = counterMap.get(CounterType.INFECTIONS).count;
            int cases = counterMap.get(CounterType.CASES).count;
            int hospitalizationsWithBed = counterMap.get(CounterType.HOSPITALIZATIONS_WITH_BED).count;
            int hospitalizationsWithoutBed = counterMap.get(CounterType.HOSPITALIZATIONS_WITHOUT_BED).count;
            int deaths = counterMap.get(CounterType.DEATHS).count;
            if (infections > 0 | cases > 0 | hospitalizationsWithBed > 0 | hospitalizationsWithoutBed > 0 | deaths > 0) {
                reportItemBuilder.setReportHeader(reportHeader);
                reportItemBuilder.setReportId(reportContext.getCurrentReportId());

                fillTimeFields(reportItemBuilder);
                reportItemBuilder.addValue(regionId);
                reportItemBuilder.addValue(infections);
                reportItemBuilder.addValue(cases);
                reportItemBuilder.addValue(hospitalizationsWithBed);
                reportItemBuilder.addValue(hospitalizationsWithoutBed);
                reportItemBuilder.addValue(deaths);

                reportContext.releaseOutput(reportItemBuilder.build());

                // Reset counters
                for (CounterType counterType : CounterType.values()) {
                    counterMap.get(counterType).count = 0;
                }
            }
        }

    }

    RegionDataView regionDataView;
    RegionLocationDataView regionLocationDataView;
    PersonPropertyDataView personPropertyDataView;

    @Override
    public void init(ReportContext reportContext) {
        super.init(reportContext);

        // Changes to PersonProperty.IS_SYMPTOMATIC or PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED flags
        reportContext.subscribeToEvent(PersonPropertyChangeObservationEvent.class, this::handlePersonPropertyChangeObservationEvent);
        reportContext.subscribeToEvent(PersonCompartmentChangeObservationEvent.class, this::handlePersonCompartmentChangeObservationEvent);
        reportContext.subscribeToEvent(PersonResourceChangeObservationEvent.class, this::handlePersonResourceChangeObservationEvent);

        regionDataView = reportContext.getDataView(RegionDataView.class).get();
        regionLocationDataView = reportContext.getDataView(RegionLocationDataView.class).get();
        personPropertyDataView = reportContext.getDataView(PersonPropertyDataView.class).get();

        // Initialize regionCounterMap
        for (RegionId regionId : regionDataView.getRegionIds()) {
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
