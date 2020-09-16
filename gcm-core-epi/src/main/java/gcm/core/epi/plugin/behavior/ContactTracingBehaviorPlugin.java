package gcm.core.epi.plugin.behavior;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.components.AbstractComponent;
import gcm.core.epi.components.ContactManager;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.propertytypes.*;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        CONTACT_TRACING_TRIGGER_START(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false)
                .timeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        CONTACT_TRACING_TRIGGER_END(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false)
                .timeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build());

        private final TypedPropertyDefinition propertyDefinition;

        ContactTracingRegionProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public enum ContactTracingPersonProperty implements DefinedPersonProperty {

        // Will track infectious contacts in the GLOBAL setting
        GLOBAL_INFECTION_SOURCE_PERSON_ID(TypedPropertyDefinition.builder()
                .type(Integer.class).defaultValue(-1).mapOption(MapOption.ARRAY).build()),

        // Track infectious contacts outside of GLOBAL setting.
        // This is used when CONTACT_TRACING_MAX_CONTACTS_TO_TRACE is limited
        NON_GLOBAL_INFECTION_SOURCE_PERSON_ID(TypedPropertyDefinition.builder()
                .type(Integer.class).defaultValue(-1).mapOption(MapOption.ARRAY).build());

        final TypedPropertyDefinition propertyDefinition;

        ContactTracingPersonProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public enum ContactTracingGlobalProperty implements DefinedGlobalProperty {

        MAXIMUM_INFECTIONS_TO_TRACE(TypedPropertyDefinition.builder()
                .type(FipsCodeDouble.class).defaultValue(ImmutableFipsCodeDouble.builder().build())
                .isMutable(false).build()),

        CURRENT_INFECTIONS_BEING_TRACED(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<FipsCode, ContactTracingManager.Counter>>() {
                })
                .defaultValue(new HashMap<FipsCode,
                        ContactTracingManager.Counter>()).build(), false),

        FRACTION_INFECTIONS_TRACED(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).build()),

        // This value will multiply both infected & uninfected contacts
        // There's no way to specify that you want to correctly identify and quarantine X% of infected contacts and
        // then also say that some other number of people should quarantine with Z probability.
        FRACTION_CONTACTS_TRACED_AND_ISOLATED(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<ContactGroupType, Double>>() {
                })
                .defaultValue(new EnumMap<ContactGroupType, Double>(ContactGroupType.class))
                .isMutable(false).build()),

        TRACED_CONTACT_STAY_HOME_DURATION(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        CONTACT_TRACING_ASCERTAINMENT_DELAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        CONTACT_TRACING_DELAY(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<ContactGroupType, Double>>() {
                })
                .defaultValue(new EnumMap<ContactGroupType, Double>(ContactGroupType.class))
                .isMutable(false).build()),

        CONTACT_TRACING_TIME(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        CONTACT_TRACING_START(TypedPropertyDefinition.builder()
                .type(String.class).defaultValue("").isMutable(false).build()),

        CONTACT_TRACING_END(TypedPropertyDefinition.builder()
                .type(String.class).defaultValue("").isMutable(false).build()),

        CONTACT_TRACING_MAX_CONTACTS_TO_TRACE(TypedPropertyDefinition.builder()
                .type(Integer.class).defaultValue(Integer.MAX_VALUE).isMutable(false).build()),

        ADDITIONAL_GLOBAL_CONTACTS_TO_TRACE(TypedPropertyDefinition.builder()
                .type(Integer.class).defaultValue(0).isMutable(false).build());

        final TypedPropertyDefinition propertyDefinition;
        final boolean isExternal;

        ContactTracingGlobalProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = true;
        }

        ContactTracingGlobalProperty(TypedPropertyDefinition propertyDefinition, boolean isExternal) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = isExternal;
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

    private enum ContactTracingRandomId implements RandomNumberGeneratorId {
        ID
    }

    public static class ContactTracingManager extends AbstractComponent {

        private final Map<FipsCode, Double> maximumInfectionsToTrace = new HashMap<>();
        private FipsScope scope;

        @Override
        public void init(Environment environment) {
            // Get maximum number of infections that can be traced
            FipsCodeDouble maximumInfectionsToTraceFromProperty = environment.getGlobalPropertyValue(
                    ContactTracingGlobalProperty.MAXIMUM_INFECTIONS_TO_TRACE);
            maximumInfectionsToTrace.putAll(maximumInfectionsToTraceFromProperty.getFipsCodeValues(environment));
            scope = maximumInfectionsToTraceFromProperty.scope();
            Map<FipsCode, Counter> currentInfectionsBeingTraced = new HashMap<>();
            maximumInfectionsToTrace.keySet().forEach(
                    fipsCode -> currentInfectionsBeingTraced.put(fipsCode, new Counter())
            );
            environment.setGlobalPropertyValue(ContactTracingGlobalProperty.CURRENT_INFECTIONS_BEING_TRACED,
                    currentInfectionsBeingTraced);

            // Are we ever possibly doing any contact tracing anywhere? If not, don't bother initializing anything else
            if (maximumInfectionsToTrace.values().stream().anyMatch(value -> value != 0)) {
                // Begin observing
                setObservationStatus(environment, true);
            }
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
                    double caseAscertainmentDelay = environment.getGlobalPropertyValue(
                            ContactTracingGlobalProperty.CONTACT_TRACING_ASCERTAINMENT_DELAY);

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
            //TODO: Handle contacts of contacts
            // Determine if we are currently tracing contacts
            RegionId regionId = environment.getPersonRegion(personId);
            boolean triggerIsInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                    ContactTracingRegionProperty.CONTACT_TRACING_TRIGGER_START,
                    ContactTracingRegionProperty.CONTACT_TRACING_TRIGGER_END);
            if (triggerIsInEffect) {
                FipsCode fipsCode = scope.getFipsSubCode(regionId);
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
                        List<PersonId> personStayingHome = new ArrayList<>(1);
                        personStayingHome.add(personId);
                        double stayAtHomeDuration = environment.getGlobalPropertyValue(ContactTracingGlobalProperty.TRACED_CONTACT_STAY_HOME_DURATION);
                        environment.addPlan(new EndIsolationPlan(personStayingHome), environment.getTime() + stayAtHomeDuration);

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

                        // Determine if we need to limit the number of people we trace
                        int maxNumberOfContactsToTrace = environment.getGlobalPropertyValue(
                                ContactTracingGlobalProperty.CONTACT_TRACING_MAX_CONTACTS_TO_TRACE);
                        boolean limitNumberOfContactsToTrace = maxNumberOfContactsToTrace != Integer.MAX_VALUE;

                        List<PersonId> infectionsFromContact = new ArrayList<>();
                        if (limitNumberOfContactsToTrace) {
                            // This logic is predicated on infections stopping at this point in time.
                            // It will take longer to identify these people, but we don't expect new infections.
                            //TODO: If identified cases don't shelter & continue infecting people, then this logic is flawed.
                            infectionsFromContact = environment.getPeopleWithPropertyValue(
                                    ContactTracingPersonProperty.NON_GLOBAL_INFECTION_SOURCE_PERSON_ID, personId.getValue());
                        }

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

                                // Add additional random people if needed
                                // Using a set means we don't care about grabbing the same person twice
                                int numberOfAdditionalGlobalContactsToTrace = environment.getGlobalPropertyValue(
                                        ContactTracingGlobalProperty.ADDITIONAL_GLOBAL_CONTACTS_TO_TRACE);
                                Set<PersonId> additionalGlobalContacts = new HashSet<>();
                                IntStream.range(1, numberOfAdditionalGlobalContactsToTrace)
                                        .forEach(x -> ContactManager.getGlobalContactFor(environment, personId,
                                                ContactTracingRandomId.ID)
                                                .ifPresent(additionalGlobalContacts::add));
                                peopleInGroup.addAll(additionalGlobalContacts);
                            }
                            double fractionToTraceAndIsolate = fractionToTraceAndIsolateByGroup.getOrDefault(contactGroupType, 1.0);

                            // Determine if we need to worry about limiting how many people to trace
                            // Don't limit GLOBAL contacts (we let them expand as needed)
                            if (limitNumberOfContactsToTrace && contactGroupType != ContactGroupType.GLOBAL) {
                                // Presume that we're most likely to get actual infections first
                                Set<PersonId> infectionsInGroupToPrioritize = infectionsFromContact.stream()
                                        .filter(peopleInGroup::contains)
                                        .limit(maxNumberOfContactsToTrace)
                                        .collect(Collectors.toSet());

                                // Now add additional non-infections
                                Set<PersonId> additionalContactsToTrace = new HashSet<>();
                                if (infectionsInGroupToPrioritize.size() < maxNumberOfContactsToTrace) {
                                    additionalContactsToTrace = peopleInGroup.stream()
                                            .filter(i -> !infectionsInGroupToPrioritize.contains(i))
                                            .limit(maxNumberOfContactsToTrace - infectionsInGroupToPrioritize.size())
                                            .collect(Collectors.toSet());
                                }
                                infectionsInGroupToPrioritize.addAll(additionalContactsToTrace);

                                // Finally do contact tracing
                                for (PersonId individualContactToTrace : infectionsInGroupToPrioritize) {
                                    if (!individualContactToTrace.equals(personId) &&
                                            environment.getRandomGeneratorFromId(
                                                    ContactTracingRandomId.ID).nextDouble() < fractionToTraceAndIsolate) {
                                        peopleToTraceAndIsolate.add(individualContactToTrace);
                                    }
                                }

                            } else {
                                // Just do contact tracing on everyone
                                for (PersonId personInGroup : peopleInGroup) {
                                    if (!personInGroup.equals(personId) &&
                                            environment.getRandomGeneratorFromId(
                                                    ContactTracingRandomId.ID).nextDouble() < fractionToTraceAndIsolate) {
                                        peopleToTraceAndIsolate.add(personInGroup);
                                    }
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
                FipsCode fipsCode = scope.getFipsSubCode(((ContractTracingCompletePlan) plan).regionId);
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
                ContactGroupType setting = mostRecentInfectionData.transmissionSetting();
                if (mostRecentInfectionData.transmissionOccurred() &&
                        // Never look at the home
                        setting != ContactGroupType.HOME &&
                        // Always look at global and, if we are limiting the contact tracing, look at school & work, too
                        (setting == ContactGroupType.GLOBAL ||
                                (int) environment.getGlobalPropertyValue(ContactTracingGlobalProperty.CONTACT_TRACING_MAX_CONTACTS_TO_TRACE)
                                        < Integer.MAX_VALUE) &&
                        mostRecentInfectionData.sourcePersonId().isPresent()) {
                    Optional<PersonId> targetPersonId = mostRecentInfectionData.targetPersonId();
                    if (targetPersonId.isPresent()) {
                        PersonId sourcePersonId = mostRecentInfectionData.sourcePersonId().get();
                        if (setting == ContactGroupType.GLOBAL) {
                            environment.setPersonPropertyValue(targetPersonId.get(),
                                    ContactTracingPersonProperty.GLOBAL_INFECTION_SOURCE_PERSON_ID,
                                    sourcePersonId.getValue());
                        } else {
                            environment.setPersonPropertyValue(targetPersonId.get(),
                                    ContactTracingPersonProperty.NON_GLOBAL_INFECTION_SOURCE_PERSON_ID,
                                    sourcePersonId.getValue());
                        }
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
