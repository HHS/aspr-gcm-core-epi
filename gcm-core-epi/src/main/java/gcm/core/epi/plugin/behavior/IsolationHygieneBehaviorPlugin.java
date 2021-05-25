package gcm.core.epi.plugin.behavior;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import plugins.gcm.agents.Plan;
import plugins.gcm.agents.AbstractComponent;
import plugins.gcm.agents.Environment;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.globals.support.GlobalComponentId;
import plugins.groups.support.GroupId;
import plugins.people.support.PersonId;
import plugins.personproperties.support.PersonPropertyId;
import plugins.regions.support.RegionId;
import plugins.regions.support.RegionPropertyId;
import plugins.stochastics.support.RandomNumberGeneratorId;

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

        // TODO: Allow people to eventually stop washing their hands. (Add event to turn off IS_USING_HAND_HYGIENE)
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
        experimentBuilder.addGlobalComponentId(INFECTION_AWARENESS_MANAGER_ID, () -> new InfectionAwarenessManager()::init);
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

        IS_USING_HAND_HYGIENE(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false).build());

        final TypedPropertyDefinition propertyDefinition;

        IsolationHygienePersonProperties(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public enum IsolationHygieneGlobalProperty implements DefinedGlobalProperty {

        HAND_HYGIENE_EFFICACY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        FRACTION_STAY_HOME_WHEN_SUSPECT_INFECTION(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        FRACTION_USING_HAND_HYGIENE(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        ISOLATION_HYGIENE_START(TypedPropertyDefinition.builder()
                .type(String.class).defaultValue("").isMutable(false).build()),

        ISOLATION_HYGIENE_END(TypedPropertyDefinition.builder()
                .type(String.class).defaultValue("").isMutable(false).build()),

        ISOLATION_HYGIENE_DELAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        ISOLATION_HYGIENE_DURATION(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build());

        private final TypedPropertyDefinition propertyDefinition;
        private final boolean isExternal;

        IsolationHygieneGlobalProperty(TypedPropertyDefinition propertyDefinition, boolean isExternal) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = isExternal;
        }

        IsolationHygieneGlobalProperty(TypedPropertyDefinition propertyDefinition) {
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

    public enum IsolationHygieneRegionProperty implements DefinedRegionProperty {

        ISOLATION_HYGIENE_TRIGGER_START(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false).build()),

        ISOLATION_HYGIENE_TRIGGER_END(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false).build()),

        ISOLATION_HYGIENE_IN_EFFECT(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false).build());

        private final TypedPropertyDefinition propertyDefinition;

        IsolationHygieneRegionProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
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
            //noinspection OptionalGetWithoutIsPresent - We know that the behavior plugin should have been loaded
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
            //noinspection OptionalGetWithoutIsPresent - We know that the behavior plugin should have been loaded
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
            //noinspection OptionalGetWithoutIsPresent - We know that the behavior plugin should have been loaded
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

        private EndIsolationPlan(final PersonId personId) {
            this.personId = personId;
        }
    }


}
