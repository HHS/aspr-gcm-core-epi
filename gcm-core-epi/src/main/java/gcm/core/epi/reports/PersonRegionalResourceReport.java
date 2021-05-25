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
import plugins.regions.datacontainers.RegionDataView;
import plugins.regions.datacontainers.RegionLocationDataView;
import plugins.regions.events.observation.PersonRegionChangeObservationEvent;
import plugins.regions.support.RegionId;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;
import plugins.reports.support.ReportPeriod;
import plugins.resources.datacontainers.ResourceDataView;
import plugins.resources.events.observation.PersonResourceChangeObservationEvent;
import plugins.resources.support.ResourceId;
import util.annotations.Source;
import util.annotations.TestStatus;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
public final class PersonRegionalResourceReport extends RegionAggregationPeriodicReport {
    /*
     * The resources that will be used in this report. They are derived from the
     * values passed in the init() method.
     */
    private final Set<ResourceId> resourceIds = new LinkedHashSet<>();
    // Mapping of the (regionId, compartmentId, resource Id, InventoryType) to
    // sets of person id. Maintained via the processing of events.
    private final Map<String, Map<CompartmentId, Map<ResourceId, Map<InventoryType, Set<PersonId>>>>> regionMap = new LinkedHashMap<>();
    /*
     * Boolean for controlling the reporting of people without resources.
     */
    private final boolean reportPeopleWithoutResources;
    /*
     * Boolean for controlling the reporting of people without resources.
     */
    private final boolean reportZeroPopulations;
    /*
     * The derived header for this report
     */
    private final ReportHeader reportHeader;
    private CompartmentLocationDataView compartmentLocationDataView;
    private RegionLocationDataView regionLocationDataView;
    private ResourceDataView resourceDataView;

    public PersonRegionalResourceReport(ReportPeriod reportPeriod, FipsScope fipsScope,
                                        boolean reportPeopleWithoutResources, boolean reportZeroPopulations, ResourceId...resourceIds) {
        super(reportPeriod, fipsScope);
        this.reportPeopleWithoutResources = reportPeopleWithoutResources;
        this.reportZeroPopulations = reportZeroPopulations;
        for(ResourceId resourceId : resourceIds) {
            this.resourceIds.add(resourceId);
        }
        ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
        addTimeFieldHeaders(reportHeaderBuilder);
        reportHeaderBuilder
                .add("Region")
                .add("Compartment")
                .add("Resource")
                .add("PeopleWithResource");
        if (reportPeopleWithoutResources) {
            reportHeaderBuilder.add("PeopleWithoutResource");
        }
        reportHeader = reportHeaderBuilder.build();
    }

    /*
     * Adds a person to the set of people associated with the given tuple
     */
    private void add(final RegionId regionId, final CompartmentId compartmentId, final ResourceId resourceId, final InventoryType inventoryType, final PersonId personId) {
        final Set<PersonId> people = regionMap.get(getFipsString(regionId)).get(compartmentId).get(resourceId).get(inventoryType);
        people.add(personId);
    }

    @Override
    protected void flush(ReportContext reportContext) {
        final ReportItem.Builder reportItemBuilder = ReportItem.builder();
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
                        reportItemBuilder.setReportHeader(reportHeader);
                        reportItemBuilder.setReportId(reportContext.getCurrentReportId());
                        fillTimeFields(reportItemBuilder);
                        reportItemBuilder.addValue(regionId);
                        reportItemBuilder.addValue(compartmentId.toString());
                        reportItemBuilder.addValue(resourceId.toString());
                        reportItemBuilder.addValue(positiveCount);
                        if (reportPeopleWithoutResources) {
                            reportItemBuilder.addValue(zeroCount);
                        }
                        reportContext.releaseOutput(reportItemBuilder.build());
                    }
                }
            }
        }
    }

    private void handlePersonCompartmentChangeObservationEvent(ReportContext reportContext, PersonCompartmentChangeObservationEvent personCompartmentChangeObservationEvent) {
        PersonId personId = personCompartmentChangeObservationEvent.getPersonId();
        CompartmentId sourceCompartmentId = personCompartmentChangeObservationEvent.getPreviousCompartmentId();
        final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
        final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);

        for (final ResourceId resourceId : resourceIds) {
            final long personResourceLevel = resourceDataView.getPersonResourceLevel(resourceId, personId);
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

    private void handlePersonCreationObservationEvent(ReportContext reportContext, PersonCreationObservationEvent personCreationObservationEvent) {
        PersonId personId = personCreationObservationEvent.getPersonId();
        final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
        final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);

        for (final ResourceId resourceId : resourceIds) {
            final long personResourceLevel = resourceDataView.getPersonResourceLevel(resourceId, personId);
            if (personResourceLevel > 0) {
                add(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            } else {
                if (reportPeopleWithoutResources) {
                    add(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    private void handlePersonImminentRemovalObservationEvent(ReportContext reportContext, PersonImminentRemovalObservationEvent personImminentRemovalObservationEvent) {
        PersonId personId = personImminentRemovalObservationEvent.getPersonId();
        RegionId regionId = regionLocationDataView.getPersonRegion(personId);
        CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
        for (ResourceId resourceId : resourceIds) {
            long amount = resourceDataView.getPersonResourceLevel(resourceId, personId);
            if (amount > 0) {
                remove(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            } else {
                if (reportPeopleWithoutResources) {
                    remove(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    private void handlePersonResourceChangeObservationEvent(ReportContext reportContext, PersonResourceChangeObservationEvent personResourceChangeObservationEvent) {
        ResourceId resourceId = personResourceChangeObservationEvent.getResourceId();
        if (!resourceIds.contains(resourceId)) {
            return;
        }
        PersonId personId = personResourceChangeObservationEvent.getPersonId();
        long currentLevel = personResourceChangeObservationEvent.getCurrentResourceLevel();
        long previousLevel = personResourceChangeObservationEvent.getPreviousResourceLevel();
        long amount = currentLevel - previousLevel;

        if (amount == 0) {
            return;
        }
        if (amount > 0) {
            final long personResourceLevel = resourceDataView.getPersonResourceLevel(resourceId, personId);
            if (personResourceLevel == amount) {
                final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
                final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
                if (reportPeopleWithoutResources) {
                    remove(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
                add(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            }
        } else {
            amount = -amount;
            final long personResourceLevel = resourceDataView.getPersonResourceLevel(resourceId, personId);
            if (personResourceLevel == 0) {
                final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
                final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
                remove(regionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
                if (reportPeopleWithoutResources) {
                    add(regionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    private void handlePersonRegionChangeObservationEvent(ReportContext reportContext, PersonRegionChangeObservationEvent personRegionChangeObservationEvent) {
        PersonId personId = personRegionChangeObservationEvent.getPersonId();
        RegionId previousRegionId = personRegionChangeObservationEvent.getPreviousRegionId();
        RegionId currentRegionId = personRegionChangeObservationEvent.getCurrentRegionId();

        final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
        for (final ResourceId resourceId : resourceIds) {
            final long personResourceLevel = resourceDataView.getPersonResourceLevel(resourceId, personId);
            if (personResourceLevel > 0) {
                remove(previousRegionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
                add(currentRegionId, compartmentId, resourceId, InventoryType.POSITIVE, personId);
            } else {
                if (reportPeopleWithoutResources) {
                    remove(previousRegionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                    add(currentRegionId, compartmentId, resourceId, InventoryType.ZERO, personId);
                }
            }
        }
    }

    @Override
    public void init(final ReportContext reportContext) {
        super.init(reportContext);

        reportContext.subscribeToEvent(PersonResourceChangeObservationEvent.class, this::handlePersonCompartmentChangeObservationEvent);
        reportContext.subscribeToEvent(PersonCreationObservationEvent.class, this::handlePersonCreationObservationEvent);
        reportContext.subscribeToEvent(PersonImminentRemovalObservationEvent.class, this::handlePersonImminentRemovalObservationEvent);
        reportContext.subscribeToEvent(PersonCompartmentChangeObservationEvent.class, this::handlePersonRegionChangeObservationEvent);
        reportContext.subscribeToEvent(PersonRegionChangeObservationEvent.class, this::handlePersonResourceChangeObservationEvent);

        resourceDataView = reportContext.getDataView(ResourceDataView.class).get();
        PersonDataView personDataView = reportContext.getDataView(PersonDataView.class).get();
        compartmentLocationDataView = reportContext.getDataView(CompartmentLocationDataView.class).get();
        regionLocationDataView = reportContext.getDataView(RegionLocationDataView.class).get();
        CompartmentDataView compartmentDataView = reportContext.getDataView(CompartmentDataView.class).get();
        RegionDataView regionDataView = reportContext.getDataView(RegionDataView.class).get();

        /*
         * If no resources were selected, then assume that all are desired.
         */
        if (resourceIds.size() == 0) {
            resourceIds.addAll(resourceDataView.getResourceIds());
        }

        /*
         * Ensure that the resources are valid
         */
        final Set<ResourceId> validResourceIds = resourceDataView.getResourceIds();
        for (final ResourceId resourceId : resourceIds) {
            if (!validResourceIds.contains(resourceId)) {
                throw new RuntimeException("invalid resource id " + resourceId);
            }
        }

        /*
         * Build the tuple map to empty sets of people in preparation for people
         * being added to the simulation
         */
        Set<CompartmentId> compartmentIds = compartmentDataView.getCompartmentIds();
        for (final RegionId regionId : regionDataView.getRegionIds()) {
            final Map<CompartmentId, Map<ResourceId, Map<InventoryType, Set<PersonId>>>> compartmentMap = new LinkedHashMap<>();
            regionMap.put(getFipsString(regionId), compartmentMap);
            for (final CompartmentId compartmentId : compartmentIds) {
                final Map<ResourceId, Map<InventoryType, Set<PersonId>>> resourceMap = new LinkedHashMap<>();
                compartmentMap.put(compartmentId, resourceMap);
                for (final ResourceId resourceId : resourceIds) {
                    final Map<InventoryType, Set<PersonId>> inventoryMap = new LinkedHashMap<>();
                    resourceMap.put(resourceId, inventoryMap);
                    for (final InventoryType inventoryType : InventoryType.values()) {
                        final Set<PersonId> people = new LinkedHashSet<>();
                        inventoryMap.put(inventoryType, people);
                    }
                }
            }
        }

        /*
         * Place the initial population in the mapping
         */
        for (final PersonId personId : personDataView.getPeople()) {
            for (final ResourceId resourceId : resourceIds) {
                final RegionId regionId = regionLocationDataView.getPersonRegion(personId);
                final CompartmentId compartmentId = compartmentLocationDataView.getPersonCompartment(personId);
                final long personResourceLevel = resourceDataView.getPersonResourceLevel(resourceId, personId);
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