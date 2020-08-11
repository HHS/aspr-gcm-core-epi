package gcm.core.epi.plugin.vaccine.resourcebased;

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
import gcm.core.epi.util.property.DefinedResourceProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Equality;
import gcm.simulation.Filter;
import gcm.simulation.Plan;
import gcm.util.MultiKey;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

public class ResourceBasedVaccinePlugin implements VaccinePlugin {

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(VaccineGlobalProperty.class));
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        VaccinePlugin.super.load(experimentBuilder);
        // Track when last dose of vaccine was received
        experimentBuilder.setResourceTimeTracking(VaccineId.VACCINE_ONE, TimeTrackingPolicy.TRACK_TIME);
        experimentBuilder.addGlobalComponentId(VACCINE_MANAGER_IDENTIFIER, VaccineManager.class);
    }

    @Override
    public Map<ResourceId, Set<DefinedResourceProperty>> getResourceProperties() {
        Map<ResourceId, Set<DefinedResourceProperty>> resourcePropertyMap =
                new HashMap<>();
        // Vaccine properties
        resourcePropertyMap.put(VaccineId.VACCINE_ONE, new HashSet<>(EnumSet.allOf(VaccineProperty.class)));
        return resourcePropertyMap;
    }

    private double getEffectivenessFunctionValue(Environment environment, PersonId personId) {
        if (environment.getPersonResourceLevel(personId, VaccineId.VACCINE_ONE) > 0) {
            double vaccinationTime = environment.getPersonResourceTime(personId, VaccineId.VACCINE_ONE);
            double relativeTime = environment.getTime() - vaccinationTime;
            EffectivenessFunction effectivenessFunction = environment.getResourcePropertyValue(VaccineId.VACCINE_ONE,
                    VaccineProperty.EFFECTIVENESS_FUNCTION);
            if (relativeTime < effectivenessFunction.initialDelay()) {
                return 0.0;
            } else if (relativeTime < effectivenessFunction.peakTime()) {
                return (relativeTime - effectivenessFunction.initialDelay()) /
                        (effectivenessFunction.peakTime() - effectivenessFunction.initialDelay());
            } else if (relativeTime < effectivenessFunction.peakTime() + effectivenessFunction.peakDuration()) {
                return 1.0;
            } else {
                return Math.exp(-effectivenessFunction.afterPeakDecay() *
                        (relativeTime - effectivenessFunction.peakTime() - effectivenessFunction.peakDuration()));
            }
        } else {
            return 0.0;
        }
    }

    @Override
    public double getVES(Environment environment, PersonId personId) {
        double vES = environment.getResourcePropertyValue(VaccineId.VACCINE_ONE, VaccineProperty.VE_S);
        return vES * getEffectivenessFunctionValue(environment, personId);
    }

    @Override
    public double getVEI(Environment environment, PersonId personId) {
        double vEI = environment.getResourcePropertyValue(VaccineId.VACCINE_ONE, VaccineProperty.VE_I);
        return vEI * getEffectivenessFunctionValue(environment, personId);
    }

    @Override
    public double getVEP(Environment environment, PersonId personId) {
        double vEP = environment.getResourcePropertyValue(VaccineId.VACCINE_ONE, VaccineProperty.VE_P);
        return vEP * getEffectivenessFunctionValue(environment, personId);
    }

    /*
        The global properties added to the simulation by this module
     */
    public enum VaccineGlobalProperty implements DefinedGlobalProperty {

        VACCINE_DELIVERIES(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(new HashMap<Double, Long>()).build()),

        VACCINATION_START_DAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        VACCINATION_RATE_PER_DAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

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

    enum VaccineId implements ResourceId {

        VACCINE_ONE

    }

    enum VaccineProperty implements DefinedResourceProperty {

        VE_S(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VE_I(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VE_P(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        EFFECTIVENESS_FUNCTION(PropertyDefinition.builder().setType(EffectivenessFunction.class)
                .setDefaultValue(ImmutableEffectivenessFunction.builder().build()).build());

        final PropertyDefinition propertyDefinition;

        VaccineProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public static class VaccineManager extends AbstractComponent {

        // Keys for vaccine indexes
        private static final Object TO_VACCINATE_INDEX_KEY = new Object();
        private final Map<AgeGroup, Object> vaccineIndexKeys = new HashMap<>();
        // Re-used array for selecting age group to vaccinate next
        private final List<Double> vaccineCumulativeWeights = new ArrayList<>();
        // Track whether currently vaccinating
        boolean currentlyVaccinating = false;
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
                    Filter filter = Filter.resource(
                            VaccineId.VACCINE_ONE,
                            Equality.EQUAL,
                            0)
                            .and(Filter.property(
                                    PersonProperty.AGE_GROUP_INDEX,
                                    Equality.EQUAL,
                                    populationDescription.ageGroupPartition().getAgeGroupIndexFromName(ageGroup.toString())
                            ));
                    Object indexKey = new MultiKey(TO_VACCINATE_INDEX_KEY, ageGroup);
                    vaccineIndexKeys.put(ageGroup, indexKey);
                    environment.addPopulationIndex(filter, indexKey);
                }

                // Schedule vaccine deliveries
                Map<Double, Long> vaccineDeliveries = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINE_DELIVERIES);
                for (Map.Entry<Double, Long> entry : vaccineDeliveries.entrySet()) {
                    environment.addPlan(new VaccineDeliveryPlan(entry.getValue()), entry.getKey());
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
            } else if (plan.getClass() == VaccineDeliveryPlan.class) {
                // TODO: Add resources to region

                /*                environment.addResourceToRegion(VaccineId.VACCINE_ONE, -----,
                        ((VaccineDeliveryPlan) plan).doses);*/
                if (!currentlyVaccinating) {
                    vaccinateAndScheduleNext(environment);
                }
            } else {
                throw new RuntimeException("Unhandled Vaccine Plan");
            }
        }

        private void vaccinateAndScheduleNext(Environment environment) {
            // TODO: add Region logic
            // environment.getRegionResourceLevel(REGION-ID, VaccineId.VACCINE_ONE) > 0
            boolean hasResource = true;
            if (hasResource) {
                currentlyVaccinating = true;
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

                // Randomly select age group using the cumulative weights
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
                    environment.transferResourceToPerson(VaccineId.VACCINE_ONE, personId.get(), 1);

                    // Schedule next vaccination
                    final double vaccinationTime = environment.getTime() + interVaccinationDelayDistribution.sample();
                    environment.addPlan(new VaccinationPlan(), vaccinationTime);
                } else {
                    // Nobody left to vaccinate for now, so register to observe new arrivals
                    environment.observeGlobalPersonArrival(true);
                }
            } else {
                // No vaccine available, so pause vaccinating for now and wait for vaccine delivery
                currentlyVaccinating = false;
            }
        }

        /*
            A plan to add vaccine to the simulation
         */
        private class VaccineDeliveryPlan implements Plan {
            final Long doses;

            private VaccineDeliveryPlan(Long doses) {
                this.doses = doses;
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
