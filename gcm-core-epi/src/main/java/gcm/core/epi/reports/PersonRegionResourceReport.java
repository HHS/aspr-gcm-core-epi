package gcm.core.epi.reports;

import gcm.output.reports.PersonInfo;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportHeader.ReportHeaderBuilder;
import gcm.output.reports.ReportItem.ReportItemBuilder;
import gcm.output.reports.StateChange;
import gcm.scenario.CompartmentId;
import gcm.scenario.PersonId;
import gcm.scenario.RegionId;
import gcm.scenario.ResourceId;
import gcm.simulation.ObservableEnvironment;
import gcm.util.annotations.Source;
import gcm.util.annotations.TestStatus;

import java.util.*;

/**
 * A periodic Report that displays number of people who have/do not have any
 * units of a particular resource with a region/compartment pair. Adapted from
 * PersonResourceReport in GCM.
 * <p>
 * <p>
 * Fields
 * <p>
 * Region -- the region identifier
 * <p>
 * Compartment -- the compartment identifier
 * <p>
 * Resource -- the resource identifier
 * <p>
 * PeopleWithResource -- the number of people in the region/compartment pair who
 * have at least one unit of the given resource
 * <p>
 * PeopleWithoutResource -- the number of people in the region/compartment pair
 * who do not have any units of the given resource
 *
 * @author Shawn Hatch
 */
@Source(status = TestStatus.UNEXPECTED)
public final class PersonRegionResourceReport extends RegionAggregationPeriodicReport {
    /*
     * The resources that will be used in this report. They are derived from the
     * values passed in the init() method.
     */
    private final Set<ResourceId> resourceIds = new LinkedHashSet<>();
    // Mapping of the (regionId, compartmentId, resource Id, InventoryType) to
    // sets of person id. Maintained via the processing of events.
    private final Map<String, Map<CompartmentId, Map<ResourceId, Map<InventoryType, Set<PersonId>>>>> regionMap = new LinkedHashMap<>();
    /*
     * Boolean for controlling the reporting of people with out resources. Set
     * in the init() method.
     */
    private boolean reportPeopleWithoutResources;
    /*
     * Boolean for controlling the reporting of people with out resources. Set
     * in the init() method.
     */
    private boolean reportZeroPopulations;
    /*
     * The derived header for this report
     */
    private ReportHeader reportHeader;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeaderBuilder reportHeaderBuilder = new ReportHeaderBuilder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeaderBuilder.add("Region");
            reportHeaderBuilder.add("Compartment");
            reportHeaderBuilder.add("Resource");
            reportHeaderBuilder.add("PeopleWithResource");
            if (reportPeopleWithoutResources) {
                reportHeaderBuilder.add("PeopleWithoutResource");
            }
            reportHeader = reportHeaderBuilder.build();
        }
        return reportHeader;
    }

    /*
     * Adds a person to the set of people associated with the given tuple
     */
    private void add(final RegionId regionId, final CompartmentId compartmentId, final ResourceId resourceId, final InventoryType inventoryType, final PersonId personId) {
        final Set<PersonId> people = regionMap.get(getFipsString(regionId)).get(compartmentId).get(resourceId).get(inventoryType);
        people.add(personId);
    }

    @Override
    protected void flush(ObservableEnvironment observableEnvironment) {
        final ReportItemBuilder reportItemBuilder = new ReportItemBuilder();
        for (final String regionId : regionMap.keySet()) {
            final Map<CompartmentId, Map<ResourceId, Map<InventoryType, Set<PersonId>>>> compartmentMap = regionMap.get(regionId);
            for (final CompartmentId compartmentId : compartmentMap.keySet()) {
                final Map<ResourceId, Map<InventoryType, Set<PersonId>>> resourceMap = compartmentMap.get(compartmentId);
                for (final ResourceId resourceId : resourceIds) {
                    final Map<InventoryType, Set<PersonId>> inventoryMap = resourceMap.get(resourceId);

                    final int positiveCount = inventoryMap.get(InventoryType.POSITIVE).size();
                    int count = positiveCount;
                    final int zeroCount = inventoryMap.get(InventoryType.ZERO).size();
                    if (reportPeopleWithoutResources) {
                        count += zeroCount;
                    }
                    final boolean shouldReport = reportZeroPopulations || (count > 0);

                    if (shouldReport) {
                        reportItemBuilder.setReportHeader(getReportHeader());
                        reportItemBuilder.setReportType(getClass());
                        reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                        reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());

                        buildTimeFields(reportItemBuilder);
                        reportItemBuilder.addValue(regionId);
                        reportItemBuilder.addValue(compartmentId.toString());
                        reportItemBuilder.addValue(resourceId.toString());
                        reportItemBuilder.addValue(positiveCount);
                        if (reportPeopleWithoutResources) {
                            reportItemBuilder.addValue(zeroCount);
                        }
                        observableEnvironment.releaseOutputItem(reportItemBuilder.build());
                    }
                }
            }
        }
    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        final Set<StateChange> result = new LinkedHashSet<>();
        result.add(StateChange.PERSON_ADDITION);
        result.add(StateChange.PERSON_REMOVAL);
        result.add(StateChange.REGION_ASSIGNMENT);
        result.add(StateChange.COMPARTMENT_ASSIGNMENT);
        result.add(StateChange.PERSON_RESOURCE_REMOVAL);
        result.add(StateChange.PERSON_RESOURCE_TRANSFER_TO_REGION);
        result.add(StateChange.REGION_RESOURCE_TRANSFER_TO_PERSON);
        result.add(StateChange.PERSON_RESOURCE_ADDITION);
        return result;
    }

    @Override
    public void handleCompartmentAssignment(ObservableEnvironment observableEnvironment, final PersonId personId, final CompartmentId sourceCompartmentId) {
        setCurrentReportingPeriod(observableEnvironment);
        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);

        for (final ResourceId resourceId : resourceIds) {

            final long personResourceLevel = observableEnvironment.getPersonResourceLevel(personId, resourceId);

            if (personResourceLevel > 0) {
                remove(regionId, sourceCompartmentId, resourceId, InventoryType.POSITIVE, personId);
                add(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            } else {
                if (reportPeopleWithoutResources) {
                    remove(regionId, sourceCompartmentId, resourceId, InventoryType.ZERO, personId);
                    add(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    @Override
    public void handlePersonAddition(ObservableEnvironment observableEnvironment, final PersonId personId) {
        setCurrentReportingPeriod(observableEnvironment);
        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);

        for (final ResourceId resourceId : resourceIds) {
            final long personResourceLevel = observableEnvironment.getPersonResourceLevel(personId, resourceId);
            if (personResourceLevel > 0) {
                add(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            } else {
                if (reportPeopleWithoutResources) {
                    add(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    @Override
    public void handlePersonRemoval(ObservableEnvironment observableEnvironment, PersonInfo personInfo) {
        setCurrentReportingPeriod(observableEnvironment);
        Map<ResourceId, Long> resourceValues = personInfo.getResourceValues();
        RegionId regionId = personInfo.getRegionId();
        CompartmentId compartmentId = personInfo.getCompartmentId();
        PersonId personId = personInfo.getPersonId();
        for (final ResourceId resourceId : resourceIds) {
            final Long resourceValue = resourceValues.get(resourceId);
            if (resourceValue != null) {
                if (resourceValue > 0) {
                    remove(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
                } else {
                    if (reportPeopleWithoutResources) {
                        remove(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                    }
                }
            }
        }
    }

    @Override
    public void handlePersonResourceAddition(ObservableEnvironment observableEnvironment, final PersonId personId, final ResourceId resourceId, final long amount) {
        if (amount > 0 && resourceIds.contains(resourceId)) {
            setCurrentReportingPeriod(observableEnvironment);
            final long personResourceLevel = observableEnvironment.getPersonResourceLevel(personId, resourceId);
            if (personResourceLevel == amount) {
                final RegionId regionId = observableEnvironment.getPersonRegion(personId);
                final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
                if (reportPeopleWithoutResources) {
                    remove(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
                add(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            }
        }
    }

    @Override
    public void handlePersonResourceRemoval(ObservableEnvironment observableEnvironment, final PersonId personId, final ResourceId resourceId, final long amount) {
        if (amount > 0 && resourceIds.contains(resourceId)) {
            setCurrentReportingPeriod(observableEnvironment);
            final long personResourceLevel = observableEnvironment.getPersonResourceLevel(personId, resourceId);
            if (personResourceLevel == 0) {
                final RegionId regionId = observableEnvironment.getPersonRegion(personId);
                final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
                remove(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
                if (reportPeopleWithoutResources) {
                    add(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    @Override
    public void handlePersonResourceTransferToRegion(ObservableEnvironment observableEnvironment, final PersonId personId, final ResourceId resourceId, final long amount) {
        if (amount > 0 && resourceIds.contains(resourceId)) {
            setCurrentReportingPeriod(observableEnvironment);
            final long personResourceLevel = observableEnvironment.getPersonResourceLevel(personId, resourceId);
            if (personResourceLevel == 0) {
                final RegionId regionId = observableEnvironment.getPersonRegion(personId);
                final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
                remove(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
                if (reportPeopleWithoutResources) {
                    add(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    @Override
    public void handleRegionAssignment(ObservableEnvironment observableEnvironment, final PersonId personId, final RegionId sourceRegionId) {
        setCurrentReportingPeriod(observableEnvironment);
        final RegionId regionId = observableEnvironment.getPersonRegion(personId);
        final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
        for (final ResourceId resourceId : resourceIds) {
            final long personResourceLevel = observableEnvironment.getPersonResourceLevel(personId, resourceId);
            if (personResourceLevel > 0) {
                remove(sourceRegionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
                add(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            } else {
                if (reportPeopleWithoutResources) {
                    remove(sourceRegionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                    add(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    @Override
    public void handleRegionResourceTransferToPerson(ObservableEnvironment observableEnvironment, final PersonId personId, final ResourceId resourceId, final long amount) {
        if (amount > 0 && resourceIds.contains(resourceId)) {
            setCurrentReportingPeriod(observableEnvironment);
            final long personResourceLevel = observableEnvironment.getPersonResourceLevel(personId, resourceId);
            if (personResourceLevel == amount) {
                final RegionId regionId = observableEnvironment.getPersonRegion(personId);
                final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
                if (reportPeopleWithoutResources) {
                    remove(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
                add(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            }
        }
    }

    @Override
    public void init(final ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        super.init(observableEnvironment, initialData);
        /*
         * Determine the resources and display options from the initial data
         */
        for (Object initialDatum : initialData) {
            if (initialDatum instanceof ResourceId) {
                ResourceId resourceId = (ResourceId) initialDatum;
                resourceIds.add(resourceId);
            } else if (initialDatum instanceof PersonResourceReportOption) {
                PersonResourceReportOption personResourceReportOption = (PersonResourceReportOption) initialDatum;
                switch (personResourceReportOption) {
                    case REPORT_PEOPLE_WITHOUT_RESOURCES:
                        reportPeopleWithoutResources = true;
                        break;
                    case REPORT_ZERO_POPULATIONS:
                        reportZeroPopulations = true;
                        break;
                    default:
                        throw new RuntimeException("unhandled PersonResourceReportOption");
                }
            }
        }

        /*
         * If no resources were selected, then assume that all are desired.
         */
        if (resourceIds.size() == 0) {
            resourceIds.addAll(observableEnvironment.getResourceIds());
        }

        /*
         * Ensure that the resources are valid
         */
        final Set<ResourceId> validResourceIds = observableEnvironment.getResourceIds();
        for (final ResourceId resourceId : resourceIds) {
            if (!validResourceIds.contains(resourceId)) {
                throw new RuntimeException("invalid resource id " + resourceId);
            }
        }

        /*
         * Build the tuple map to empty sets of people in preparation for people
         * being added to the simulation
         */
        for (final RegionId regionId : observableEnvironment.getRegionIds()) {
            final Map<CompartmentId, Map<ResourceId, Map<InventoryType, Set<PersonId>>>> compartmentMap = new LinkedHashMap<>();
            regionMap.put(getFipsString(regionId), compartmentMap);
            for (final CompartmentId compartmentId : observableEnvironment.getCompartmentIds()) {
                final Map<ResourceId, Map<InventoryType, Set<PersonId>>> resourceMap = new LinkedHashMap<>();
                compartmentMap.put(compartmentId, resourceMap);
                for (final ResourceId resourceId : resourceIds) {
                    final Map<InventoryType, Set<PersonId>> inventoryMap = new LinkedHashMap<>();
                    resourceMap.put(resourceId, inventoryMap);
                    for (final InventoryType inventoryType : InventoryType.values()) {
                        final Set<PersonId> people = new HashSet<>();
                        inventoryMap.put(inventoryType, people);
                    }
                }
            }
        }

        /*
         * Place the initial population in the mapping
         */
        setCurrentReportingPeriod(observableEnvironment);
        for (final PersonId personId : observableEnvironment.getPeople()) {
            for (final ResourceId resourceId : resourceIds) {
                final RegionId regionId = observableEnvironment.getPersonRegion(personId);
                final CompartmentId compartmentId = observableEnvironment.getPersonCompartment(personId);
                final long personResourceLevel = observableEnvironment.getPersonResourceLevel(personId, resourceId);
                if (personResourceLevel > 0) {
                    add(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
                } else {
                    if (reportPeopleWithoutResources) {
                        add(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                    }
                }
            }
        }


    }

    /*
     * Removes a person to the set of people associated with the given tuple
     */
    private void remove(final RegionId regionId, final CompartmentId compartmentId, final ResourceId resourceId, final InventoryType inventoryType, final PersonId personId) {
        if (resourceIds.contains(resourceId)) {
            final Set<PersonId> people = regionMap.get(getFipsString(regionId)).get(compartmentId).get(resourceId).get(inventoryType);
            people.remove(personId);
        }
    }

    /**
     * An enumeration that represents two boolean options for this report. They
     * are implemented as an enumeration so that they can be passes
     * unambiguously in the varargs of the init() method;
     *
     * @author Shawn Hatch
     */
    public enum PersonResourceReportOption {
        REPORT_PEOPLE_WITHOUT_RESOURCES, REPORT_ZERO_POPULATIONS
    }

    /**
     * An enumeration mirroring the differentiation in the report for populations
     * of people with and without a resource.
     *
     * @author Shawn Hatch
     */
    private enum InventoryType {
        ZERO, POSITIVE
    }

}