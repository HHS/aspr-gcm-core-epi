package gcm.core.epi.plugin;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.util.property.*;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.RandomNumberGeneratorId;
import gcm.scenario.ResourceId;

import java.util.*;

public interface Plugin {

    /*
        Gets the global properties that need to be added to the simulation
     */
    default Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>();
    }

    /*
        Gets the region properties that need to be added to the simulation
     */
    default Set<DefinedRegionProperty> getRegionProperties() {
        return new HashSet<>();
    }

    /*
        Gets the person properties that need to be added to the simulation
     */
    default Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>();
    }

    /*
        Gets the group properties that need to be added to the simulation
     */
    default Map<ContactGroupType, Set<DefinedGroupProperty>> getGroupProperties() {
        return new HashMap<>();
    }

    /*
        Gets the resources and properties that need to be added to the simulation
     */
    default Map<ResourceId, Set<DefinedResourceProperty>> getResourceProperties() {
        return new HashMap<>();
    }

    /*
        Gets the random IDs used by this simulation
     */
    default List<RandomNumberGeneratorId> getRandomIds() {
        return new ArrayList<>();
    }

    /*
        Load all of the identifiers, components, and property definitions that will be used by the plugin
     */
    default void load(ExperimentBuilder experimentBuilder) {

        for (DefinedGlobalProperty globalProperty : getGlobalProperties()) {
            experimentBuilder.defineGlobalProperty(globalProperty, globalProperty.getPropertyDefinition());
        }

        for (DefinedRegionProperty regionProperty : getRegionProperties()) {
            experimentBuilder.defineRegionProperty(regionProperty, regionProperty.getPropertyDefinition());
        }

        for (DefinedPersonProperty personProperty : getPersonProperties()) {
            experimentBuilder.definePersonProperty(personProperty, personProperty.getPropertyDefinition());
        }

        for (Map.Entry<ContactGroupType, Set<DefinedGroupProperty>> entry : getGroupProperties().entrySet()) {
            for (DefinedGroupProperty groupProperty : entry.getValue()) {
                experimentBuilder.defineGroupProperty(entry.getKey(), groupProperty,
                        groupProperty.getPropertyDefinition());
            }
        }

        for (Map.Entry<ResourceId, Set<DefinedResourceProperty>> entry : getResourceProperties().entrySet()) {
            ResourceId resourceId = entry.getKey();
            experimentBuilder.addResource(resourceId);
            for (DefinedResourceProperty resourceProperty : entry.getValue()) {
                experimentBuilder.defineResourceProperty(resourceId, resourceProperty,
                        resourceProperty.getPropertyDefinition());
            }
        }

        for (RandomNumberGeneratorId randomId : getRandomIds()) {
            experimentBuilder.addRandomNumberGeneratorId(randomId);
        }

    }

}
