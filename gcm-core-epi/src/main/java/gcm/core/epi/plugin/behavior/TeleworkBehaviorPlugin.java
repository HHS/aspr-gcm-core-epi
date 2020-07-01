package gcm.core.epi.plugin.behavior;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.*;
import gcm.scenario.*;
import gcm.simulation.Environment;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class TeleworkBehaviorPlugin extends BehaviorPlugin {

    static final GlobalComponentId WORKPLACE_TELEWORK_MANAGER_ID = new GlobalComponentId() {
        @Override
        public String toString() {
            return "WORKPLACE_TELEWORK_MANAGER_ID";
        }
    };

    private static boolean isPersonTeleworkAble(Environment environment, PersonId personId) {
        RegionId regionId = environment.getPersonRegion(personId);
        boolean teleworkIsInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                TeleworkRegionProperty.TELEWORK_TRIGGER_START,
                TeleworkRegionProperty.TELEWORK_TRIGGER_END);
        if (teleworkIsInEffect) {
            float personTeleworkPropensity = environment.getPersonPropertyValue(personId,
                    TeleworkPersonProperty.TELEWORK_PROPENSITY);
            // We know this person has a workplace
            GroupId workplaceId = environment.getGroupsForGroupTypeAndPerson(ContactGroupType.WORK, personId).get(0);
            float workplaceTeleworkPropensity = environment.getGroupPropertyValue(workplaceId,
                    TeleworkWorkProperty.TELEWORK_PROPENSITY);
            double fractionOfWorkplacesTeleworking = getRegionalPropertyValue(environment, regionId,
                    TeleworkGlobalAndRegionProperty.FRACTION_OF_WORKPLACES_WITH_TELEWORK_EMPLOYEES);
            double fractionOfEmployeesTeleworking = getRegionalPropertyValue(environment, regionId,
                    TeleworkGlobalAndRegionProperty.FRACTION_OF_EMPLOYEES_WHO_TELEWORK_WHEN_ABLE);
            // Telework only if workplace and person are teleworking
            return (fractionOfWorkplacesTeleworking > 1 - workplaceTeleworkPropensity &&
                    fractionOfEmployeesTeleworking > 1 - personTeleworkPropensity);
        }
        return false;
    }

    @Override
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId, ContactGroupType selectedContactGroupType) {
        // Can only affect workplace transmission
        if (selectedContactGroupType == ContactGroupType.WORK) {
            if (isPersonTeleworkAble(environment, personId)) {
                RegionId regionId = environment.getPersonRegion(personId);
                double teleworkTimeFraction = getRegionalPropertyValue(environment, regionId,
                        TeleworkGlobalAndRegionProperty.TELEWORK_TIME_FRACTION);
                if (environment.getRandomGeneratorFromId(TeleworkRandomId.ID).nextDouble() <
                        teleworkTimeFraction) {
                    // Substitute workplace contacts
                    Map<ContactGroupType, Double> teleworkContactSubstitutionWeights = getRegionalPropertyValue(environment, regionId,
                            TeleworkGlobalAndRegionProperty.WORKPLACE_TELEWORK_CONTACT_SUBSTITUTION_WEIGHTS);
                    List<Pair<ContactGroupType, Double>> teleworkContactSubstitutionWeightsList = teleworkContactSubstitutionWeights
                            .entrySet()
                            .stream()
                            .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList());
                    EnumeratedDistribution<ContactGroupType> contactGroupDistribution =
                            new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(TeleworkRandomId.ID),
                                    teleworkContactSubstitutionWeightsList);
                    return Optional.of(contactGroupDistribution.sample());
                }
            }
        }
        // All other cases leave contact alone
        return Optional.of(selectedContactGroupType);
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(TeleworkRandomId.ID);
        return randomIds;
    }

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>(EnumSet.allOf(TeleworkPersonProperty.class));
    }

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        Set<DefinedGlobalProperty> globalProperties = new HashSet<>();
        globalProperties.addAll(EnumSet.allOf(TeleworkGlobalProperty.class));
        globalProperties.addAll(EnumSet.allOf(TeleworkGlobalAndRegionProperty.class));
        return globalProperties;
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        Set<DefinedRegionProperty> regionProperties = new HashSet<>();
        regionProperties.addAll(EnumSet.allOf(TeleworkRegionProperty.class));
        regionProperties.addAll(EnumSet.allOf(TeleworkGlobalAndRegionProperty.class));
        return regionProperties;
    }

    @Override
    public Map<ContactGroupType, Set<DefinedGroupProperty>> getGroupProperties() {
        Map<ContactGroupType, Set<DefinedGroupProperty>> groupProperties = new EnumMap<>(ContactGroupType.class);
        groupProperties.put(ContactGroupType.WORK, new HashSet<>(EnumSet.allOf(TeleworkWorkProperty.class)));
        return groupProperties;
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        // First add overall start/stop triggers
        String triggerId = environment.getGlobalPropertyValue(TeleworkGlobalProperty.TELEWORK_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, TeleworkRegionProperty.TELEWORK_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(TeleworkGlobalProperty.TELEWORK_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, TeleworkRegionProperty.TELEWORK_TRIGGER_END);
        // Then add trigger property overrides
        List<TriggeredPropertyOverride> triggeredPropertyOverrides = environment.getGlobalPropertyValue(
                TeleworkGlobalProperty.TELEWORK_TRIGGER_OVERRIDES);
        addTriggerOverrideCallbacks(triggerCallbacks, triggeredPropertyOverrides,
                Arrays.stream(TeleworkGlobalAndRegionProperty.values()).collect(Collectors.toSet()), environment);
        return triggerCallbacks;
    }

    @Override
    public double getInfectionProbability(Environment environment, ContactGroupType contactSetting, PersonId personId) {
        // Can only affect workplace transmission
        if (contactSetting == ContactGroupType.WORK) {
            if (isPersonTeleworkAble(environment, personId)) {
                RegionId regionId = environment.getPersonRegion(personId);
                double teleworkTimeFraction = getRegionalPropertyValue(environment, regionId,
                        TeleworkGlobalAndRegionProperty.TELEWORK_TIME_FRACTION);
                return 1.0 - teleworkTimeFraction;
            }
        }
        // Otherwise do not change infection probability
        return 1.0;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        super.load(experimentBuilder);
        experimentBuilder.addGlobalComponentId(WORKPLACE_TELEWORK_MANAGER_ID, TeleworkManager.class);
    }

    private enum TeleworkRandomId implements RandomNumberGeneratorId {
        ID
    }

    public enum TeleworkPersonProperty implements DefinedPersonProperty {

        TELEWORK_PROPENSITY(PropertyDefinition.builder()
                .setType(Float.class).setDefaultValue(0.0f).build());

        private final PropertyDefinition propertyDefinition;

        TeleworkPersonProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public enum TeleworkWorkProperty implements DefinedGroupProperty {

        TELEWORK_PROPENSITY(PropertyDefinition.builder()
                .setType(Float.class).setDefaultValue(0.0f).build());

        private final PropertyDefinition propertyDefinition;

        TeleworkWorkProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public enum TeleworkGlobalProperty implements DefinedGlobalProperty {

        TELEWORK_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        TELEWORK_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        TELEWORK_TRIGGER_OVERRIDES(PropertyDefinition.builder()
                .setType(List.class).setDefaultValue(new ArrayList<TriggeredPropertyOverride>())
                .setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;

        TeleworkGlobalProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        private static Map<ContactGroupType, Double> getDefaultContactSubstitutionWeights() {
            Map<ContactGroupType, Double> weights = new EnumMap<>(ContactGroupType.class);
            weights.put(ContactGroupType.HOME, 0.95);
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

    public enum TeleworkGlobalAndRegionProperty implements DefinedGlobalAndRegionProperty {

        FRACTION_OF_WORKPLACES_WITH_TELEWORK_EMPLOYEES(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).build()),

        FRACTION_OF_EMPLOYEES_WHO_TELEWORK_WHEN_ABLE(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).build()),

        TELEWORK_TIME_FRACTION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).build()),

        WORKPLACE_TELEWORK_CONTACT_SUBSTITUTION_WEIGHTS(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(getDefaultContactSubstitutionWeights())
                .setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;

        TeleworkGlobalAndRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        private static Map<ContactGroupType, Double> getDefaultContactSubstitutionWeights() {
            Map<ContactGroupType, Double> weights = new EnumMap<>(ContactGroupType.class);
            weights.put(ContactGroupType.HOME, 0.95);
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

    public enum TeleworkRegionProperty implements DefinedRegionProperty {

        TELEWORK_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        TELEWORK_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build());

        private final PropertyDefinition propertyDefinition;

        TeleworkRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public static class TeleworkManager extends AbstractComponent {

        @Override
        public void init(Environment environment) {
            // Determine at workplace creation if the workplace will support telework
            environment.observeGroupConstructionByType(true, ContactGroupType.WORK);
        }

        @Override
        public void observeGroupConstruction(Environment environment, GroupId groupId) {
            // Only called for workplaces upon construction
            RandomGenerator teleworkRandomGenerator = environment.getRandomGeneratorFromId(TeleworkRandomId.ID);
            // Assign propensity for workplace
            environment.setGroupPropertyValue(groupId, TeleworkWorkProperty.TELEWORK_PROPENSITY,
                    teleworkRandomGenerator.nextFloat());
            // Assign propensities for workers
            List<PersonId> workerIds = environment.getPeopleForGroup(groupId);
            for (PersonId workerId : workerIds) {
                environment.setPersonPropertyValue(workerId, TeleworkPersonProperty.TELEWORK_PROPENSITY,
                        teleworkRandomGenerator.nextFloat());
            }
        }
    }
}
