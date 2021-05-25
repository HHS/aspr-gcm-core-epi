package gcm.core.epi.reports;

import gcm.core.epi.propertytypes.FipsScope;
import nucleus.ReportContext;
import plugins.compartments.datacontainers.CompartmentDataView;
import plugins.compartments.datacontainers.CompartmentLocationDataView;
import plugins.compartments.events.observation.PersonCompartmentChangeObservationEvent;
import plugins.compartments.support.CompartmentId;
import plugins.people.datacontainers.PersonDataView;
import plugins.people.events.observation.PersonCreationObservationEvent;
import plugins.people.events.observation.PersonImminentRemovalObservationEvent;
import plugins.people.support.PersonId;
import plugins.personproperties.datacontainers.PersonPropertyDataView;
import plugins.personproperties.events.observation.PersonPropertyChangeObservationEvent;
import plugins.personproperties.support.PersonPropertyId;
import plugins.regions.datacontainers.RegionDataView;
import plugins.regions.datacontainers.RegionLocationDataView;
import plugins.regions.events.observation.PersonRegionChangeObservationEvent;
import plugins.regions.support.RegionId;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;
import plugins.reports.support.ReportPeriod;
import util.annotations.Source;
import util.annotations.TestStatus;

import java.util.*;

/**
 * A periodic Report that displays the number of people exhibiting a particular
 * value for each person property for a given region/compartment pair. Only
 * non-zero person counts are reported. Adapted from PersonPropertyReport in GCM.
 * <p>
 * <p>
 * Fields
 * <p>
 * Region -- the region identifier
 * <p>
 * Compartment -- the compartment identifier
 * <p>
 * Property -- the person property identifier
 * <p>
 * Value -- the value of the property
 * <p>
 * PersonCount -- the number of people having the property value within the
 * region/compartment pair
 *
 * @author Shawn Hatch
 */
@Source(status = TestStatus.UNEXPECTED)
public final class PersonRegionalPropertyReport extends RegionAggregationPeriodicReport {

    /*
     * The constrained set of person properties that will be used in this
     * report. They are set during init()
     */
    private final Set<PersonPropertyId> personPropertyIds = new LinkedHashSet<>();
    /*
     * The tuple mapping to person counts that is maintained via handling of
     * events.
     */
    private final Map<String, Map<CompartmentId, Map<PersonPropertyId, Map<Object, Counter>>>> tupleMap = new LinkedHashMap<>();
    private final ReportHeader reportHeader;

    public PersonRegionalPropertyReport(ReportPeriod reportPeriod, FipsScope fipsScope, PersonPropertyId... personPropertyIds) {
        super(reportPeriod, fipsScope);
        for (PersonPropertyId personPropertyId : personPropertyIds) {
            this.personPropertyIds.add(personPropertyId);
        }
        ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
        addTimeFieldHeaders(reportHeaderBuilder);
        reportHeader = reportHeaderBuilder
                .add("Region")
                .add("Compartment")
                .add("Property")
                .add("Value")
                .add("PersonCount")
                .build();
    }

    /*
     * Decrements the population for the given tuple
     */
    private void decrement(final RegionId regionId, final CompartmentId compartmentId, final PersonPropertyId personPropertyId, final Object personPropertyValue) {
        getCounter(regionId, compartmentId, personPropertyId, personPropertyValue).count--;
    }

    @Override
    protected void flush(ReportContext reportContext) {

        final ReportItem.Builder reportItemBuilder = ReportItem.builder();

        /*
         * For each tuple having a positive population, report the tuple
         */
        for (final String regionId : tupleMap.keySet()) {
            final Map<CompartmentId, Map<PersonPropertyId, Map<Object, Counter>>> compartmentMap = tupleMap.get(regionId);
            for (final CompartmentId compartmentId : compartmentMap.keySet()) {
                final Map<PersonPropertyId, Map<Object, Counter>> propertyIdMap = compartmentMap.get(compartmentId);
                for (final PersonPropertyId personPropertyId : propertyIdMap.keySet()) {
                    final Map<Object, Counter> personPropertyValueMap = propertyIdMap.get(personPropertyId);
                    for (final Object personPropertyValue : personPropertyValueMap.keySet()) {
                        final Counter counter = personPropertyValueMap.get(personPropertyValue);
                        if (counter.count > 0) {
                            final int personCount = counter.count;
                            reportItemBuilder.setReportHeader(reportHeader);
                            reportItemBuilder.setReportId(reportContext.getCurrentReportId());
                            fillTimeFields(reportItemBuilder);
                            reportItemBuilder.addValue(regionId);
                            reportItemBuilder.addValue(compartmentId.toString());
                            reportItemBuilder.addValue(personPropertyId.toString());
                            reportItemBuilder.addValue(personPropertyValue);
                            reportItemBuilder.addValue(personCount);

                            reportContext.releaseOutput(reportItemBuilder.build());
                        }
                    }
                }
            }
        }
    }

    /*
     * Returns the counter for the give tuple. Creates the counter if it does
     * not already exist.
     */
    private Counter getCounter(final RegionId regionId, final CompartmentId compartmentId, final PersonPropertyId personPropertyId, final Object personPropertyValue) {
        final Map<Object, Counter> propertyValueMap = tupleMap.get(getFipsString(regionId)).get(compartmentId).get(personPropertyId);
        Counter counter = propertyValueMap.get(personPropertyValue);
        if (counter == null) {
            counter = new Counter();
            propertyValueMap.put(personPropertyValue, counter);
        }
        return counter;

    }

    private void handlePersonCompartmentChangeObservationEvent(ReportContext reportContext, PersonCompartmentChangeObservationEvent personCompartmentChangeObservationEvent) {
        PersonId personId = personCompartmentChangeObservationEvent.getPersonId();
        CompartmentId sourceCompartmentId = personCompartmentChangeObservationEvent.getPreviousCompartmentId();

        final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
        final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);

        for (final PersonPropertyId personPropertyId : personPropertyIds) {
            final Object personPropertyValue = personPropertyDataView.getPersonPropertyValue(personId, personPropertyId);
            increment(regionId, compartmentId, personPropertyId, personPropertyValue);
            decrement(regionId, sourceCompartmentId, personPropertyId, personPropertyValue);
        }
    }

    private void handlePersonCreationObservationEvent(ReportContext reportContext, PersonCreationObservationEvent personCreationObservationEvent) {
        PersonId personId = personCreationObservationEvent.getPersonId();

        final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
        final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);

        for (final PersonPropertyId personPropertyId : personPropertyIds) {
            final Object personPropertyValue = personPropertyDataView.getPersonPropertyValue(personId, personPropertyId);
            increment(regionId, compartmentId, personPropertyId, personPropertyValue);
        }
    }

    private void handlePersonPropertyChangeObservationEvent(ReportContext reportContext, PersonPropertyChangeObservationEvent personPropertyChangeObservationEvent) {
        PersonPropertyId personPropertyId = personPropertyChangeObservationEvent.getPersonPropertyId();
        if (personPropertyIds.contains(personPropertyId)) {
            PersonId personId = personPropertyChangeObservationEvent.getPersonId();
            Object previousPropertyValue = personPropertyChangeObservationEvent.getPreviousPropertyValue();
            final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
            final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
            final Object currentValue = personPropertyDataView.getPersonPropertyValue(personId, personPropertyId);
            increment(regionId, compartmentId, personPropertyId, currentValue);
            decrement(regionId, compartmentId, personPropertyId, previousPropertyValue);
        }
    }

    private void handlePersonImminentRemovalObservationEvent(ReportContext reportContext, PersonImminentRemovalObservationEvent personImminentRemovalObservationEvent) {
        PersonId personId = personImminentRemovalObservationEvent.getPersonId();
        RegionId regionId = regionLocationDataView.getPersonRegion(personId);
        CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
        for (PersonPropertyId personPropertyId : personPropertyIds) {
            final Object personPropertyValue = personPropertyDataView.getPersonPropertyValue(personId, personPropertyId);
            decrement(regionId, compartmentId, personPropertyId, personPropertyValue);
        }
    }

    private void handlePersonRegionChangeObservationEvent(ReportContext reportContext, PersonRegionChangeObservationEvent personRegionChangeObservationEvent) {
        PersonId personId = personRegionChangeObservationEvent.getPersonId();
        RegionId previousRegionId = personRegionChangeObservationEvent.getPreviousRegionId();
        RegionId regionId = personRegionChangeObservationEvent.getCurrentRegionId();

        final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);

        for (final PersonPropertyId personPropertyId : personPropertyIds) {
            final Object personPropertyValue = personPropertyDataView.getPersonPropertyValue(personId, personPropertyId);
            increment(regionId, compartmentId, personPropertyId, personPropertyValue);
            decrement(previousRegionId, compartmentId, personPropertyId, personPropertyValue);
        }
    }

    /*
     * Increments the population for the given tuple
     */
    private void increment(final RegionId regionId, final CompartmentId compartmentId, final PersonPropertyId personPropertyId, final Object personPropertyValue) {
        getCounter(regionId, compartmentId, personPropertyId, personPropertyValue).count++;
    }

    private PersonPropertyDataView personPropertyDataView;
    private CompartmentLocationDataView compartmentLocationDataView;
    private RegionLocationDataView regionLocationDataView;


    @Override
    public void init(final ReportContext reportContext) {
        super.init(reportContext);

        reportContext.subscribeToEvent(PersonPropertyChangeObservationEvent.class, this::handlePersonCompartmentChangeObservationEvent);
        reportContext.subscribeToEvent(PersonCreationObservationEvent.class, this::handlePersonCreationObservationEvent);
        reportContext.subscribeToEvent(PersonImminentRemovalObservationEvent.class, this::handlePersonImminentRemovalObservationEvent);
        reportContext.subscribeToEvent(PersonCompartmentChangeObservationEvent.class, this::handlePersonPropertyChangeObservationEvent);
        reportContext.subscribeToEvent(PersonRegionChangeObservationEvent.class, this::handlePersonRegionChangeObservationEvent);

        compartmentLocationDataView = reportContext.getDataView(CompartmentLocationDataView.class).get();
        regionLocationDataView = reportContext.getDataView(RegionLocationDataView.class).get();
        personPropertyDataView = reportContext.getDataView(PersonPropertyDataView.class).get();
        PersonDataView personDataView = reportContext.getDataView(PersonDataView.class).get();

        /*
         * If no person properties were specified, then assume all are wanted
         */
        if (personPropertyIds.size() == 0) {
            personPropertyIds.addAll(personPropertyDataView.getPersonPropertyIds());
        }

        /*
         * Ensure that every client supplied property identifier is valid
         */
        final Set<PersonPropertyId> validPropertyIds = personPropertyDataView.getPersonPropertyIds();
        for (final PersonPropertyId personPropertyId : personPropertyIds) {
            if (!validPropertyIds.contains(personPropertyId)) {
                throw new RuntimeException("invalid property id " + personPropertyId);
            }
        }

        /*
         * Fill the top layers of the regionMap. We do not yet know the set of
         * property values, so we leave that layer empty.
         *
         */
        final Set<CompartmentId> compartmentIds = reportContext.getDataView(CompartmentDataView.class).get().getCompartmentIds();
        final Set<RegionId> regionIds = reportContext.getDataView(RegionDataView.class).get().getRegionIds();
        for (final RegionId regionId : regionIds) {
            final Map<CompartmentId, Map<PersonPropertyId, Map<Object, Counter>>> compartmentMap = new LinkedHashMap<>();
            tupleMap.put(getFipsString(regionId), compartmentMap);
            for (final CompartmentId compartmentId : compartmentIds) {
                final Map<PersonPropertyId, Map<Object, Counter>> propertyIdMap = new LinkedHashMap<>();
                compartmentMap.put(compartmentId, propertyIdMap);
                for (final PersonPropertyId personPropertyId : personPropertyIds) {
                    final Map<Object, Counter> propertyValueMap = new LinkedHashMap<>();
                    propertyIdMap.put(personPropertyId, propertyValueMap);
                }
            }
        }

        for (PersonId personId : personDataView.getPeople()) {
            final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
            final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);

            for (final PersonPropertyId personPropertyId : personPropertyIds) {
                final Object personPropertyValue = personPropertyDataView.getPersonPropertyValue(personId, personPropertyId);
                increment(regionId, compartmentId, personPropertyId, personPropertyValue);
            }
        }
    }

    /*
     * A counter for people having the tuple (Region, Compartment, Person
     * Property, Property Value)
     */
    private final static class Counter {
        int count;
    }

}