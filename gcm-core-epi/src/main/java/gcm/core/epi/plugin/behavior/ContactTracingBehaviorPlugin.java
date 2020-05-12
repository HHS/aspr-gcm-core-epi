package gcm.core.epi.plugin.behavior;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.propertytypes.InfectionData;
import gcm.core.epi.trigger.*;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

import java.util.*;

public class ContactTracingBehaviorPlugin extends BehaviorPlugin {

    static final GlobalComponentId CONTACT_TRACING_MANAGER_ID = new GlobalComponentId() {
        @Override
        public String toString() {
            return "CONTACT_TRACING_MANAGER_ID";
        }
    };

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>(EnumSet.allOf(ContactTracingPersonProperty.class));
    }

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(ContactTracingGlobalProperty.class));
    }

    @Override
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId, ContactGroupType selectedContactGroupType) {
        // If a person is staying home, substitute any school or work infections contacts with home contacts
        boolean isStayingHome = environment.getPersonPropertyValue(personId, PersonProperty.IS_STAYING_HOME);
        if (isStayingHome &&
                selectedContactGroupType != ContactGroupType.HOME &&
                selectedContactGroupType != ContactGroupType.GLOBAL) {
            return Optional.of(ContactGroupType.HOME);
        } else {
            return Optional.of(selectedContactGroupType);
        }
    }

    @Override
    public double getInfectionProbability(Environment environment, ContactGroupType contactSetting, PersonId personId) {
        boolean isStayingHome = environment.getPersonPropertyValue(personId, PersonProperty.IS_STAYING_HOME);
        if (isStayingHome &&
                contactSetting != ContactGroupType.HOME &&
                contactSetting != ContactGroupType.GLOBAL) {
            return 0.0;
        } else {
            // Assume has effectively zero effect on reducing global transmission risk if this person is the target
            return 1.0;
        }
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(ContactTracingRandomId.ID);
        return randomIds;
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        return new HashSet<>(EnumSet.allOf(ContactTracingRegionProperty.class));
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        String triggerId = environment.getGlobalPropertyValue(ContactTracingGlobalProperty.CONTACT_TRACING_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, ContactTracingRegionProperty.CONTACT_TRACING_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(ContactTracingGlobalProperty.CONTACT_TRACING_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, ContactTracingRegionProperty.CONTACT_TRACING_TRIGGER_END);
        return triggerCallbacks;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        super.load(experimentBuilder);
        experimentBuilder.addGlobalComponentId(CONTACT_TRACING_MANAGER_ID, ContactTracingManager.class);
    }

    public enum ContactTracingRegionProperty implements DefinedRegionProperty {

        CONTACT_TRACING_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        CONTACT_TRACING_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build());

        private final PropertyDefinition propertyDefinition;

        ContactTracingRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public enum ContactTracingPersonProperty implements DefinedPersonProperty {

        // Will track infectious contacts in the GLOBAL setting
        GLOBAL_INFECTION_SOURCE_PERSON_ID(PropertyDefinition.builder()
                .setType(Integer.class).setDefaultValue(-1).setMapOption(MapOption.ARRAY).build());

        final PropertyDefinition propertyDefinition;

        ContactTracingPersonProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public enum ContactTracingGlobalProperty implements DefinedGlobalProperty {

        MAXIMUM_INFECTIONS_TO_TRACE(PropertyDefinition.builder()
                .setType(FipsCodeValues.class).setDefaultValue(ImmutableFipsCodeValues.builder().build())
                .setPropertyValueMutability(false).build()),

        CURRENT_INFECTIONS_BEING_TRACED(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(new HashMap<FipsCode,
                        ContactTracingManager.Counter>()).build(), false),

        FRACTION_INFECTIONS_TRACED(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).build()),

        FRACTION_CONTACTS_TRACED_AND_ISOLATED(PropertyDefinition.builder()
                .setType(Map.class)
                .setDefaultValue(new EnumMap<ContactGroupType, Double>(ContactGroupType.class))
                .setPropertyValueMutability(false).build()),

        TRACED_CONTACT_STAY_HOME_DURATION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        CONTACT_TRACING_ASCERTAINMENT_DELAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        CONTACT_TRACING_DELAY(PropertyDefinition.builder()
                .setType(Map.class)
                .setDefaultValue(new EnumMap<ContactGroupType, Double>(ContactGroupType.class))
                .setPropertyValueMutability(false).build()),

        CONTACT_TRACING_TIME(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        CONTACT_TRACING_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        CONTACT_TRACING_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build());

        final PropertyDefinition propertyDefinition;
        final boolean isExternal;

        ContactTracingGlobalProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = true;
        }

        ContactTracingGlobalProperty(PropertyDefinition propertyDefinition, boolean isExternal) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = isExternal;
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

    private enum ContactTracingRandomId implements RandomNumberGeneratorId {
        ID
    }

    public static class ContactTracingManager extends AbstractComponent {

        private final Map<FipsCode, Double> maximumInfectionsToTrace = new HashMap<>();
        private FipsScope scope;

        @Override
        public void init(Environment environment) {
            // Get maximum number of infections that can be traced
            FipsCodeValues maximumInfectionsToTraceFromProperty = environment.getGlobalPropertyValue(
                    ContactTracingGlobalProperty.MAXIMUM_INFECTIONS_TO_TRACE);
            maximumInfectionsToTrace.putAll(maximumInfectionsToTraceFromProperty.getFipsCodeValues(environment));
            scope = maximumInfectionsToTraceFromProperty.scope();
            Map<FipsCode, Counter> currentInfectionsBeingTraced = new HashMap<>();
            maximumInfectionsToTrace.keySet().forEach(
                    fipsCode -> currentInfectionsBeingTraced.put(fipsCode, new Counter())
            );
            environment.setGlobalPropertyValue(ContactTracingGlobalProperty.CURRENT_INFECTIONS_BEING_TRACED,
                    currentInfectionsBeingTraced);

            // Begin observing
            setObservationStatus(environment, true);
        }

        private void setObservationStatus(Environment environment, boolean observe) {
            // Observe people becoming symptomatic
            environment.observeGlobalPersonPropertyChange(observe, PersonProperty.IS_SYMPTOMATIC);

            // Observe all transmission events to mark global events
            environment.observeGlobalPropertyChange(observe, GlobalProperty.MOST_RECENT_INFECTION_DATA);
        }

        @Override
        public void observePersonPropertyChange(Environment environment, PersonId personId, PersonPropertyId personPropertyId) {
            // Only called when IS_SYMPTOMATIC is changed
            if (personPropertyId == PersonProperty.IS_SYMPTOMATIC) {
                boolean isSymptomatic = environment.getPersonPropertyValue(personId, PersonProperty.IS_SYMPTOMATIC);
                if (isSymptomatic) {
                    double caseAscertainmentDelay = environment.getGlobalPropertyValue(ContactTracingGlobalProperty.CONTACT_TRACING_ASCERTAINMENT_DELAY);

                    // Don't bother with plans if there's no need for delays
                    if (caseAscertainmentDelay == 0.0) {
                        ascertainSymptomaticCase(environment, personId);
                    } else {
                        double time = environment.getTime();
                        environment.addPlan(new ContactTracingAscertainmentPlan(personId), time + caseAscertainmentDelay);
                    }
                }
            } else {
                throw new RuntimeException("ContactTracingManager observed unexpected person property change");
            }
        }

        // A new symptomatic case was just identified. Determine if we need to do contact tracing for it.
        private void ascertainSymptomaticCase(Environment environment, final PersonId personId) {
            // Determine if we are currently tracing contacts
            RegionId regionId = environment.getPersonRegion(personId);
            boolean triggerIsInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                    ContactTracingRegionProperty.CONTACT_TRACING_TRIGGER_START,
                    ContactTracingRegionProperty.CONTACT_TRACING_TRIGGER_END);
            if (triggerIsInEffect) {
                FipsCode fipsCode = scope.getFipsCode(regionId);
                Map<FipsCode, Counter> currentInfectionsBeingTracedMap = environment.getGlobalPropertyValue(
                        ContactTracingGlobalProperty.CURRENT_INFECTIONS_BEING_TRACED);
                Counter currentInfectionsBeingTraced = currentInfectionsBeingTracedMap.get(fipsCode);
                Double maxInfectionsToTrace = maximumInfectionsToTrace.get(fipsCode);
                if (currentInfectionsBeingTraced.count < Math.round(maxInfectionsToTrace)) {
                    double fractionInfectionsTraced = environment.getGlobalPropertyValue(
                            ContactTracingGlobalProperty.FRACTION_INFECTIONS_TRACED);
                    if (environment.getRandomGeneratorFromId(ContactTracingRandomId.ID).nextDouble() <
                            fractionInfectionsTraced) {
                        // Person stays home
                        environment.setPersonPropertyValue(personId, PersonProperty.IS_STAYING_HOME, true);

                        // Increment counter of infections being traced
                        currentInfectionsBeingTraced.count++;

                        // Plan to trace and isolate contacts
                        Map<ContactGroupType, Double> fractionToTraceAndIsolateByGroup = environment.getGlobalPropertyValue(
                                ContactTracingGlobalProperty.FRACTION_CONTACTS_TRACED_AND_ISOLATED);
                        Map<ContactGroupType, Double> contactTracingDelayByGroup = environment.getGlobalPropertyValue(
                                ContactTracingGlobalProperty.CONTACT_TRACING_DELAY);
                        // First home, work, and school as applicable
                        List<ContactGroupType> contactGroupTypes = environment.getGroupTypesForPerson(personId);
                        // Add global
                        contactGroupTypes.add(ContactGroupType.GLOBAL);
                        for (ContactGroupType contactGroupType : contactGroupTypes) {
                            List<PersonId> peopleToTraceAndIsolate = new ArrayList<>();
                            List<PersonId> peopleInGroup;
                            // Handle home/work/school directly from groups
                            if (contactGroupType != ContactGroupType.GLOBAL) {
                                GroupId groupId = environment.getGroupsForGroupTypeAndPerson(contactGroupType,
                                        personId).get(0);
                                peopleInGroup = environment.getPeopleForGroup(groupId);
                            } else {
                                // Get global infections
                                peopleInGroup = environment.getPeopleWithPropertyValue(
                                        ContactTracingPersonProperty.GLOBAL_INFECTION_SOURCE_PERSON_ID, personId.getValue());
                            }
                            double fractionToTraceAndIsolate = fractionToTraceAndIsolateByGroup.getOrDefault(contactGroupType, 1.0);
                            for (PersonId personInGroup : peopleInGroup) {
                                if (!personInGroup.equals(personId) &&
                                        environment.getRandomGeneratorFromId(
                                                ContactTracingRandomId.ID).nextDouble() < fractionToTraceAndIsolate) {
                                    peopleToTraceAndIsolate.add(personInGroup);
                                }
                            }
                            double tracingDelay = contactTracingDelayByGroup.getOrDefault(contactGroupType, 0.0);
                            if (tracingDelay > 0) {
                                environment.addPlan(new ContactTracingIsolationPlan(peopleToTraceAndIsolate),
                                        environment.getTime() + tracingDelay);
                            } else {
                                traceAndIsolate(environment, peopleToTraceAndIsolate);
                            }
                        }
                        // Plan to return the resource of contact tracing
                        double contactTracingTime = environment.getGlobalPropertyValue(ContactTracingGlobalProperty.CONTACT_TRACING_TIME);
                        environment.addPlan(new ContractTracingCompletePlan(regionId), environment.getTime() + contactTracingTime);
                    }
                }
            }
        }

        private void traceAndIsolate(Environment environment, List<PersonId> peopleToTraceAndIsolate) {
            // Set person property to indicate people are staying home
            for (PersonId personId : peopleToTraceAndIsolate) {
                environment.setPersonPropertyValue(personId, PersonProperty.IS_STAYING_HOME, true);
            }

            // Plan to end isolation
            double stayAtHomeDuration = environment.getGlobalPropertyValue(ContactTracingGlobalProperty.TRACED_CONTACT_STAY_HOME_DURATION);
            environment.addPlan(new EndIsolationPlan(peopleToTraceAndIsolate), environment.getTime() + stayAtHomeDuration);
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            if (plan.getClass().equals(ContactTracingIsolationPlan.class)) {
                // Trace and isolate the people in the plan
                traceAndIsolate(environment, ((ContactTracingIsolationPlan) plan).peopleToTraceAndIsolate);
            } else if (plan.getClass().equals(EndIsolationPlan.class)) {
                List<PersonId> peopleToEndIsolation = ((EndIsolationPlan) plan).peopleToEndIsolation;
                for (PersonId personId : peopleToEndIsolation) {
                    environment.setPersonPropertyValue(personId,
                            PersonProperty.IS_STAYING_HOME, false);
                }
            } else if (plan.getClass().equals(ContractTracingCompletePlan.class)) {
                // Decrement counter
                FipsCode fipsCode = scope.getFipsCode(((ContractTracingCompletePlan) plan).regionId);
                Map<FipsCode, Counter> currentInfectionsBeingTracedMap = environment.getGlobalPropertyValue(
                        ContactTracingGlobalProperty.CURRENT_INFECTIONS_BEING_TRACED);
                Counter currentInfectionsBeingTraced = currentInfectionsBeingTracedMap.get(fipsCode);
                currentInfectionsBeingTraced.count--;
            } else if (plan.getClass().equals(ContactTracingAscertainmentPlan.class)) {
                // A new case was just identified and needs to be processed
                PersonId personId = ((ContactTracingAscertainmentPlan) plan).personId;

                ascertainSymptomaticCase(environment, personId);
            } else {
                throw new RuntimeException("ContactTracingPlugin attempting to execute an unknown plan type");
            }
        }

        @Override
        public void observeGlobalPropertyChange(Environment environment, GlobalPropertyId globalPropertyId) {
            // Only called when MOST_RECENT_INFECTION_DATA is changed
            if (globalPropertyId == GlobalProperty.MOST_RECENT_INFECTION_DATA) {
                Optional<InfectionData> mostRecentInfectionDataContainer = environment.getGlobalPropertyValue(
                        GlobalProperty.MOST_RECENT_INFECTION_DATA);
                //noinspection OptionalGetWithoutIsPresent
                InfectionData mostRecentInfectionData = mostRecentInfectionDataContainer.get();
                if (mostRecentInfectionData.transmissionOccurred() &&
                        mostRecentInfectionData.transmissionSetting() == ContactGroupType.GLOBAL &&
                        mostRecentInfectionData.sourcePersonId().isPresent()) {
                    Optional<PersonId> targetPersonId = mostRecentInfectionData.targetPersonId();
                    if (targetPersonId.isPresent()) {
                        PersonId sourcePersonId = mostRecentInfectionData.sourcePersonId().get();
                        environment.setPersonPropertyValue(targetPersonId.get(),
                                ContactTracingPersonProperty.GLOBAL_INFECTION_SOURCE_PERSON_ID,
                                sourcePersonId.getValue());
                    }
                }
            } else {
                throw new RuntimeException("ContactTracingManager observed unexpected global property change");
            }
        }


        private static class ContactTracingIsolationPlan implements Plan {

            private final List<PersonId> peopleToTraceAndIsolate;

            private ContactTracingIsolationPlan(List<PersonId> peopleToTraceAndIsolate) {
                this.peopleToTraceAndIsolate = peopleToTraceAndIsolate;
            }

        }

        private static class EndIsolationPlan implements Plan {

            private final List<PersonId> peopleToEndIsolation;

            private EndIsolationPlan(List<PersonId> peopleToEndIsolation) {
                this.peopleToEndIsolation = peopleToEndIsolation;
            }

        }

        private static class ContractTracingCompletePlan implements Plan {

            private final RegionId regionId;

            private ContractTracingCompletePlan(RegionId regionId) {
                this.regionId = regionId;
            }
        }

        private static class ContactTracingAscertainmentPlan implements Plan {

            private final PersonId personId;

            private ContactTracingAscertainmentPlan(PersonId personId) {
                this.personId = personId;
            }

        }

        private final static class Counter {
            int count;
        }

    }

}
