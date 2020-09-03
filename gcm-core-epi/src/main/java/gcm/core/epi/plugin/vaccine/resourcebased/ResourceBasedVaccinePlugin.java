package gcm.core.epi.plugin.vaccine.resourcebased;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.plugin.vaccine.VaccinePlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.*;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedResourceProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;
import gcm.simulation.partition.LabelSet;
import gcm.simulation.partition.Partition;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class ResourceBasedVaccinePlugin implements VaccinePlugin {

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(VaccineGlobalProperty.class));
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(VaccineRandomId.ID);
        return randomIds;
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
//            EffectivenessFunction effectivenessFunction = environment.getResourcePropertyValue(VaccineId.VACCINE_ONE,
//                    VaccineProperty.EFFECTIVENESS_FUNCTION);
            EffectivenessFunction effectivenessFunction = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.EFFECTIVENESS_FUNCTION);
            if (relativeTime < effectivenessFunction.initialDelay()) {
                return 0.0;
            } else if (relativeTime < effectivenessFunction.peakTime()) {
                return (relativeTime - effectivenessFunction.initialDelay()) /
                        (effectivenessFunction.peakTime() - effectivenessFunction.initialDelay());
            } else if (relativeTime < effectivenessFunction.peakTime() + effectivenessFunction.peakDuration()) {
                return 1.0;
            } else {
                double decayRate = Math.log(2) / effectivenessFunction.afterPeakHalfLife();
                return Math.exp(-decayRate *
                        (relativeTime - effectivenessFunction.peakTime() - effectivenessFunction.peakDuration()));
            }
        } else {
            return 0.0;
        }
    }

    @Override
    public double getVES(Environment environment, PersonId personId) {
//        double vES = environment.getResourcePropertyValue(VaccineId.VACCINE_ONE, VaccineProperty.VE_S);
        double vES = environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_S);
        return vES * getEffectivenessFunctionValue(environment, personId);
    }

    @Override
    public double getVEI(Environment environment, PersonId personId) {
//        double vEI = environment.getResourcePropertyValue(VaccineId.VACCINE_ONE, VaccineProperty.VE_I);
        double vEI = environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_I);
        return vEI * getEffectivenessFunctionValue(environment, personId);
    }

    @Override
    public double getVEP(Environment environment, PersonId personId) {
//        double vEP = environment.getResourcePropertyValue(VaccineId.VACCINE_ONE, VaccineProperty.VE_P);
        double vEP = environment.getGlobalPropertyValue(VaccineGlobalProperty.VE_P);
        return vEP * getEffectivenessFunctionValue(environment, personId);
    }

    /*
        The global properties added to the simulation by this plugin
     */
    public enum VaccineGlobalProperty implements DefinedGlobalProperty {

        VE_S(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VE_I(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        VE_P(PropertyDefinition.builder().setType(Double.class).setDefaultValue(0.0)
                .setPropertyValueMutability(false).build()),

        EFFECTIVENESS_FUNCTION(PropertyDefinition.builder().setType(EffectivenessFunction.class)
                .setDefaultValue(ImmutableEffectivenessFunction.builder().build()).build()),

        VACCINE_DELIVERIES(PropertyDefinition.builder()
                .setType(Map.class).setDefaultValue(new HashMap<Double, FipsCodeValues>()).build()),

        VACCINATION_START_DAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        VACCINATION_RATE_PER_DAY(PropertyDefinition.builder()
                .setType(FipsCodeValues.class).setDefaultValue(ImmutableFipsCodeValues.builder().build())
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

    enum VaccineId implements ResourceId {

        VACCINE_ONE

    }

    enum VaccineProperty implements DefinedResourceProperty {
        // TODO: Determine how to use Global vs Resource properties
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

        // Key for vaccine partition
        private static final Object VACCINE_PARTITION_KEY = new Object();
        // Vaccines ready to be distributed
        private final Map<FipsCode, Long> vaccineDeliveries = new HashMap<>();
        // Random distributions for vaccine delays
        private final Map<FipsCode, RealDistribution> interVaccinationDelayDistribution = new HashMap<>();
        // Map giving regions in a given FipsCode
        private Map<FipsCode, Set<RegionId>> fipsCodeRegionMap = new HashMap<>();

        @Override
        public void init(final Environment environment) {

            // Determine if we are vaccinating and if so set up plans and distributions
            final FipsCodeValues vaccinationRatePerDayFipsCodeValues = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINATION_RATE_PER_DAY);
            Map<FipsCode, Double> vaccinationRatePerDayByFipsCode = vaccinationRatePerDayFipsCodeValues.getFipsCodeValues(environment);
            fipsCodeRegionMap = vaccinationRatePerDayFipsCodeValues.scope().getFipsCodeRegionMap(environment);
            boolean usingVaccine = false;
            for (Map.Entry<FipsCode, Double> entry : vaccinationRatePerDayByFipsCode.entrySet()) {
                FipsCode fipsCode = entry.getKey();
                double vaccinationRatePerDay = entry.getValue();
                if (vaccinationRatePerDay > 0) {
                    usingVaccine = true;
                    // Make distribution for inter-vaccination time delays
                    final RandomGenerator randomGenerator = environment.getRandomGeneratorFromId(VaccineRandomId.ID);
                    interVaccinationDelayDistribution.put(fipsCode, new ExponentialDistribution(randomGenerator,
                            1 / vaccinationRatePerDay));

                    // Schedule first vaccination event
                    final double vaccinationStartDay = environment.getGlobalPropertyValue(
                            VaccineGlobalProperty.VACCINATION_START_DAY);
                    environment.addPlan(new VaccinationPlan(fipsCode), vaccinationStartDay, fipsCode);
                }
            }

            if (usingVaccine) {

                // Set up population partition
                PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                        GlobalProperty.POPULATION_DESCRIPTION);
                List<AgeGroup> ageGroups = populationDescription.ageGroupPartition().ageGroupList();
                environment.addPopulationPartition(Partition.create()
                                // Partition regions by FIPS code
                                .region(regionId -> vaccinationRatePerDayFipsCodeValues.scope().getFipsSubCode(regionId))
                                // Partition by age group
                                .property(PersonProperty.AGE_GROUP_INDEX, ageGroupIndex -> ageGroups.get((int) ageGroupIndex))
                                // Partition by number of doses
                                .resource(VaccineId.VACCINE_ONE, numberOfDoses -> numberOfDoses),
                        VACCINE_PARTITION_KEY);

                // Schedule vaccine deliveries
                Map<Double, FipsCodeValues> vaccineDeliveries = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINE_DELIVERIES);
                for (Map.Entry<Double, FipsCodeValues> entry : vaccineDeliveries.entrySet()) {
                    environment.addPlan(new VaccineDeliveryPlan(entry.getValue()), entry.getKey());
                }

            }
        }

        @Override
        public void observeRegionPersonArrival(Environment environment, PersonId personId) {
            /*
             * A new person has been added to the simulation and we must have
             * already started vaccinating because we only start observing arrivals
             * after vaccination has begun
             */
            FipsCodeValues vaccinationRatePerDay = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINATION_RATE_PER_DAY);
            RegionId regionId = environment.getPersonRegion(personId);
            FipsCode fipsCode = vaccinationRatePerDay.scope().getFipsSubCode(regionId);
            final double vaccinationStartDay = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINATION_START_DAY);
            if (interVaccinationDelayDistribution.containsKey(fipsCode) &&
                    !environment.getPlan(fipsCode).isPresent() &&
                    environment.getTime() >= vaccinationStartDay) {
                toggleFipsCodePersonArrivalObservation(environment, fipsCode, false);
                vaccinateAndScheduleNext(environment, fipsCode);
            }
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            if (plan.getClass() == VaccinationPlan.class) {
                // VaccinationPlan means we should vaccinate a random person and schedule the next vaccination
                vaccinateAndScheduleNext(environment, ((VaccinationPlan) plan).fipsCode);
            } else if (plan.getClass() == VaccineDeliveryPlan.class) {
                FipsCodeValues dosesFipsCodeValues = ((VaccineDeliveryPlan) plan).doses;
                Map<FipsCode, Double> dosesByFipsCode = dosesFipsCodeValues.getFipsCodeValues(environment);
                FipsCodeValues vaccinationRatePerDay = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINATION_RATE_PER_DAY);
                final Set<FipsCode> fipsCodesToRestartVaccination = new HashSet<>();
                dosesByFipsCode.forEach(
                        (fipsCode, doubleNewDoses) -> {
                            long newDoses = Math.round(doubleNewDoses);
                            if (newDoses > 0) {
                                // Ignore finer scope for vaccine deliveries than the vaccination administration scope
                                if (!fipsCode.scope().hasBroaderScopeThan(vaccinationRatePerDay.scope())) {
                                    fipsCode = vaccinationRatePerDay.scope().getFipsSubCode(fipsCode);
                                }
                                long currentDoses = vaccineDeliveries.getOrDefault(fipsCode, 0L);
                                vaccineDeliveries.put(fipsCode, currentDoses + newDoses);
                                if (currentDoses == 0) {
                                    fipsCodesToRestartVaccination.add(fipsCode);
                                }
                            }
                        }
                );

                // Restart vaccination if needed
                final double vaccinationStartDay = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINATION_START_DAY);
                if (environment.getTime() >= vaccinationStartDay) {
                    FipsScope deliveryScope = dosesFipsCodeValues.scope().hasBroaderScopeThan(vaccinationRatePerDay.scope()) ?
                            dosesFipsCodeValues.scope() : vaccinationRatePerDay.scope();
                    interVaccinationDelayDistribution.keySet().forEach(
                            (fipsCode) -> {
                                if (!environment.getPlan(fipsCode).isPresent()) {
                                    if (fipsCodesToRestartVaccination.contains(deliveryScope.getFipsSubCode(fipsCode))) {
                                        vaccinateAndScheduleNext(environment, fipsCode);
                                    }
                                }
                            }
                    );
                }
            } else {
                throw new RuntimeException("Unhandled Vaccine Plan");
            }
        }

        private void vaccinateAndScheduleNext(Environment environment, FipsCode fipsCode) {
            // First check that vaccine is available in the FIPS hierarchy
            boolean hasResource = false;
            Optional<FipsCode> fipsCodeWithResource = Optional.of(fipsCode);
            while (fipsCodeWithResource.isPresent()) {
                long dosesInFipsCode = vaccineDeliveries.getOrDefault(fipsCodeWithResource.get(), 0L);
                if (dosesInFipsCode > 0) {
                    hasResource = true;
                    break;
                } else {
                    fipsCodeWithResource = fipsCodeWithResource.flatMap(FipsCode::getNextFipsCodeInHierarchy);
                }
            }

            if (hasResource) {
                // Get a random person to vaccinate, if possible, taking into account vaccine uptake weights
                AgeWeights vaccineUptakeWeights = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINE_UPTAKE_WEIGHTS);
                PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                        GlobalProperty.POPULATION_DESCRIPTION);
                List<AgeGroup> ageGroups = populationDescription.ageGroupPartition().ageGroupList();

                // Randomly select age group using the cumulative weights
                List<Pair<AgeGroup, Double>> ageGroupTargetWeights = ageGroups
                        .stream()
                        .map(ageGroup -> new Pair<>(ageGroup,
                                (double) environment.getPartitionSize(VACCINE_PARTITION_KEY, LabelSet.create()
                                        .region(fipsCode)
                                        .property(PersonProperty.AGE_GROUP_INDEX, ageGroup)
                                        // Be careful to use long and not int 0
                                        .resource(VaccineId.VACCINE_ONE, 0L)) *
                                        vaccineUptakeWeights.getWeight(ageGroup)))
                        .collect(Collectors.toList());
                // Check weights are not all zero and partitions are not all empty
                if (ageGroupTargetWeights.stream().anyMatch(x -> x.getSecond() > 0)) {
                    AgeGroup targetAgeGroup = new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(VaccineRandomId.ID),
                            ageGroupTargetWeights).sample();

                    // Randomly select age group using the cumulative weights
                    // We already know this index is nonempty
                    // noinspection OptionalGetWithoutIsPresent
                    final PersonId personId = environment.samplePartition(VACCINE_PARTITION_KEY, LabelSet.create()
                            .region(fipsCode)
                            .property(PersonProperty.AGE_GROUP_INDEX, targetAgeGroup)
                            // Be careful to use long and not int 0
                            .resource(VaccineId.VACCINE_ONE, 0L), VaccineRandomId.ID).get();

                    // Vaccinate the person, delivering vaccine to the appropriate region just in time
                    RegionId regionId = environment.getPersonRegion(personId);
                    long currentDoses = vaccineDeliveries.get(fipsCodeWithResource.get());
                    vaccineDeliveries.put(fipsCodeWithResource.get(), currentDoses - 1);
                    environment.addResourceToRegion(VaccineId.VACCINE_ONE, regionId, 1);
                    environment.transferResourceToPerson(VaccineId.VACCINE_ONE, personId, 1);

                    // Schedule next vaccination
                    final double vaccinationTime = environment.getTime() + interVaccinationDelayDistribution.get(fipsCode).sample();
                    environment.addPlan(new VaccinationPlan(fipsCode), vaccinationTime, fipsCode);
                } else {
                    // Nobody left to vaccinate for now, so register to observe new arrivals
                    toggleFipsCodePersonArrivalObservation(environment, fipsCode, true);
                }
            }
            // No vaccine available, so pause vaccinating for now and wait for vaccine delivery
        }

        private void toggleFipsCodePersonArrivalObservation(Environment environment, FipsCode fipsCode, boolean observe) {
            for (RegionId regionId : fipsCodeRegionMap.get(fipsCode)) {
                environment.observeRegionPersonArrival(observe, regionId);
            }
        }

        /*
            A plan to add vaccine to the simulation
         */
        private static class VaccineDeliveryPlan implements Plan {

            final FipsCodeValues doses;

            private VaccineDeliveryPlan(FipsCodeValues doses) {
                this.doses = doses;
            }

        }

        /*
            A plan to vaccinate a random person from the population
         */
        private static class VaccinationPlan implements Plan {

            private final FipsCode fipsCode;

            public VaccinationPlan(FipsCode fipsCode) {
                this.fipsCode = fipsCode;
            }

        }

    }

}
