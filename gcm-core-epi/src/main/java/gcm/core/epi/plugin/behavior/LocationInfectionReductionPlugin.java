package gcm.core.epi.plugin.behavior;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.Util;
import gcm.core.epi.propertytypes.FipsCodeValue;
import gcm.core.epi.propertytypes.ImmutableFipsCodeValue;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalAndRegionProperty;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.scenario.*;
import gcm.simulation.Environment;

import java.util.*;
import java.util.stream.Collectors;

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
            Map<AgeGroup, Map<ContactGroupType, Double>> infectionReductionByAgeAndLocationMap =
                    getRegionalPropertyValue(environment, regionId,
                            LocationInfectionReductionGlobalAndRegionProperty.LOCATION_INFECTION_REDUCTION);
            Map<ContactGroupType, Double> infectionReductionByLocationMap = infectionReductionByAgeAndLocationMap
                    .getOrDefault(ageGroup, new HashMap<>());
            infectionReduction = infectionReductionByLocationMap.getOrDefault(contactSetting, 0.0);
        }

        return 1.0 - infectionReduction;
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        HashSet<DefinedRegionProperty> regionProperties = new HashSet<>(EnumSet.allOf(LocationInfectionReductionRegionProperty.class));
        for (DefinedGlobalAndRegionProperty property : LocationInfectionReductionGlobalAndRegionProperty.values()) {
            regionProperties.add(property.getRegionProperty());
        }
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
        addTriggerOverrideCallbacks(triggerCallbacks, triggeredPropertyOverrides,
                Arrays.stream(LocationInfectionReductionGlobalAndRegionProperty.values()).collect(Collectors.toCollection(LinkedHashSet::new)),
                environment);
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

        LOCATION_INFECTION_REDUCTION_START(TypedPropertyDefinition.builder()
                .type(String.class).defaultValue("").isMutable(false).build()),

        LOCATION_INFECTION_REDUCTION_END(TypedPropertyDefinition.builder()
                .type(String.class).defaultValue("").isMutable(false).build()),

        LOCATION_INFECTION_REDUCTION_TRIGGER_OVERRIDES(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<List<TriggeredPropertyOverride>>() {
                })
                .defaultValue(new ArrayList<TriggeredPropertyOverride>())
                .isMutable(false).build());

        private final TypedPropertyDefinition propertyDefinition;
        private final boolean isExternal;

        LocationInfectionReductionGlobalProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = true;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return isExternal;
        }

    }

    public enum LocationInfectionReductionGlobalAndRegionProperty implements DefinedGlobalAndRegionProperty {

        LOCATION_INFECTION_REDUCTION(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<FipsCodeValue<Map<AgeGroup, Map<ContactGroupType, Double>>>>() {
                })
                .defaultValue(ImmutableFipsCodeValue.builder()
                        .defaultValue(new HashMap<AgeGroup, Map<ContactGroupType, Double>>()).build())
                .timeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build(),
                LocationInfectionReductionRegionProperty.LOCATION_INFECTION_REDUCTION);

        private final TypedPropertyDefinition propertyDefinition;
        private final DefinedRegionProperty regionProperty;

        LocationInfectionReductionGlobalAndRegionProperty(TypedPropertyDefinition propertyDefinition, DefinedRegionProperty regionProperty) {
            this.propertyDefinition = propertyDefinition;
            this.regionProperty = regionProperty;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

        @Override
        public DefinedRegionProperty getRegionProperty() {
            return regionProperty;
        }
    }

    public enum LocationInfectionReductionRegionProperty implements DefinedRegionProperty {

        LOCATION_INFECTION_REDUCTION(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<AgeGroup, Map<ContactGroupType, Double>>>() {
                })
                .type(Map.class).defaultValue(new HashMap<AgeGroup, Map<ContactGroupType, Double>>())
                .timeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        LOCATION_INFECTION_REDUCTION_TRIGGER_START(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false)
                .timeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        LOCATION_INFECTION_REDUCTION_TRIGGER_END(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false)
                .timeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build());

        private final TypedPropertyDefinition propertyDefinition;

        LocationInfectionReductionRegionProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

}
