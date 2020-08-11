package gcm.core.epi.plugin.vaccine.onedose;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.plugin.vaccine.VaccinePlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.ImmutableAgeWeights;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.MapOption;
import gcm.scenario.PersonId;
import gcm.scenario.PropertyDefinition;
import gcm.simulation.Environment;
import gcm.simulation.Equality;
import gcm.simulation.Filter;
import gcm.simulation.Plan;
import gcm.util.MultiKey;
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
        The person properties added to the simulation by this module
     */
    public enum VaccinePersonProperty implements DefinedPersonProperty {

        VACCINE_STATUS(PropertyDefinition.builder()
                .setType(OneDoseVaccineStatus.class)
                .setDefaultValue(OneDoseVaccineStatus.NOT_VACCINATED)
                .setMapOption(MapOption.ARRAY).build());

        private final PropertyDefinition propertyDefinition;

        VaccinePersonProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    /*
        The global properties added to the simulation by this module
     */
    public enum VaccineGlobalProperty implements DefinedGlobalProperty {

        VE_S(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VE_I(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VE_P(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VACCINATION_START_DAY(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VACCINATION_RATE_PER_DAY(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VE_DELAY_DAYS(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VE_DURATION_DAYS(PropertyDefinition.builder().setType(Double.class).setDefaultValue(Double.POSITIVE_INFINITY)
                .setPropertyValueMutability(false).build()),

        VACCINE_UPTAKE_WEIGHTS(PropertyDefinition.builder()
                .setType(AgeWeights.class)
                .setDefaultValue(ImmutableAgeWeights.builder().defaultValue(1.0).build()).build());

        private final PropertyDefinition propertyDefinition;

        VaccineGlobalProperty(PropertyDefinition propertyDefinition) {
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

    public static class VaccineManager extends AbstractComponent {

        // Keys for vaccine indexes
        private static final Object TO_VACCINATE_INDEX_KEY = new Object();
        private final Map<AgeGroup, Object> vaccineIndexKeys = new HashMap<>();
        // Re-used array for selecting age group to vaccinate next
        private final List<Double> vaccineCumulativeWeights = new ArrayList<>();
        private RealDistribution interVaccinationDelayDistribution;

        @Override
        public void init(final Environment environment) {

            final double vaccinationRatePerDay = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINATION_RATE_PER_DAY);
            if (vaccinationRatePerDay > 0) {
                // Make distribution for inter-vaccination time delays
                final RandomGenerator randomGenerator = environment.getRandomGenerator();
                interVaccinationDelayDistribution = new ExponentialDistribution(randomGenerator,
                        1 / vaccinationRatePerDay);

                // Random vaccination target indexes
                PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                        GlobalProperty.POPULATION_DESCRIPTION);
                for (AgeGroup ageGroup : populationDescription.ageGroupPartition().ageGroupList()) {
                    Filter filter = Filter.property(
                            VaccinePersonProperty.VACCINE_STATUS,
                            Equality.EQUAL,
                            OneDoseVaccineStatus.NOT_VACCINATED)
                            .and(Filter.property(
                                    PersonProperty.AGE_GROUP_INDEX,
                                    Equality.EQUAL,
                                    populationDescription.ageGroupPartition().getAgeGroupIndexFromName(ageGroup.toString())
                            ));
                    Object indexKey = new MultiKey(TO_VACCINATE_INDEX_KEY, ageGroup);
                    vaccineIndexKeys.put(ageGroup, indexKey);
                    environment.addPopulationIndex(filter, indexKey);
                }

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
            int ageGroupIndex = 0;
            double cumulativeWeight = 0;
            // Calculate cumulative weights for each age group
            PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                    GlobalProperty.POPULATION_DESCRIPTION);
            AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
            for (AgeGroup ageGroup : ageGroupPartition.ageGroupList()) {
                double weight = vaccineUptakeWeights.getWeight(ageGroup) *
                        environment.getIndexSize(vaccineIndexKeys.get(ageGroup));
                cumulativeWeight += weight;
                if (vaccineCumulativeWeights.size() <= ageGroupIndex) {
                    vaccineCumulativeWeights.add(cumulativeWeight);
                } else {
                    vaccineCumulativeWeights.set(ageGroupIndex, cumulativeWeight);
                }
                ageGroupIndex++;
            }

            // Randomly select age group using the cumulative weights            // Get a random person to vaccinate, if possible, taking into account vaccine uptake weights
            //            AgeWeights vaccineUptakeWeights = environment.getGlobalPropertyValue(
            //                    VaccineGlobalProperty.VACCINE_UPTAKE_WEIGHTS);
            //            int ageGroupIndex = 0;
            //            double cumulativeWeight = 0;
            //            // Calculate cumulative weights for each age group
            //            PopulationDescription populationDescription = environment.getGlobalPropertyValue(
            //                    GlobalProperty.POPULATION_DESCRIPTION);
            //            AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
            //            for (AgeGroup ageGroup : ageGroupPartition.ageGroupList()) {
            //                double weight = vaccineUptakeWeights.getWeight(ageGroup) *
            //                        environment.getIndexSize(vaccineIndexKeys.get(ageGroup));
            //                cumulativeWeight += weight;
            //                if (vaccineCumulativeWeights.size() <= ageGroupIndex) {
            //                    vaccineCumulativeWeights.add(cumulativeWeight);
            //                } else {
            //                    vaccineCumulativeWeights.set(ageGroupIndex, cumulativeWeight);
            //                }
            //                ageGroupIndex++;
            //            }
            //
            //            // Randomly select age group using the cumulative weights
            //            final Optional<PersonId> personId;
            //            if (cumulativeWeight == 0) {
            //                personId = Optional.empty();
            //            } else {
            //                double targetWeight = environment.getRandomGenerator().nextDouble() * cumulativeWeight;
            //                ageGroupIndex = 0;
            //                while (vaccineCumulativeWeights.get(ageGroupIndex) < targetWeight) {
            //                    ageGroupIndex++;
            //                }
            //                personId = environment.getRandomIndexedPerson(vaccineIndexKeys.get(ageGroupPartition.getAgeGroupFromIndex(ageGroupIndex)));
            //            }
            final Optional<PersonId> personId;
            if (cumulativeWeight == 0) {
                personId = Optional.empty();
            } else {
                double targetWeight = environment.getRandomGenerator().nextDouble() * cumulativeWeight;
                ageGroupIndex = 0;
                while (vaccineCumulativeWeights.get(ageGroupIndex) < targetWeight) {
                    ageGroupIndex++;
                }
                personId = environment.getRandomIndexedPerson(vaccineIndexKeys.get(ageGroupPartition.getAgeGroupFromIndex(ageGroupIndex)));
            }

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
        private class VaccinationPlan implements Plan {
        }

        /*
            A plan to toggle vaccine protection on or off
         */
        private class VaccineProtectionTogglePlan implements Plan {
            final PersonId personId;

            private VaccineProtectionTogglePlan(PersonId personId) {
                this.personId = personId;
            }
        }

    }

}
