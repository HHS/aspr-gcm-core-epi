package gcm.core.epi.plugin.vaccine.onedose;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.plugin.vaccine.VaccinePlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.ImmutableAgeWeights;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.PersonId;
import gcm.scenario.RandomNumberGeneratorId;
import gcm.simulation.Environment;
import gcm.simulation.Equality;
import gcm.simulation.Plan;
import gcm.simulation.partition.Filter;
import gcm.simulation.partition.Partition;
import gcm.simulation.partition.PartitionSampler;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

public class OneDoseVaccinePlugin implements VaccinePlugin {

    /*
        Determines if the person in question is vaccine protected
     */
    private boolean isVaccineProtected(Environment environment, PersonId personId) {
        return environment.getPersonPropertyValue(personId, VaccinePersonProperty.VACCINE_STATUS) ==
                OneDoseVaccineStatus.VACCINE_PROTECTED;
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(VaccineRandomId.ID);
        return randomIds;
    }

    @Override
    public double getVES(Environment environment, PersonId personId) {
        return isVaccineProtected(environment, personId) ?
                environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_S) : 0.0;
    }

    @Override
    public double getVEI(Environment environment, PersonId personId) {
        return isVaccineProtected(environment, personId) ?
                environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_I) : 0.0;
    }

    @Override
    public double getVEP(Environment environment, PersonId personId) {
        return isVaccineProtected(environment, personId) ?
                environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_P) : 0.0;
    }


    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(VaccineGlobalProperty.class));
    }

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>(EnumSet.allOf(VaccinePersonProperty.class));
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        VaccinePlugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalComponentId(VACCINE_MANAGER_IDENTIFIER, VaccineManager.class);
    }

    /*
        The person properties added to the simulation by this plugin
     */
    public enum VaccinePersonProperty implements DefinedPersonProperty {

        VACCINE_STATUS(TypedPropertyDefinition.builder()
                .type(OneDoseVaccineStatus.class)
                .defaultValue(OneDoseVaccineStatus.NOT_VACCINATED)
                .build());

        private final TypedPropertyDefinition propertyDefinition;

        VaccinePersonProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    /*
        The global properties added to the simulation by this plugin
     */
    public enum VaccineGlobalProperty implements DefinedGlobalProperty {

        VE_S(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        VE_I(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        VE_P(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        VACCINATION_START_DAY(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        VACCINATION_RATE_PER_DAY(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        VE_DELAY_DAYS(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        VE_DURATION_DAYS(TypedPropertyDefinition.builder().type(Double.class).defaultValue(Double.POSITIVE_INFINITY)
                .isMutable(false).build()),

        VACCINE_UPTAKE_WEIGHTS(TypedPropertyDefinition.builder()
                .type(AgeWeights.class)
                .defaultValue(ImmutableAgeWeights.builder().defaultValue(1.0).build()).build());

        private final TypedPropertyDefinition propertyDefinition;

        VaccineGlobalProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }
    }

    public static class VaccineManager extends AbstractComponent {

        // Key for vaccine partition
        private static final Object VACCINE_PARTITION_KEY = new Object();
        private RealDistribution interVaccinationDelayDistribution;

        @Override
        public void init(final Environment environment) {

            final double vaccinationRatePerDay = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINATION_RATE_PER_DAY);
            if (vaccinationRatePerDay > 0) {
                // Make distribution for inter-vaccination time delays
                final RandomGenerator randomGenerator = environment.getRandomGeneratorFromId(VaccineRandomId.ID);
                interVaccinationDelayDistribution = new ExponentialDistribution(randomGenerator,
                        1 / vaccinationRatePerDay);

                // Set up population partition
                PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                        GlobalProperty.POPULATION_DESCRIPTION);
                List<AgeGroup> ageGroups = populationDescription.ageGroupPartition().ageGroupList();
                environment.addPartition(Partition.builder()
                                // Filter by vaccine status
                                .setFilter(Filter.property(VaccinePersonProperty.VACCINE_STATUS, Equality.EQUAL,
                                        OneDoseVaccineStatus.NOT_VACCINATED))
                                // Partition by age group
                                .setPersonPropertyFunction(PersonProperty.AGE_GROUP_INDEX, ageGroupIndex -> ageGroups.get((int) ageGroupIndex))
                                .build(),
                        VACCINE_PARTITION_KEY);

                // Schedule first vaccination event
                final double vaccinationStartDay = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINATION_START_DAY);
                environment.addPlan(new VaccinationPlan(), vaccinationStartDay);
            }
        }

        @Override
        public void observeGlobalPersonArrival(Environment environment, PersonId personId) {
            /*
             * A new person has been added to the simulation and we must have
             * already started vaccinating because we only start observing arrivals
             * after vaccination has begun
             */
            environment.observeGlobalPersonArrival(false);
            vaccinateAndScheduleNext(environment);
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            if (plan.getClass() == VaccinationPlan.class) {
                // VaccinationPlan means we should vaccinate a random person and schedule the next vaccination
                vaccinateAndScheduleNext(environment);

            } else if (plan.getClass() == VaccineProtectionTogglePlan.class) {
                // VaccineProtectionTogglePlan means we should toggle on or off vaccine protection
                PersonId personId = ((VaccineProtectionTogglePlan) plan).personId;

                // If person is not yet vaccine protected, make them so
                if (environment.getPersonPropertyValue(personId, VaccinePersonProperty.VACCINE_STATUS) ==
                        OneDoseVaccineStatus.VACCINATED_NOT_YET_PROTECTED) {
                    // Make person vaccine protected
                    environment.setPersonPropertyValue(personId, VaccinePersonProperty.VACCINE_STATUS,
                            OneDoseVaccineStatus.VACCINE_PROTECTED);

                    // Handle possibility of finite VE duration, scheduling toggling of vaccine protection off
                    double vaccineEffectivenessDuration =
                            environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_DURATION_DAYS);
                    if (vaccineEffectivenessDuration < Double.POSITIVE_INFINITY) {
                        environment.addPlan(new VaccineProtectionTogglePlan(personId),
                                environment.getTime() + vaccineEffectivenessDuration);
                    }
                } else {  // Person is already vaccine protected, so toggle vaccine protection off
                    environment.setPersonPropertyValue(personId, VaccinePersonProperty.VACCINE_STATUS,
                            OneDoseVaccineStatus.VACCINATED_NO_LONGER_PROTECTED);
                }

            } else {
                throw new RuntimeException("VaccineManager tried to process unknown plan type");
            }
        }

        private void vaccinateAndScheduleNext(Environment environment) {

            // Get a random person to vaccinate, if possible, taking into account vaccine uptake weights
            AgeWeights vaccineUptakeWeights = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINE_UPTAKE_WEIGHTS);

            final Optional<PersonId> personId = environment.samplePartition(VACCINE_PARTITION_KEY, PartitionSampler.builder()
                    .setLabelSetWeightingFunction((observableEnvironment, labelSetInfo) -> {
                        // We know this labelSetInfo will have a label for this person property
                        //noinspection OptionalGetWithoutIsPresent
                        AgeGroup ageGroup = (AgeGroup) labelSetInfo.getPersonPropertyLabel(PersonProperty.AGE_GROUP_INDEX).get();
                        return vaccineUptakeWeights.getWeight(ageGroup);
                    })
                    .setRandomNumberGeneratorId(VaccineRandomId.ID)
                    .build());

            if (personId.isPresent()) {
                // Vaccinate the person
                double vaccineEffectivenessDelay = environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_DELAY_DAYS);
                double vaccineEffectivenessDuration =
                        environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_DURATION_DAYS);
                if (vaccineEffectivenessDelay > 0) {
                    // Need to schedule onset of protection
                    environment.setPersonPropertyValue(personId.get(), VaccinePersonProperty.VACCINE_STATUS,
                            OneDoseVaccineStatus.VACCINATED_NOT_YET_PROTECTED);
                    environment.addPlan(new VaccineProtectionTogglePlan(personId.get()),
                            environment.getTime() + vaccineEffectivenessDelay);
                } else {
                    // Person is immediately protected
                    environment.setPersonPropertyValue(personId.get(), VaccinePersonProperty.VACCINE_STATUS,
                            OneDoseVaccineStatus.VACCINE_PROTECTED);

                    if (vaccineEffectivenessDuration < Double.POSITIVE_INFINITY) {
                        // Need to schedule onset of loss of protection
                        environment.addPlan(new VaccineProtectionTogglePlan(personId.get()),
                                environment.getTime() + vaccineEffectivenessDuration);
                    }
                }

                // Schedule next vaccination
                final double vaccinationTime = environment.getTime() + interVaccinationDelayDistribution.sample();
                environment.addPlan(new VaccinationPlan(), vaccinationTime);
            } else {
                // Nobody left to vaccinate for now, so register to observe new arrivals
                environment.observeGlobalPersonArrival(true);
            }
        }

        /*
            A plan to vaccinate a random person from the population
         */
        private static class VaccinationPlan implements Plan {
        }

        /*
            A plan to toggle vaccine protection on or off
         */
        private static class VaccineProtectionTogglePlan implements Plan {
            final PersonId personId;

            private VaccineProtectionTogglePlan(PersonId personId) {
                this.personId = personId;
            }
        }

    }

}
