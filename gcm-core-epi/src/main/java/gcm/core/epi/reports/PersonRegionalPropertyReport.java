package gcm.core.epi.reports;

import gcm.output.reports.PersonInfo;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportHeader.ReportHeaderBuilder;
import gcm.output.reports.ReportItem.ReportItemBuilder;
import gcm.output.reports.StateChange;
import gcm.scenario.CompartmentId;
import gcm.scenario.PersonId;
import gcm.scenario.PersonPropertyId;
import gcm.scenario.RegionId;
import gcm.simulation.ObservableEnvironment;
import gcm.util.annotations.Source;
import gcm.util.annotations.TestStatus;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
    private ReportHeader reportHeader;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeaderBuilder reportHeaderBuilder = new ReportHeaderBuilder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeaderBuilder.add("Region");
            reportHeaderBuilder.add("Compartment");
            reportHeaderBuilder.add("Property");
            reportHeaderBuilder.add("Value");
            reportHeaderBuilder.add("PersonCount");
            reportHeader = reportHeaderBuilder.build();
        }
        return reportHeader;
    }

    /*
     * Decrements the population for the given tuple
     */
    private void decrement(final RegionId regionId, final CompartmentId compartmentId, final PersonPropertyId personPropertyId, final Object personPropertyValue) {
        getCounter(regionId, compartmentId, personPropertyId, personPropertyValue).count--;
    }

    @Override
    protected void flush(ObservableEnvironment observableEnvironment) {

        final ReportItemBuilder reportItemBuilder = new ReportItemBuilder();

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
                            reportItemBuilder.setReportHeader(getReportHeader());
                            reportItemBuilder.setReportType(getClass());
                            reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                            reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());

                            buildTimeFields(reportItemBuilder);
                            reportItemBuilder.addValue(regionId);
                            reportItemBuilder.addValue(compartmentId.toString());
                            reportItemBuilder.addValue(personPropertyId.toString());
                            reportItemBuilder.addValue(personPropertyValue);
                            reportItemBuilder.addValue(personCount);

                            observableEnvironment.releaseOutputItem(reportItemBuilder.build());
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

    @Override
    public Set<StateChange> getListenedStateChanges() {
        final Set<StateChange> result = new LinkedHashSet<>();
        result.add(StateChange.PERSON_PROPERTY_VALUE_ASSIGNMENT);
        result.add(StateChange.COMPARTMENT_ASSIGNMENT);
        result.add(StateChange.REGION_ASSIGNMENT);
        result.add(StateChange.PERSON_ADDITION);
        result.add(StateChange.PERSON_REMOVAL);

        return result;
    }

    @Override
    public void handleCompartmentAssignment(ObservableEnvironment observableEnvironment, final PersonId personId, final CompartmentId sourceCompartmentId) {
        setCurrentReportingPeriod(observableEnvironment);

        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);

        for (final PersonPropertyId personPropertyId : personPropertyIds) {
            final Object personPropertyValue = observableEnvironment.getPersonPropertyValue(personId, personPropertyId);
            increment(regionId, compartmentId, personPropertyId, personPropertyValue);
            decrement(regionId, sourceCompartmentId, personPropertyId, personPropertyValue);
        }
    }

    @Override
    public void handlePersonAddition(ObservableEnvironment observableEnvironment, final PersonId personId) {
        setCurrentReportingPeriod(observableEnvironment);

        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);

        for (final PersonPropertyId personPropertyId : personPropertyIds) {
            final Object personPropertyValue = observableEnvironment.getPersonPropertyValue(personId, personPropertyId);
            increment(regionId, compartmentId, personPropertyId, personPropertyValue);
        }
    }

    @Override
    public void handlePersonPropertyValueAssignment(ObservableEnvironment observableEnvironment, final PersonId personId, final PersonPropertyId personPropertyId,
                                                    final Object oldPersonPropertyValue) {

        if (personPropertyIds.contains(personPropertyId)) {

            setCurrentReportingPeriod(observableEnvironment);

            final RegionId regionId = observableEnvironment.getPersonRegion(personId);
            final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
            final Object currentValue = observableEnvironment.getPersonPropertyValue(personId, personPropertyId);
            increment(regionId, compartmentId, personPropertyId, currentValue);
            decrement(regionId, compartmentId, personPropertyId, oldPersonPropertyValue);
        }
    }

    @Override
    public void handlePersonRemoval(ObservableEnvironment observableEnvironment, PersonInfo personInfo) {
        setCurrentReportingPeriod(observableEnvironment);
        Map<PersonPropertyId, Object> propertyValues = personInfo.getPropertyValues();
        RegionId regionId = personInfo.getRegionId();
        CompartmentId compartmentId = personInfo.getCompartmentId();
        for (final PersonPropertyId personPropertyId : personPropertyIds) {
            final Object personPropertyValue = propertyValues.get(personPropertyId);
            decrement(regionId, compartmentId, personPropertyId, personPropertyValue);
        }
    }

    @Override
    public void handleRegionAssignment(ObservableEnvironment observableEnvironment, final PersonId personId, final RegionId sourceRegionId) {
        setCurrentReportingPeriod(observableEnvironment);

        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);

        for (final PersonPropertyId personPropertyId : personPropertyIds) {
            final Object personPropertyValue = observableEnvironment.getPersonPropertyValue(personId, personPropertyId);
            increment(regionId, compartmentId, personPropertyId, personPropertyValue);
            decrement(sourceRegionId, compartmentId, personPropertyId, personPropertyValue);
        }
    }

    /*
     * Increments the population for the given tuple
     */
    private void increment(final RegionId regionId, final CompartmentId compartmentId, final PersonPropertyId personPropertyId, final Object personPropertyValue) {
        getCounter(regionId, compartmentId, personPropertyId, personPropertyValue).count++;
    }

    @Override
    public void init(final ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        super.init(observableEnvironment, initialData);

        for (Object initialDatum : initialData) {
            if (initialDatum instanceof PersonPropertyId[]) {
                PersonPropertyId[] personPropertyList = (PersonPropertyId[]) initialDatum;
//                Arrays.stream(personPropertyList).map(personPropertyId -> personPropertyIds.add(personPropertyId));
                for (PersonPropertyId personProperty : personPropertyList) {
                    personPropertyIds.add(personProperty);
                }
            }
        }

        /*
         * If no person properties were specified, then assume all are wanted
         */
        if (personPropertyIds.size() == 0) {
            personPropertyIds.addAll(observableEnvironment.getPersonPropertyIds());
        }

        /*
         * Ensure that every client supplied property identifier is valid
         */
        final Set<PersonPropertyId> validPropertyIds = observableEnvironment.getPersonPropertyIds();
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
        final Set<CompartmentId> compartmentIds = observableEnvironment.getCompartmentIds();
        final Set<RegionId> regionIds = observableEnvironment.getRegionIds();
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


        setCurrentReportingPeriod(observableEnvironment);
        for (PersonId personId : observableEnvironment.getPeople()) {
            final RegionId regionId = observableEnvironment.getPersonRegion(personId);
            final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);

            for (final PersonPropertyId personPropertyId : personPropertyIds) {
                final Object personPropertyValue = observableEnvironment.getPersonPropertyValue(personId, personPropertyId);
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