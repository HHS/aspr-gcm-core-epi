package gcm.core.epi.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.behavior.TriggeredPropertyOverride;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.FipsCodeValue;
import gcm.core.epi.trigger.*;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import gcm.core.epi.util.property.*;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.RandomNumberGeneratorId;
import gcm.scenario.RegionId;
import gcm.scenario.ResourceId;
import gcm.simulation.Environment;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Plugin {

    /*
        Get the regional value of a global property with a potential regional triggered override
     */
    static <T> T getRegionalPropertyValue(Environment environment, RegionId regionId, DefinedGlobalAndRegionProperty propertyId) {
        if (environment.getRegionPropertyTime(regionId, propertyId.getRegionProperty()) > 0) {
            return environment.getRegionPropertyValue(regionId, propertyId.getRegionProperty());
        } else {
            FipsCodeValue<T> globalPropertyValue = environment.getGlobalPropertyValue(propertyId);
            return globalPropertyValue.getValue(regionId);
        }
    }

    /*
        Add trigger callbacks for each of the triggered property overrides
     */
    static void addTriggerOverrideCallbacks(Map<String, Set<TriggerCallback>> triggerCallbacks,
                                            List<TriggeredPropertyOverride> triggeredPropertyOverrides,
                                            Set<DefinedGlobalAndRegionProperty> overrideableProperties,
                                            Environment environment) {
        addTriggerOverrideCallbacks(triggerCallbacks, triggeredPropertyOverrides, overrideableProperties,
                new HashMap<>(), environment);
    }

    /*
        Add trigger callbacks for each of the triggered property overrides with validation
     */
    static void addTriggerOverrideCallbacks(Map<String, Set<TriggerCallback>> triggerCallbacks,
                                            List<TriggeredPropertyOverride> triggeredPropertyOverrides,
                                            Set<DefinedGlobalAndRegionProperty> overrideableProperties,
                                            Map<DefinedGlobalAndRegionProperty, TriggerOverrideValidator> validators,
                                            Environment environment) {
        String triggerStringId;
        for (TriggeredPropertyOverride override : triggeredPropertyOverrides) {
            triggerStringId = override.trigger();
            // Get the trigger for validation
            TriggerContainer triggerContainer = environment.getGlobalPropertyValue(GlobalProperty.TRIGGER_CONTAINER);
            TriggerId<Trigger> triggerId = triggerContainer.getId(triggerStringId);
            if (triggerId == null) {
                throw new RuntimeException("Override trigger id is invalid: " + triggerStringId);
            }
            Map<String, JsonNode> propertyOverrides = override.overrides();
            Map<String, DefinedGlobalAndRegionProperty> propertyNameMap = overrideableProperties.stream()
                    .collect(Collectors.toMap(
                            Objects::toString,
                            Function.identity()
                    ));
            final Map<DefinedGlobalAndRegionProperty, FipsCodeValue<?>> overrideValues = new HashMap<>();
            PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
            AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
            propertyOverrides.forEach(
                    (propertyId, valueJson) -> {
                        DefinedGlobalAndRegionProperty property = propertyNameMap.get(propertyId);
                        if (property == null) {
                            throw new RuntimeException("Unrecognized property for triggered parameter override: " + propertyId);
                        }
                        try {
                            // Get the property value - we know the property has type FipsCodeValue<T>
                            //noinspection unchecked
                            FipsCodeValue<?> overrideValue = (FipsCodeValue<?>) CoreEpiBootstrapUtil.getPropertyValueFromJson(valueJson,
                                    property.getPropertyDefinition().overrideJavaType(), ageGroupPartition);
                            if (validators.containsKey(property)) {
                                // Throw an exception if this is an invalid property value
                                validators.get(property).validate(environment, triggerId, overrideValue);
                            }
                            overrideValues.put(property, overrideValue);
                        } catch (IOException e) {
                            throw new RuntimeException("Property override value cannot be parsed from: " + valueJson);
                        }
                    }
            );
            TriggerUtils.addCallback(triggerCallbacks, triggerStringId,
                    (env, regionId) -> {
                        // Override each value
                        overrideValues.forEach(
                                (property, overrideValue) -> env.setRegionPropertyValue(regionId, property.getRegionProperty(), overrideValue.getValue(regionId))
                        );
                    });
        }
    }

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
    default Map<ResourceId, Set<DefinedResourceProperty>> getResourcesAndProperties() {
        return new HashMap<>();
    }

    /*
        Gets the random IDs used by this simulation
     */
    default List<RandomNumberGeneratorId> getRandomIds() {
        return new ArrayList<>();
    }

    /*
    Get the collection of triggers that will fire the corresponding callback methods
    */
    default Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        return new HashMap<>();
    }

    /*
        Load all of the identifiers, components, and property definitions that will be used by the plugin
     */
    default void load(ExperimentBuilder experimentBuilder) {

        for (DefinedGlobalProperty globalProperty : getGlobalProperties()) {
            experimentBuilder.defineGlobalProperty(globalProperty,
                    globalProperty.getPropertyDefinition().definition());
        }

        for (DefinedRegionProperty regionProperty : getRegionProperties()) {
            experimentBuilder.defineRegionProperty(regionProperty,
                    regionProperty.getPropertyDefinition().definition());
        }

        for (DefinedPersonProperty personProperty : getPersonProperties()) {
            experimentBuilder.definePersonProperty(personProperty,
                    personProperty.getPropertyDefinition().definition());
        }

        for (Map.Entry<ContactGroupType, Set<DefinedGroupProperty>> entry : getGroupProperties().entrySet()) {
            for (DefinedGroupProperty groupProperty : entry.getValue()) {
                experimentBuilder.defineGroupProperty(entry.getKey(), groupProperty,
                        groupProperty.getPropertyDefinition().definition());
            }
        }

        for (Map.Entry<ResourceId, Set<DefinedResourceProperty>> entry : getResourcesAndProperties().entrySet()) {
            ResourceId resourceId = entry.getKey();
            experimentBuilder.addResource(resourceId);
            for (DefinedResourceProperty resourceProperty : entry.getValue()) {
                experimentBuilder.defineResourceProperty(resourceId, resourceProperty,
                        resourceProperty.getPropertyDefinition().definition());
            }
        }

        for (RandomNumberGeneratorId randomId : getRandomIds()) {
            experimentBuilder.addRandomNumberGeneratorId(randomId);
        }

    }

}
