package gcm.core.epi.plugin.behavior;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Filter;
import gcm.simulation.Plan;
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
        boolean isStayingHome = environment.getPersonPropertyValue(personId, RandomTestingPersonProperty.IS_STAYING_HOME);
        if (isStayingHome) {
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
        boolean isStayingHome = environment.getPersonPropertyValue(personId, RandomTestingPersonProperty.IS_STAYING_HOME);
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

        FRACTION_OF_POPULATION_TESTED_DAILY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        INFECTION_TARGETING_RATIO(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(1.0).setPropertyValueMutability(false).build()),

        TEST_SENSITIVITY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(1.0).setPropertyValueMutability(false).build()),

        TEST_ISOLATION_DELAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        TEST_ISOLATION_DURATION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        TEST_ISOLATION_TRANSMISSION_REDUCTION(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(new EnumMap<ContactGroupType, Double>(ContactGroupType.class))
                .setPropertyValueMutability(false).build()),

        TESTING_START_DAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        TESTING_END_DAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(Double.POSITIVE_INFINITY).setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;
        private final boolean isExternal;

        RandomTestingGlobalProperty(PropertyDefinition propertyDefinition, boolean isExternal) {
            this.propertyDefinition = propertyDefinition;
            this.isExternal = isExternal;
        }

        RandomTestingGlobalProperty(PropertyDefinition propertyDefinition) {
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

    public enum RandomTestingPersonProperty implements DefinedPersonProperty {

        HAS_RECENTLY_TESTED_POSITIVE(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),

        IS_STAYING_HOME(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build());

        final PropertyDefinition propertyDefinition;

        RandomTestingPersonProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    private enum RandomTestingRandomId implements RandomNumberGeneratorId {
        ID
    }

    public static class RandomTestingManager extends AbstractComponent {

        private static final Object INFECTED_INDEX_KEY = new Object();

        @Override
        public void init(Environment environment) {
            double startTestingTime = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.TESTING_START_DAY);
            environment.addPlan(new RandomTestingPlan(), startTestingTime);
            // Add index
            environment.addPopulationIndex(Filter.compartment(Compartment.INFECTED), INFECTED_INDEX_KEY);
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            if (plan.getClass() == RandomTestingPlan.class) {
                double infectionTargetingRatio = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.INFECTION_TARGETING_RATIO);
                int numberInfected = environment.getIndexSize(INFECTED_INDEX_KEY);
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
                            Optional<PersonId> personToIsolate = environment.getRandomIndexedPersonFromGenerator(INFECTED_INDEX_KEY,
                                    RandomTestingRandomId.ID);
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
                environment.setPersonPropertyValue(personToIsolate, RandomTestingPersonProperty.IS_STAYING_HOME, true);
                double isolationDuration = environment.getGlobalPropertyValue(RandomTestingGlobalProperty.TEST_ISOLATION_DURATION);
                environment.addPlan(new StopIsolatingPlan(personToIsolate), environment.getTime() + isolationDuration);
            } else if (plan.getClass() == StopIsolatingPlan.class) {
                // Stop Isolating
                PersonId personIsolated = ((StopIsolatingPlan) plan).personId;
                environment.setPersonPropertyValue(personIsolated, RandomTestingPersonProperty.IS_STAYING_HOME, false);
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
