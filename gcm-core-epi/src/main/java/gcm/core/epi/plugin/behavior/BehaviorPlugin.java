package gcm.core.epi.plugin.behavior;

import com.fasterxml.jackson.databind.JsonNode;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import gcm.core.epi.util.property.DefinedGlobalAndRegionProperty;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.PersonId;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BehaviorPlugin implements Plugin {

    /*
        Get the regional value of a global property with a potential regional triggered override
     */
    static <T> T getRegionalPropertyValue(Environment environment, RegionId regionId, DefinedGlobalAndRegionProperty propertyId) {
        T regionPropertyValue = environment.getRegionPropertyValue(regionId, propertyId);
        //noinspection OptionalGetWithoutIsPresent
        if (regionPropertyValue.equals(environment.getRegionPropertyDefinition(propertyId).getDefaultValue().get())) {
            return environment.getGlobalPropertyValue(propertyId);
        } else {
            return regionPropertyValue;
        }
    }

    /*
        Add trigger callbacks for each of the triggered property overrides
     */
    static void addTriggerOverrideCallbacks(Map<String, Set<TriggerCallback>> triggerCallbacks,
                                            List<TriggeredPropertyOverride> triggeredPropertyOverrides,
                                            Set<DefinedGlobalAndRegionProperty> overrideableProperties,
                                            Environment environment) {
        String triggerId;
        for (TriggeredPropertyOverride override : triggeredPropertyOverrides) {
            triggerId = override.trigger();
            Map<String, JsonNode> propertyOverrides = override.overrides();
            Map<String, DefinedGlobalAndRegionProperty> propertyNameMap = overrideableProperties.stream()
                    .collect(Collectors.toMap(
                            Objects::toString,
                            Function.identity()
                    ));
            final Map<DefinedGlobalAndRegionProperty, Object> overrideValues = new HashMap<>();
            PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
            AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
            propertyOverrides.forEach(
                    (propertyId, valueJson) -> {
                        DefinedGlobalAndRegionProperty property = propertyNameMap.get(propertyId);
                        if (property == null) {
                            throw new RuntimeException("Unrecognized property for triggered parameter override: " + propertyId);
                        }
                        try {
                            Object overrideValue = CoreEpiBootstrapUtil.getPropertyValueFromJson(valueJson,
                                    property, ageGroupPartition);
                            overrideValues.put(property, overrideValue);
                        } catch (IOException e) {
                            throw new RuntimeException("Property override value cannot be parsed from: " + valueJson);
                        }
                    }
            );
            TriggerUtils.addCallback(triggerCallbacks, triggerId,
                    (env, regionId) -> {
                        // Override each value
                        overrideValues.forEach(
                                (property, overrideValue) -> env.setRegionPropertyValue(regionId, property, overrideValue)
                        );
                    });
        }
    }

    /*
        Handle a person learning they may be infected
     */
    public void handleSuspectedInfected(Environment environment, PersonId personId) {
        // Do nothing by default
    }

    /*
        Handle a person becoming aware of an infection in their home
     */
    public void handleHomeInfection(Environment environment, PersonId personId) {
        // Do nothing by default
    }

    /*
        Gets the relative frequency of attempted transmission for the person taking into account behavior change
     */
    public double getRelativeActivityLevel(Environment environment, PersonId personId) {
        // Do not change this by default
        return 1.0;
    }

    /*
        Get a substitute contact group
     */
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId,
                                                                 ContactGroupType selectedContactGroupType) {
        // Do not substitute contact group by default
        return Optional.of(selectedContactGroupType);
    }

    /*
        Get the (generally reduced) probability of infection for the specified person due to behavior change
     */
    public double getInfectionProbability(Environment environment, ContactGroupType contactSetting, PersonId personId) {
        // Do not change this by default
        return 1.0;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        Plugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN, Optional.of(this));
    }

    /*
        Get the collection of triggers that will fire the corresponding callback methods
     */
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        return new HashMap<>();
    }

}
