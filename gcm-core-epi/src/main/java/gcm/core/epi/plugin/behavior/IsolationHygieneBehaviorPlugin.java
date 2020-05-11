package gcm.core.epi.plugin.behavior;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

import java.util.*;

public class IsolationHygieneBehaviorPlugin extends BehaviorPlugin {

    static final GlobalComponentId INFECTION_AWARENESS_MANAGER_ID = new GlobalComponentId() {
        @Override
        public String toString() {
            return "INFECTION_AWARENESS_MANAGER_ID";
        }
    };

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(IsolationHygieneGlobalProperty.class));
    }

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>(EnumSet.allOf(IsolationHygienePersonProperties.class));
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(IsolationHygieneRandomId.ID);
        return randomIds;
    }

    @Override
    public void handleSuspectedInfected(Environment environment, PersonId personId) {
        double fractionStayHomeWhenSuspectInfection = environment.getGlobalPropertyValue(
                IsolationHygieneGlobalProperty.FRACTION_STAY_HOME_WHEN_SUSPECT_INFECTION);
        if (fractionStayHomeWhenSuspectInfection > 0 &&
                environment.getRandomGeneratorFromId(IsolationHygieneRandomId.ID).nextDouble() < fractionStayHomeWhenSuspectInfection) {
            environment.setPersonPropertyValue(personId, PersonProperty.IS_STAYING_HOME, true);
            double durationOfIsolation = environment.getGlobalPropertyValue(IsolationHygieneGlobalProperty.ISOLATION_HYGIENE_DURATION);
            double time = environment.getTime();
            environment.addPlan(new EndIsolationPlan(personId), time + durationOfIsolation);
        }
    }

    @Override
    public void handleHomeInfection(Environment environment, PersonId personId) {
        double fractionUsingHandHygiene = environment.getGlobalPropertyValue(
                IsolationHygieneGlobalProperty.FRACTION_USING_HAND_HYGIENE);

        if (fractionUsingHandHygiene > 0 &&
                environment.getRandomGeneratorFromId(IsolationHygieneRandomId.ID).nextDouble() < fractionUsingHandHygiene) {
            environment.setPersonPropertyValue(personId, IsolationHygienePersonProperties.IS_USING_HAND_HYGIENE, true);
        }
    }

    @Override
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId,
                                                                 ContactGroupType selectedContactGroupType) {
        // If a person is staying home, substitute any school or work infections contacts with home contacts
        final boolean isStayingHome = environment.getPersonPropertyValue(personId, PersonProperty.IS_STAYING_HOME);
        if (isStayingHome &&
                selectedContactGroupType != ContactGroupType.HOME &&
                selectedContactGroupType != ContactGroupType.GLOBAL) {
            return Optional.of(ContactGroupType.HOME);
        } else {
            return Optional.of(selectedContactGroupType);
        }
    }

    @Override
    public double getRelativeActivityLevel(Environment environment, PersonId personId) {
        // For now this is unchanged by this plugin
        return 1.0;
    }

    @Override
    public double getInfectionProbability(Environment environment, ContactGroupType contactSetting, PersonId personId) {
        RegionId regionId = environment.getPersonRegion(personId);
        boolean isolationHygieneInEffect = environment.getRegionPropertyValue(regionId,
                IsolationHygieneRegionProperty.ISOLATION_HYGIENE_IN_EFFECT);
        if (isolationHygieneInEffect) {
            final boolean isUsingHandHygiene = environment.getPersonPropertyValue(personId,
                    IsolationHygienePersonProperties.IS_USING_HAND_HYGIENE);
            final double handHygieneEfficacy = environment.getGlobalPropertyValue(
                    IsolationHygieneGlobalProperty.HAND_HYGIENE_EFFICACY);
            return isUsingHandHygiene ? 1.0 - handHygieneEfficacy : 1.0;
        } else {
            return 1.0;
        }
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        super.load(experimentBuilder);
        experimentBuilder.addGlobalComponentId(INFECTION_AWARENESS_MANAGER_ID, InfectionAwarenessManager.class);
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        return new HashSet<>(EnumSet.allOf(IsolationHygieneRegionProperty.class));
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        String triggerId = environment.getGlobalPropertyValue(IsolationHygieneGlobalProperty.ISOLATION_HYGIENE_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, IsolationHygieneRegionProperty.ISOLATION_HYGIENE_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(IsolationHygieneGlobalProperty.ISOLATION_HYGIENE_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, IsolationHygieneRegionProperty.ISOLATION_HYGIENE_TRIGGER_END);
        return triggerCallbacks;
    }

    private enum IsolationHygieneRandomId implements RandomNumberGeneratorId {
        ID
    }

    enum IsolationHygienePersonProperties implements DefinedPersonProperty {

        IS_USING_HAND_HYGIENE(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build());

        final PropertyDefinition propertyDefinition;

        IsolationHygienePersonProperties(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public enum IsolationHygieneGlobalProperty implements DefinedGlobalProperty {

        HAND_HYGIENE_EFFICACY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        FRACTION_STAY_HOME_WHEN_SUSPECT_INFECTION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        FRACTION_USING_HAND_HYGIENE(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        ISOLATION_HYGIENE_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        ISOLATION_HYGIENE_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        ISOLATION_HYGIENE_DELAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        ISOLATION_HYGIENE_DURATION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;
        private final boolean isExternal;

        IsolationHygieneGlobalProperty(PropertyDefinition propertyDefinition, boolean isExternal) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = isExternal;
        }

        IsolationHygieneGlobalProperty(PropertyDefinition propertyDefinition) {
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

    public enum IsolationHygieneRegionProperty implements DefinedRegionProperty {

        ISOLATION_HYGIENE_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),

        ISOLATION_HYGIENE_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),

        ISOLATION_HYGIENE_IN_EFFECT(PropertyDefinition.builder().
                setType(Boolean.class).setDefaultValue(false).build());

        private final PropertyDefinition propertyDefinition;

        IsolationHygieneRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public static class InfectionAwarenessManager extends AbstractComponent {

        @Override
        public void init(Environment environment) {
            environment.observeGlobalRegionPropertyChange(true, IsolationHygieneRegionProperty.ISOLATION_HYGIENE_TRIGGER_START);
            environment.observeGlobalRegionPropertyChange(true, IsolationHygieneRegionProperty.ISOLATION_HYGIENE_TRIGGER_END);
        }

        @Override
        public void observeRegionPropertyChange(Environment environment, RegionId regionId, RegionPropertyId regionPropertyId) {
            if (regionPropertyId == IsolationHygieneRegionProperty.ISOLATION_HYGIENE_TRIGGER_START) {
                environment.setRegionPropertyValue(regionId, IsolationHygieneRegionProperty.ISOLATION_HYGIENE_IN_EFFECT, true);
                environment.observeRegionPersonPropertyChange(true, regionId, PersonProperty.IS_SYMPTOMATIC);
            } else if (regionPropertyId == IsolationHygieneRegionProperty.ISOLATION_HYGIENE_TRIGGER_END) {
                boolean isInEffect = environment.getRegionPropertyValue(regionId, IsolationHygieneRegionProperty.ISOLATION_HYGIENE_IN_EFFECT);
                if (isInEffect) {
                    environment.setRegionPropertyValue(regionId, IsolationHygieneRegionProperty.ISOLATION_HYGIENE_IN_EFFECT, false);
                    environment.observeRegionPersonPropertyChange(false, regionId, PersonProperty.IS_SYMPTOMATIC);
                } else {
                    throw new RuntimeException("Isolation Hygiene Behavior Plugin trigger ended before it began");
                }
            } else {
                throw new RuntimeException("Isolation Hygiene Behavior Plugin encountered unexpected region property change");
            }
        }

        @Override
        public void observePersonPropertyChange(Environment environment, PersonId personId, PersonPropertyId personPropertyId) {
            Optional<BehaviorPlugin> behaviorPluginContainer = environment.getGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN);
            //noinspection OptionalGetWithoutIsPresent - We know that the behavior module should have been loaded
            BehaviorPlugin behaviorPlugin = behaviorPluginContainer.get();

            double delayToStartIsolationAndHygiene = environment.getGlobalPropertyValue(
                    IsolationHygieneGlobalProperty.ISOLATION_HYGIENE_DELAY);

            // Should only be called when a person becomes symptomatic
            if (personPropertyId.equals(PersonProperty.IS_SYMPTOMATIC)) {
                // Handle awareness for the infected and symptomatic person
                if (delayToStartIsolationAndHygiene == 0.0) {
                    behaviorPlugin.handleSuspectedInfected(environment, personId);
                    makePeopleInHomeAware(environment, personId);
                } else {
                    double currentTime = environment.getTime();
                    environment.addPlan(new SuspectedInfectionPlan(personId), currentTime + delayToStartIsolationAndHygiene);
                    environment.addPlan(new HomeInfectionPlan(personId), currentTime + delayToStartIsolationAndHygiene);
                }
            }
        }

        private void makePeopleInHomeAware(Environment environment, final PersonId personId) {
            Optional<BehaviorPlugin> behaviorPluginContainer = environment.getGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN);
            //noinspection OptionalGetWithoutIsPresent - We know that the behavior module should have been loaded
            BehaviorPlugin behaviorPlugin = behaviorPluginContainer.get();

            GroupId homeId = environment.getGroupsForGroupTypeAndPerson(ContactGroupType.HOME, personId).get(0);
            for (PersonId occupantId : environment.getPeopleForGroup(homeId)) {
                // Propagate awareness in the rest of the household
                behaviorPlugin.handleHomeInfection(environment, occupantId);
            }
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            Optional<BehaviorPlugin> behaviorPluginContainer = environment.getGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN);
            //noinspection OptionalGetWithoutIsPresent - We know that the behavior module should have been loaded
            BehaviorPlugin behaviorPlugin = behaviorPluginContainer.get();

            if (plan.getClass().equals(SuspectedInfectionPlan.class)) {
                behaviorPlugin.handleSuspectedInfected(environment, ((SuspectedInfectionPlan) plan).personId);
            } else if (plan.getClass().equals(HomeInfectionPlan.class)) {
                makePeopleInHomeAware(environment, ((HomeInfectionPlan) plan).personId);
            } else if (plan.getClass().equals(EndIsolationPlan.class)) {
                environment.setPersonPropertyValue(((EndIsolationPlan) plan).personId, PersonProperty.IS_STAYING_HOME, false);
            } else {
                throw new RuntimeException("IsolationHygieneBehaviorPlug attempting to execute an unknown plan type");
            }
        }
    }

    private static class HomeInfectionPlan implements Plan {
        private final PersonId personId;

        private HomeInfectionPlan(final PersonId personId) {
            this.personId = personId;
        }
    }

    private static class SuspectedInfectionPlan implements Plan {
        private final PersonId personId;

        private SuspectedInfectionPlan(final PersonId personId) {
            this.personId = personId;
        }
    }

    private static class EndIsolationPlan implements Plan {
        private final PersonId personId;

        private EndIsolationPlan(final PersonId personId) { this.personId = personId;}
    }


}
