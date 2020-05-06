package gcm.core.epi.plugin.behavior;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class SchoolClosureBehaviorPlugin extends BehaviorPlugin {

    @Override
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId, ContactGroupType selectedContactGroupType) {

        if (selectedContactGroupType == ContactGroupType.SCHOOL) {
            RegionId regionId = environment.getPersonRegion(personId);

            // Summer closure
            boolean summerInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                    SchoolClosureRegionProperty.SUMMER_TRIGGER_START,
                    SchoolClosureRegionProperty.SUMMER_TRIGGER_END);

            // Emergency school closure
            boolean schoolClosureInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                    SchoolClosureRegionProperty.SCHOOL_CLOSURE_TRIGGER_START,
                    SchoolClosureRegionProperty.SCHOOL_CLOSURE_TRIGGER_END);

            if (summerInEffect) {
                // Summer
                Map<ContactGroupType, Double> schoolClosedContactSubstitutionWeights = environment.getGlobalPropertyValue(
                        SchoolClosureGlobalProperty.SUMMER_CONTACT_SUBSTITUTION_WEIGHTS);
                List<Pair<ContactGroupType, Double>> schoolClosedContactSubstitutionWeightsList = schoolClosedContactSubstitutionWeights
                        .entrySet()
                        .stream()
                        .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());

                EnumeratedDistribution<ContactGroupType> contactGroupDistribution =
                        new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(TimedSchoolClosureRandomId.ID),
                                schoolClosedContactSubstitutionWeightsList);

                return Optional.of(contactGroupDistribution.sample());
            } else if (schoolClosureInEffect) {
                // School closure outside of summer
                Map<ContactGroupType, Double> schoolClosedContactSubstitutionWeights = environment.getGlobalPropertyValue(
                        SchoolClosureGlobalProperty.SCHOOL_CLOSED_CONTACT_SUBSTITUTION_WEIGHTS);
                List<Pair<ContactGroupType, Double>> schoolClosedContactSubstitutionWeightsList = schoolClosedContactSubstitutionWeights
                        .entrySet()
                        .stream()
                        .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());

                EnumeratedDistribution<ContactGroupType> contactGroupDistribution =
                        new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(TimedSchoolClosureRandomId.ID),
                                schoolClosedContactSubstitutionWeightsList);

                return Optional.of(contactGroupDistribution.sample());
            } else {
                // Leave it alone
                return Optional.of(selectedContactGroupType);
            }
        }

        // Otherwise leave contacts alone
        return Optional.of(selectedContactGroupType);
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(TimedSchoolClosureRandomId.ID);
        return randomIds;
    }

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(SchoolClosureGlobalProperty.class));
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        return new HashSet<>(EnumSet.allOf(SchoolClosureRegionProperty.class));
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        // School closure
        String triggerId = environment.getGlobalPropertyValue(SchoolClosureGlobalProperty.SCHOOL_CLOSURE_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, SchoolClosureRegionProperty.SCHOOL_CLOSURE_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(SchoolClosureGlobalProperty.SCHOOL_CLOSURE_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, SchoolClosureRegionProperty.SCHOOL_CLOSURE_TRIGGER_END);
        // Summer
        triggerId = environment.getGlobalPropertyValue(SchoolClosureGlobalProperty.SUMMER_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, SchoolClosureRegionProperty.SUMMER_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(SchoolClosureGlobalProperty.SUMMER_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, SchoolClosureRegionProperty.SUMMER_TRIGGER_END);
        return triggerCallbacks;
    }

    private enum TimedSchoolClosureRandomId implements RandomNumberGeneratorId {
        ID
    }

    public enum SchoolClosureGlobalProperty implements DefinedGlobalProperty {

        SCHOOL_CLOSURE_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SCHOOL_CLOSURE_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SUMMER_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SUMMER_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SCHOOL_CLOSED_CONTACT_SUBSTITUTION_WEIGHTS(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(getSchoolClosedSubstitutionWeights())
                .setPropertyValueMutability(false).build()),

        SUMMER_CONTACT_SUBSTITUTION_WEIGHTS(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(getSummerSubstitutionWeights())
                .setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;

        SchoolClosureGlobalProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        private static Map<ContactGroupType, Double> getSchoolClosedSubstitutionWeights() {
            Map<ContactGroupType, Double> weights = new EnumMap<>(ContactGroupType.class);
            weights.put(ContactGroupType.HOME, 0.90);
            weights.put(ContactGroupType.SCHOOL, 0.05);
            weights.put(ContactGroupType.GLOBAL, 0.05);
            return weights;
        }

        private static Map<ContactGroupType, Double> getSummerSubstitutionWeights() {
            Map<ContactGroupType, Double> weights = new EnumMap<>(ContactGroupType.class);
            weights.put(ContactGroupType.HOME, 0.50);
            weights.put(ContactGroupType.SCHOOL, 0.45);
            weights.put(ContactGroupType.GLOBAL, 0.05);
            return weights;
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

    public enum SchoolClosureRegionProperty implements DefinedRegionProperty {

        SCHOOL_CLOSURE_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        SCHOOL_CLOSURE_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        SUMMER_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        SUMMER_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build());

        private final PropertyDefinition propertyDefinition;

        SchoolClosureRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

}
