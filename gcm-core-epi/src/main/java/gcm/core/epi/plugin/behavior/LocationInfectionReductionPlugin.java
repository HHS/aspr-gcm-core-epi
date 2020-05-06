package gcm.core.epi.plugin.behavior;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.population.Util;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import gcm.core.epi.util.property.DefinedGlobalAndRegionProperty;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;

import java.io.IOException;
import java.util.*;

public class LocationInfectionReductionPlugin extends BehaviorPlugin {
    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        Set<DefinedGlobalProperty> globalProperties = new HashSet<>();
        globalProperties.addAll(EnumSet.allOf(LocationInfectionReductionGlobalProperty.class));
        globalProperties.addAll(EnumSet.allOf(LocationInfectionReductionGlobalAndRegionProperty.class));
        return globalProperties;
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(LocationInfectionReductionPlugin.LocationInfectionReductionRandomId.ID);
        return randomIds;
    }

    @Override
    public double getInfectionProbability(Environment environment, ContactGroupType contactSetting, PersonId personId) {
        // Check if we even need to think about whether to reduce infection probability
        RegionId regionId = environment.getPersonRegion(personId);
        boolean triggerIsInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                LocationInfectionReductionRegionProperty.LOCATION_INFECTION_REDUCTION_TRIGGER_START,
                LocationInfectionReductionRegionProperty.LOCATION_INFECTION_REDUCTION_TRIGGER_END);

        double infectionReduction = 0.0;
        if (triggerIsInEffect) {
            // We are in the timeframe when infections should be reduced
            AgeGroup ageGroup = Util.getAgeGroupForPerson(environment, personId);
            // See if region property value has been set
            Map<AgeGroup, Map<ContactGroupType, Double>> infectionReductionByAgeAndLocationMap =
                    environment.getRegionPropertyValue(regionId,
                            LocationInfectionReductionGlobalAndRegionProperty.LOCATION_INFECTION_REDUCTION);
            //noinspection OptionalGetWithoutIsPresent
            if (infectionReductionByAgeAndLocationMap.equals(environment.getRegionPropertyDefinition(
                    LocationInfectionReductionGlobalAndRegionProperty.LOCATION_INFECTION_REDUCTION)
                    .getDefaultValue().get())) {
                // If still is the default region property, use the value from global definition
                infectionReductionByAgeAndLocationMap = environment.getGlobalPropertyValue(
                        LocationInfectionReductionGlobalAndRegionProperty.LOCATION_INFECTION_REDUCTION);
            }
            Map<ContactGroupType, Double> infectionReductionByLocationMap = infectionReductionByAgeAndLocationMap
                    .getOrDefault(ageGroup, new HashMap<>());
            infectionReduction = infectionReductionByLocationMap.getOrDefault(contactSetting, 0.0);
        }

        return 1.0 - infectionReduction;
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        HashSet<DefinedRegionProperty> regionProperties = new HashSet<>();
        regionProperties.addAll(EnumSet.allOf(LocationInfectionReductionRegionProperty.class));
        regionProperties.addAll(EnumSet.allOf(LocationInfectionReductionGlobalAndRegionProperty.class));
        return regionProperties;
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        // First add overall start/stop triggers
        String triggerId = environment.getGlobalPropertyValue(LocationInfectionReductionGlobalProperty.LOCATION_INFECTION_REDUCTION_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, LocationInfectionReductionRegionProperty.LOCATION_INFECTION_REDUCTION_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(LocationInfectionReductionGlobalProperty.LOCATION_INFECTION_REDUCTION_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, LocationInfectionReductionRegionProperty.LOCATION_INFECTION_REDUCTION_TRIGGER_END);
        // Then add trigger property overrides
        List<TriggeredPropertyOverride> triggeredPropertyOverrides = environment.getGlobalPropertyValue(
                LocationInfectionReductionGlobalProperty.LOCATION_INFECTION_REDUCTION_TRIGGER_OVERRIDES);
        for (TriggeredPropertyOverride override : triggeredPropertyOverrides) {
            triggerId = override.trigger();
            // This should only be called for LOCATION_INFECTION_REDUCTION
            String parameterId = override.property();
            if (!parameterId.equals(LocationInfectionReductionGlobalAndRegionProperty.LOCATION_INFECTION_REDUCTION.toString())) {
                throw new RuntimeException("Unrecognized property for triggered parameter override: " + parameterId);
            }
            DefinedGlobalAndRegionProperty property = LocationInfectionReductionGlobalAndRegionProperty.LOCATION_INFECTION_REDUCTION;
            try {
                PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
                AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
                Object overrideValue = CoreEpiBootstrapUtil.getPropertyValueFromJson(override.value(), property, ageGroupPartition);
                TriggerUtils.addCallback(triggerCallbacks, triggerId,
                        (env, regionId) -> env.setRegionPropertyValue(regionId, property, overrideValue));
            } catch (IOException e) {
                throw new RuntimeException("Property override value cannot be parsed from: " + override.value());
            }
        }
        return triggerCallbacks;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        super.load(experimentBuilder);
    }

    private enum LocationInfectionReductionRandomId implements RandomNumberGeneratorId {
        ID
    }

    public enum LocationInfectionReductionGlobalProperty implements DefinedGlobalProperty {

        LOCATION_INFECTION_REDUCTION_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        LOCATION_INFECTION_REDUCTION_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        LOCATION_INFECTION_REDUCTION_TRIGGER_OVERRIDES(PropertyDefinition.builder()
                .setType(List.class).setDefaultValue(new ArrayList<>()).setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;
        private final boolean isExternal;

        LocationInfectionReductionGlobalProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = true;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return isExternal;
        }

    }

    public enum LocationInfectionReductionGlobalAndRegionProperty implements DefinedGlobalAndRegionProperty {

        LOCATION_INFECTION_REDUCTION(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(new HashMap<AgeGroup, Map<ContactGroupType, Double>>())
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build());

        private final PropertyDefinition propertyDefinition;

        LocationInfectionReductionGlobalAndRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

    }

    public enum LocationInfectionReductionRegionProperty implements DefinedRegionProperty {

        LOCATION_INFECTION_REDUCTION_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        LOCATION_INFECTION_REDUCTION_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build());

        private final PropertyDefinition propertyDefinition;

        LocationInfectionReductionRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

}
