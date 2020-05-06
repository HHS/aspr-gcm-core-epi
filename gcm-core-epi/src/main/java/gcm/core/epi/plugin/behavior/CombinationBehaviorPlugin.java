package gcm.core.epi.plugin.behavior;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.Util;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedGroupProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;

import java.util.*;

/*
    Combines Isolation & Hygiene, Workplace Telework, and School Closure Plugins with general distancing
 */
public class CombinationBehaviorPlugin extends BehaviorPlugin {

    private static List<BehaviorPlugin> getPlugins() {
        List<BehaviorPlugin> plugins = new ArrayList<>();
        plugins.add(new ShelterInPlaceBehaviorPlugin());
        plugins.add(new ContactTracingBehaviorPlugin());
        plugins.add(new RandomTestingBehaviorPlugin());
        plugins.add(new IsolationHygieneBehaviorPlugin());
        plugins.add(new TeleworkBehaviorPlugin());
        plugins.add(new SchoolClosureBehaviorPlugin());
        plugins.add(new LocationInfectionReductionPlugin());
        return plugins;
    }

    @Override
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId, ContactGroupType selectedContactGroupType) {

        // First see if shelter in place applies
        Optional<ContactGroupType> substitutedGroupType = new ShelterInPlaceBehaviorPlugin()
                .getSubstitutedContactGroup(environment, personId, selectedContactGroupType);
        // Then see if contact tracing applies
        substitutedGroupType = substitutedGroupType.flatMap(contactGroupType ->
                new ContactTracingBehaviorPlugin().getSubstitutedContactGroup(environment, personId, contactGroupType));
        // Then see if random testing applies
        substitutedGroupType = substitutedGroupType.flatMap(contactGroupType ->
                new RandomTestingBehaviorPlugin().getSubstitutedContactGroup(environment, personId, contactGroupType));
        // Then see if isolation applies
        substitutedGroupType = substitutedGroupType.flatMap(contactGroupType ->
                new IsolationHygieneBehaviorPlugin().getSubstitutedContactGroup(environment, personId, contactGroupType));
        // Then school closure
        substitutedGroupType = substitutedGroupType.flatMap(contactGroupType ->
                new SchoolClosureBehaviorPlugin().getSubstitutedContactGroup(environment, personId, contactGroupType));
        // Then workplace telework
        substitutedGroupType = substitutedGroupType.flatMap(contactGroupType ->
                new TeleworkBehaviorPlugin().getSubstitutedContactGroup(environment, personId, contactGroupType));

        return substitutedGroupType;

    }

    @Override
    public double getInfectionProbability(Environment environment, ContactGroupType contactSetting, PersonId personId) {
        // Sub-plugin effects
        double contactTracingInfectionProbability =
                new ContactTracingBehaviorPlugin().getInfectionProbability(environment, contactSetting, personId);
        double isolationHygieneInfectionProbability =
                new IsolationHygieneBehaviorPlugin().getInfectionProbability(environment, contactSetting, personId);
        double workplaceTeleworkInfectionProbability =
                new TeleworkBehaviorPlugin().getInfectionProbability(environment, contactSetting, personId);
        double generalInfectionProbability =
                new LocationInfectionReductionPlugin().getInfectionProbability(environment, contactSetting, personId);
        double randomTestingInfectionProbability =
                new RandomTestingBehaviorPlugin().getInfectionProbability(environment, contactSetting, personId);
        double subPluginProbability = contactTracingInfectionProbability * isolationHygieneInfectionProbability *
                workplaceTeleworkInfectionProbability * generalInfectionProbability *
                randomTestingInfectionProbability;

        // Transmission rate reduction
        RegionId regionId = environment.getPersonRegion(personId);

        boolean transmissionRateReductionIsInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                CombinationBehaviorRegionProperty.TRANSMISSION_RATE_REDUCTION_TRIGGER_START,
                CombinationBehaviorRegionProperty.TRANSMISSION_RATE_REDUCTION_TRIGGER_END);
        double transmissionRateReduction = 0.0;
        if (transmissionRateReductionIsInEffect) {
            transmissionRateReduction = environment.getGlobalPropertyValue(
                    CombinationBehaviorGlobalProperty.TRANSMISSION_RATE_REDUCTION);
        }

        // Infection rate reduction
        boolean infectionRateReductionIsInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                CombinationBehaviorRegionProperty.INFECTION_RATE_REDUCTION_TRIGGER_START,
                CombinationBehaviorRegionProperty.INFECTION_RATE_REDUCTION_TRIGGER_END);

        double infectionRateReduction = 0.0;
        if (infectionRateReductionIsInEffect) {
            AgeGroup ageGroup = Util.getAgeGroupForPerson(environment, personId);
            Map<AgeGroup, Double> infectionRateReductionByAge = environment.getGlobalPropertyValue(
                    CombinationBehaviorGlobalProperty.INFECTION_RATE_REDUCTION);
            infectionRateReduction = infectionRateReductionByAge.getOrDefault(ageGroup, 0.0);
        }

        return subPluginProbability * (1 - transmissionRateReduction) * (1 - infectionRateReduction);
    }

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        Set<DefinedGlobalProperty> result = new HashSet<>();
        getPlugins().forEach(plugin -> result.addAll(plugin.getGlobalProperties()));
        result.addAll(EnumSet.allOf(CombinationBehaviorGlobalProperty.class));
        return result;
    }

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        Set<DefinedPersonProperty> result = new HashSet<>();
        getPlugins().forEach(plugin -> result.addAll(plugin.getPersonProperties()));
        return result;
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> result = new ArrayList<>();
        getPlugins().forEach(plugin -> result.addAll(plugin.getRandomIds()));
        return result;
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        Set<DefinedRegionProperty> result = new HashSet<>();
        getPlugins().forEach(plugin -> result.addAll(plugin.getRegionProperties()));
        result.addAll(EnumSet.allOf(CombinationBehaviorRegionProperty.class));
        return result;
    }

    @Override
    public Map<ContactGroupType, Set<DefinedGroupProperty>> getGroupProperties() {
        Map<ContactGroupType, Set<DefinedGroupProperty>> result = new EnumMap<>(ContactGroupType.class);
        getPlugins().forEach(plugin -> plugin.getGroupProperties().forEach(
                (contactGroupType, definedGroupProperties) -> {
                    Set<DefinedGroupProperty> accumulatedGroupProperties = result.computeIfAbsent(contactGroupType,
                            key -> new HashSet<>());
                    accumulatedGroupProperties.addAll(definedGroupProperties);
                }
        ));
        return result;
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        // Delegate plugin callbacks
        getPlugins().forEach(plugin -> TriggerUtils.mergeCallbacks(triggerCallbacks, plugin.getTriggerCallbacks(environment)));
        // Transmission rate reduction
        String triggerId = environment.getGlobalPropertyValue(CombinationBehaviorGlobalProperty.TRANSMISSION_RATE_REDUCTION_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, CombinationBehaviorRegionProperty.TRANSMISSION_RATE_REDUCTION_TRIGGER_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, CombinationBehaviorRegionProperty.TRANSMISSION_RATE_REDUCTION_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(CombinationBehaviorGlobalProperty.TRANSMISSION_RATE_REDUCTION_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, CombinationBehaviorRegionProperty.TRANSMISSION_RATE_REDUCTION_TRIGGER_END);
        // Infection rate reduction
        triggerId = environment.getGlobalPropertyValue(CombinationBehaviorGlobalProperty.INFECTION_RATE_REDUCTION_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, CombinationBehaviorRegionProperty.INFECTION_RATE_REDUCTION_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(CombinationBehaviorGlobalProperty.INFECTION_RATE_REDUCTION_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, CombinationBehaviorRegionProperty.INFECTION_RATE_REDUCTION_TRIGGER_END);
        return triggerCallbacks;
    }

    @Override
    public void handleSuspectedInfected(Environment environment, PersonId personId) {
        new IsolationHygieneBehaviorPlugin().handleSuspectedInfected(environment, personId);
    }

    @Override
    public void handleHomeInfection(Environment environment, PersonId personId) {
        new IsolationHygieneBehaviorPlugin().handleHomeInfection(environment, personId);
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        super.load(experimentBuilder);

        experimentBuilder.addGlobalComponentId(TeleworkBehaviorPlugin.WORKPLACE_TELEWORK_MANAGER_ID,
                TeleworkBehaviorPlugin.TeleworkManager.class);
        experimentBuilder.addGlobalComponentId(IsolationHygieneBehaviorPlugin.INFECTION_AWARENESS_MANAGER_ID,
                IsolationHygieneBehaviorPlugin.InfectionAwarenessManager.class);
        experimentBuilder.addGlobalComponentId(ContactTracingBehaviorPlugin.CONTACT_TRACING_MANAGER_ID,
                ContactTracingBehaviorPlugin.ContactTracingManager.class);
        experimentBuilder.addGlobalComponentId(RandomTestingBehaviorPlugin.RANDOM_TESTING_MANAGER_ID,
                RandomTestingBehaviorPlugin.RandomTestingManager.class);
    }

    public enum CombinationBehaviorGlobalProperty implements DefinedGlobalProperty {

        TRANSMISSION_RATE_REDUCTION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        TRANSMISSION_RATE_REDUCTION_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        TRANSMISSION_RATE_REDUCTION_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        INFECTION_RATE_REDUCTION(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

        INFECTION_RATE_REDUCTION_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        INFECTION_RATE_REDUCTION_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;

        CombinationBehaviorGlobalProperty(PropertyDefinition propertyDefinition) {
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

    public enum CombinationBehaviorRegionProperty implements DefinedRegionProperty {

        TRANSMISSION_RATE_REDUCTION_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),

        TRANSMISSION_RATE_REDUCTION_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),

        INFECTION_RATE_REDUCTION_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),

        INFECTION_RATE_REDUCTION_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build());

        private final PropertyDefinition propertyDefinition;

        CombinationBehaviorRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

}
