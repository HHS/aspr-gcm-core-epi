package gcm.core.epi.plugin.behavior;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.GlobalComponentId;
import gcm.scenario.PersonId;
import gcm.scenario.RandomNumberGeneratorId;
import gcm.simulation.Environment;
import gcm.simulation.Plan;
import gcm.simulation.partition.Filter;
import gcm.simulation.partition.LabelSet;
import gcm.simulation.partition.Partition;
import gcm.simulation.partition.PartitionSampler;
import org.apache.commons.math3.distribution.BinomialDistribution;

import java.util.*;
import java.util.stream.IntStream;

public class RandomTestingBehaviorPlugin extends BehaviorPlugin {

    static final GlobalComponentId RANDOM_TESTING_MANAGER_ID = new GlobalComponentId() {
        @Override
        public String toString() {
            return "RANDOM_TESTING_MANAGER_ID";
        }
    };

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(RandomTestingGlobalProperty.class));
    }

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>(EnumSet.allOf(RandomTestingPersonProperty.class));
    }

    @Override
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId, ContactGroupType selectedContactGroupType) {
        // If a person is staying home, substitute any school or work infections contacts with home contacts
        boolean isStayingHome = environment.getPersonPropertyValue(personId, PersonProperty.IS_STAYING_HOME);
        if (isStayingHome) {
            /*TODO: Need to unify all IS_STAYING_HOME logic in one place.
               Currently this plugin drops Global contacts if TEST_ISOLATION_TRANSMISSION_REDUCTION != 0
               Since this plugin is called by the CombinationBehaviorPlugin, this effect always happens.
               Need to try and ensure that this effect isn't duplicated between plugins.
               May want to unify it into a new plugin, the Combination plugin, somewhere else?
             */
            Map<ContactGroupType, Double> transmissionReductionBySetting = environment.getGlobalPropertyValue(
                    RandomTestingGlobalProperty.TEST_ISOLATION_TRANSMISSION_REDUCTION);
            double transmissionReduction = transmissionReductionBySetting.getOrDefault(selectedContactGroupType, 0.0);
            if (environment.getRandomGeneratorFromId(RandomTestingRandomId.ID).nextDouble() < transmissionReduction) {
                // Drop this transmission event
                return Optional.empty();
            }
            if (selectedContactGroupType != ContactGroupType.HOME &&
                    selectedContactGroupType != ContactGroupType.GLOBAL) {
                return Optional.of(ContactGroupType.HOME);
            } else {
                return Optional.of(selectedContactGroupType);
            }
        }
        return Optional.of(selectedContactGroupType);
    }

    @Override
    public double getInfectionProbability(Environment environment, ContactGroupType contactSetting, PersonId personId) {
        boolean isStayingHome = environment.getPersonPropertyValue(personId, PersonProperty.IS_STAYING_HOME);
        if (isStayingHome &&
                contactSetting != ContactGroupType.HOME &&
                contactSetting != ContactGroupType.GLOBAL) {
            return 0.0;
        } else {
            // Assume has effective zero effect on reducing global transmission risk if this person is the target
            return 1.0;
        }
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(RandomTestingRandomId.ID);
        return randomIds;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        super.load(experimentBuilder);
        experimentBuilder.addGlobalComponentId(RANDOM_TESTING_MANAGER_ID, RandomTestingManager.class);
    }

    public enum RandomTestingGlobalProperty implements DefinedGlobalProperty {

        FRACTION_OF_POPULATION_TESTED_DAILY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        INFECTION_TARGETING_RATIO(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build()),

        TEST_SENSITIVITY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build()),

        TEST_ISOLATION_DELAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        TEST_ISOLATION_DURATION(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        TEST_ISOLATION_TRANSMISSION_REDUCTION(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<ContactGroupType, Double>>() {
                })
                .defaultValue(new EnumMap<ContactGroupType, Double>(ContactGroupType.class))
                .isMutable(false).build()),

        TESTING_START_DAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        TESTING_END_DAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(Double.POSITIVE_INFINITY).isMutable(false).build());

        private final TypedPropertyDefinition propertyDefinition;
        private final boolean isExternal;

        RandomTestingGlobalProperty(TypedPropertyDefinition propertyDefinition, boolean isExternal) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = isExternal;
        }

        RandomTestingGlobalProperty(TypedPropertyDefinition propertyDefinition) {
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

    public enum RandomTestingPersonProperty implements DefinedPersonProperty {

        HAS_RECENTLY_TESTED_POSITIVE(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false).build());

        final TypedPropertyDefinition propertyDefinition;

        RandomTestingPersonProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    private enum RandomTestingRandomId implements RandomNumberGeneratorId {
        ID
    }

    public static class RandomTestingManager extends AbstractComponent {

        private static final Object INFECTED_PARTITION_KEY = new Object();

        @Override
        public void init(Environment environment) {
            double startTestingTime = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.TESTING_START_DAY);
            environment.addPlan(new RandomTestingPlan(), startTestingTime);
            // Add index
            environment.addPartition(Partition.create().filter(Filter.compartment(Compartment.INFECTED)),
                    INFECTED_PARTITION_KEY);
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            if (plan.getClass() == RandomTestingPlan.class) {
                double infectionTargetingRatio = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.INFECTION_TARGETING_RATIO);
                int numberInfected = environment.getPartitionSize(INFECTED_PARTITION_KEY, LabelSet.builder().build());
                int numberNotInfected = environment.getPopulationCount() - numberInfected;
                double probabilityTestInfected = numberInfected * infectionTargetingRatio /
                        (numberInfected * infectionTargetingRatio + numberNotInfected);

                double fractionOfPopulationTested = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.FRACTION_OF_POPULATION_TESTED_DAILY);
                int numberToTest = (int) Math.round(environment.getPopulationCount() * fractionOfPopulationTested);
                double testSensitivity = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.TEST_SENSITIVITY);
                // Sample how many infected people will be tested and test positive
                int numberInfectedTestPositive = new BinomialDistribution(environment.getRandomGeneratorFromId(RandomTestingRandomId.ID),
                        numberToTest, probabilityTestInfected * testSensitivity).sample();
                double testIsolationDelay = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.TEST_ISOLATION_DELAY);

                // Randomly select infected people to isolate.
                IntStream.range(0, numberInfectedTestPositive).forEach(
                        x -> {
                            Optional<PersonId> personToIsolate = environment.samplePartition(INFECTED_PARTITION_KEY,
                                    PartitionSampler.builder().setRandomNumberGeneratorId(RandomTestingRandomId.ID).build());
                            if (personToIsolate.isPresent()) {
                                boolean hasTestedPositive = environment.getPersonPropertyValue(personToIsolate.get(),
                                        RandomTestingPersonProperty.HAS_RECENTLY_TESTED_POSITIVE);
                                if (!hasTestedPositive) {
                                    // Plan to isolate this person
                                    environment.addPlan(new StartIsolatingPlan(personToIsolate.get()),
                                            environment.getTime() + testIsolationDelay);
                                }
                            }
                        }
                );
                // Plan next testing
                double time = environment.getTime();
                double endTestingTime = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.TESTING_END_DAY);
                // TODO: Handle extinction of infections but plans to infect people in the future, potentially
                if (time + 1 < endTestingTime &&
                        environment.getCompartmentPopulationCount(Compartment.INFECTED) > 0) {
                    environment.addPlan(plan, time + 1);
                }
            } else if (plan.getClass() == StartIsolatingPlan.class) {
                // Start Isolating
                PersonId personToIsolate = ((StartIsolatingPlan) plan).personId;
                environment.setPersonPropertyValue(personToIsolate, PersonProperty.IS_STAYING_HOME, true);
                double isolationDuration = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.TEST_ISOLATION_DURATION);
                environment.addPlan(new StopIsolatingPlan(personToIsolate), environment.getTime() + isolationDuration);
            } else if (plan.getClass() == StopIsolatingPlan.class) {
                // Stop Isolating
                PersonId personIsolated = ((StopIsolatingPlan) plan).personId;
                environment.setPersonPropertyValue(personIsolated, PersonProperty.IS_STAYING_HOME, false);
                environment.setPersonPropertyValue(personIsolated, RandomTestingPersonProperty.HAS_RECENTLY_TESTED_POSITIVE, false);
            }

        }

        private static class RandomTestingPlan implements Plan {
        }

        private static class StartIsolatingPlan implements Plan {
            private final PersonId personId;

            private StartIsolatingPlan(PersonId personId) {
                this.personId = personId;
            }
        }

        private static class StopIsolatingPlan implements Plan {
            private final PersonId personId;

            private StopIsolatingPlan(PersonId personId) {
                this.personId = personId;
            }
        }

    }

}
