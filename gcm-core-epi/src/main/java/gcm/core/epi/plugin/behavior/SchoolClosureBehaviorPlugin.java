package gcm.core.epi.plugin.behavior;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.propertytypes.DayOfWeekSchedule;
import gcm.core.epi.propertytypes.ImmutableDayOfWeekSchedule;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedGroupProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class SchoolClosureBehaviorPlugin extends BehaviorPlugin {

    private static final double COHORT_ONE_FRACTION = 0.5;

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

            // Cohorting
            boolean cohortingInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                    SchoolClosureRegionProperty.COHORTING_TRIGGER_START,
                    SchoolClosureRegionProperty.COHORTING_TRIGGER_END);

            if (summerInEffect) {
                // Summer
                return getSampledContactGroupType(environment, environment.getGlobalPropertyValue(
                        SchoolClosureGlobalProperty.SUMMER_CONTACT_SUBSTITUTION_WEIGHTS));
            } else if (schoolClosureInEffect) {
                // School closure outside of summer
                return getSampledContactGroupType(environment, environment.getGlobalPropertyValue(
                        SchoolClosureGlobalProperty.SCHOOL_CLOSED_CONTACT_SUBSTITUTION_WEIGHTS));
            } else if (cohortingInEffect) {
                // Cohorting and school is open

                // Assign school cohorts if not already assigned
                GroupId schoolId = environment.getGroupsForGroupTypeAndPerson(ContactGroupType.SCHOOL, personId).get(0);
                boolean cohortsAreAssigned = environment.getGroupPropertyValue(schoolId, SchoolClosureSchoolProperty.HAS_ASSIGNED_COHORTS);
                if (!cohortsAreAssigned) {
                    // Assign cohorts

                    // Create groups
                    GroupId cohortOne = environment.addGroup(ContactGroupType.SCHOOL_COHORT);
                    environment.setGroupPropertyValue(cohortOne, SchoolClosureCohortProperty.COHORT_NUMBER, Cohort.ONE);
                    GroupId cohortTwo = environment.addGroup(ContactGroupType.SCHOOL_COHORT);
                    environment.setGroupPropertyValue(cohortTwo, SchoolClosureCohortProperty.COHORT_NUMBER, Cohort.TWO);
                    List<PersonId> schoolMembers = environment.getPeopleForGroup(schoolId);

                    // Shuffle list
                    Collections.shuffle(schoolMembers, RandomAdaptor.createAdaptor(environment.getRandomGeneratorFromId(
                            SchoolClosureRandomId.ID)));
                    // Iterate and assign
                    GroupId firstCohort, secondCohort;
                    if (environment.getRandomGeneratorFromId(SchoolClosureRandomId.ID).nextBoolean()) {
                        firstCohort = cohortOne;
                        secondCohort = cohortTwo;
                    } else {
                        firstCohort = cohortTwo;
                        secondCohort = cohortOne;
                    }
                    int i = 0;
                    for (PersonId schoolMember : schoolMembers) {
                        // Assign first half (floor) to first cohort (randomly selected)
                        if (i < Math.floorDiv(schoolMembers.size(), 2)) {
                            environment.addPersonToGroup(schoolMember, firstCohort);
                        } else {
                            environment.addPersonToGroup(schoolMember, secondCohort);
                        }
                        i++;
                    }

                    environment.setGroupPropertyValue(schoolId, SchoolClosureSchoolProperty.HAS_ASSIGNED_COHORTS, true);
                }

                // Get relevant cohort and schedule
                GroupId cohortId = environment.getGroupsForGroupTypeAndPerson(ContactGroupType.SCHOOL_COHORT,
                        personId).get(0);
                Cohort cohortNumber = environment.getGroupPropertyValue(cohortId, SchoolClosureCohortProperty.COHORT_NUMBER);
                SchoolClosureGlobalProperty cohortScheduleProperty = cohortNumber == Cohort.ONE ?
                        SchoolClosureGlobalProperty.COHORT_ONE_SCHEDULE :
                        SchoolClosureGlobalProperty.COHORT_TWO_SCHEDULE;
                DayOfWeekSchedule cohortSchedule = environment.getGlobalPropertyValue(cohortScheduleProperty);

                // Determine reduction in contact
                double withinCohortTransmissionFraction = environment.getGlobalPropertyValue(
                        SchoolClosureGlobalProperty.WITHIN_COHORT_TRANSMISSION_FRACTION);

                // Finally, select transmission setting
                if (environment.getRandomGeneratorFromId(SchoolClosureRandomId.ID).nextDouble() < withinCohortTransmissionFraction) {
                    if (cohortSchedule.isActiveAt(environment, environment.getTime())) {
                        // Transmission occurs within cohort
                        return Optional.of(ContactGroupType.SCHOOL_COHORT);
                    } else {
                        // Transmission occurs outside of school but within cohort
                        return getSampledContactGroupType(environment, environment.getGlobalPropertyValue(
                                SchoolClosureGlobalProperty.COHORTING_CONTACT_SUBSTITUTION_WEIGHTS));
                    }
                } else {
                    // Transmission doesn't occur
                    return Optional.empty();
                }
            } else {
                // Transmission is not school-related so leave contact group alone
                return Optional.of(selectedContactGroupType);
            }
        }

        // Otherwise leave contacts alone
        return Optional.of(selectedContactGroupType);
    }

    private Optional<ContactGroupType> getSampledContactGroupType(Environment environment, Map<ContactGroupType, Double> weights) {
        List<Pair<ContactGroupType, Double>> weightsList = weights
                .entrySet()
                .stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        EnumeratedDistribution<ContactGroupType> contactGroupDistribution =
                new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(SchoolClosureRandomId.ID),
                        weightsList);
        return Optional.of(contactGroupDistribution.sample());
    }

    @Override
    public Map<ContactGroupType, Set<DefinedGroupProperty>> getGroupProperties() {
        Map<ContactGroupType, Set<DefinedGroupProperty>> groupProperties = new EnumMap<>(ContactGroupType.class);
        groupProperties.put(ContactGroupType.SCHOOL, new HashSet<>(EnumSet.allOf(SchoolClosureSchoolProperty.class)));
        groupProperties.put(ContactGroupType.SCHOOL_COHORT, new HashSet<>(EnumSet.allOf(SchoolClosureCohortProperty.class)));
        return groupProperties;
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(SchoolClosureRandomId.ID);
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
        // Cohorting
        triggerId = environment.getGlobalPropertyValue(SchoolClosureGlobalProperty.COHORTING_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, SchoolClosureRegionProperty.COHORTING_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(SchoolClosureGlobalProperty.COHORTING_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, SchoolClosureRegionProperty.COHORTING_TRIGGER_END);
        // Summer
        triggerId = environment.getGlobalPropertyValue(SchoolClosureGlobalProperty.SUMMER_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, SchoolClosureRegionProperty.SUMMER_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(SchoolClosureGlobalProperty.SUMMER_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, SchoolClosureRegionProperty.SUMMER_TRIGGER_END);
        return triggerCallbacks;
    }

    private enum Cohort {
        ONE,

        TWO
    }

    private enum SchoolClosureSchoolProperty implements DefinedGroupProperty {

        HAS_ASSIGNED_COHORTS(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build());

        private final PropertyDefinition propertyDefinition;

        SchoolClosureSchoolProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    private enum SchoolClosureCohortProperty implements DefinedGroupProperty {

        COHORT_NUMBER(PropertyDefinition.builder()
                .setType(Cohort.class).setDefaultValue(Cohort.ONE).build());

        private final PropertyDefinition propertyDefinition;

        SchoolClosureCohortProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    private enum SchoolClosureRandomId implements RandomNumberGeneratorId {
        ID
    }

    public enum SchoolClosureGlobalProperty implements DefinedGlobalProperty {

        SCHOOL_CLOSURE_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SCHOOL_CLOSURE_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        COHORTING_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        COHORTING_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SUMMER_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SUMMER_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SCHOOL_CLOSED_CONTACT_SUBSTITUTION_WEIGHTS(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(getSchoolClosedSubstitutionWeights())
                .setPropertyValueMutability(false).build()),

        COHORTING_CONTACT_SUBSTITUTION_WEIGHTS(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(getCohortingSubstitutionWeights())
                .setPropertyValueMutability(false).build()),

        SUMMER_CONTACT_SUBSTITUTION_WEIGHTS(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(getSummerSubstitutionWeights())
                .setPropertyValueMutability(false).build()),

        COHORT_ONE_SCHEDULE(PropertyDefinition.builder()
                .setType(ImmutableDayOfWeekSchedule.class).setDefaultValue(DayOfWeekSchedule.mondayToFriday()).build()),

        COHORT_TWO_SCHEDULE(PropertyDefinition.builder()
                .setType(ImmutableDayOfWeekSchedule.class).setDefaultValue(DayOfWeekSchedule.mondayToFriday()).build()),

        WITHIN_COHORT_TRANSMISSION_FRACTION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(1.0).build());

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

        private static Map<ContactGroupType, Double> getCohortingSubstitutionWeights() {
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
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        COHORTING_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        COHORTING_TRIGGER_END(PropertyDefinition.builder()
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
