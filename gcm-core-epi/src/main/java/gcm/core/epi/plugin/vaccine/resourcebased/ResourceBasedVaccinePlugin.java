package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.plugin.TriggerOverrideValidator;
import gcm.core.epi.plugin.behavior.TriggeredPropertyOverride;
import gcm.core.epi.plugin.vaccine.VaccinePlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.*;
import gcm.core.epi.trigger.AbsoluteTimeTrigger;
import gcm.core.epi.trigger.ImmutableAbsoluteTimeTrigger;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.util.property.*;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;
import gcm.simulation.partition.LabelSet;
import gcm.simulation.partition.Partition;
import gcm.simulation.partition.PartitionSampler;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResourceBasedVaccinePlugin implements VaccinePlugin {

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        Set<DefinedGlobalProperty> globalProperties = new HashSet<>();
        globalProperties.addAll(EnumSet.allOf(VaccineGlobalProperty.class));
        globalProperties.addAll(EnumSet.allOf(VaccineGlobalAndRegionProperty.class));
        return globalProperties;
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        return new HashSet<>(EnumSet.allOf(VaccineRegionProperty.class));
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
            return effectivenessFunction.getValue(relativeTime);
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

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        Map<DefinedGlobalAndRegionProperty, TriggerOverrideValidator> validators = new HashMap<>();
        TriggerOverrideValidator uptakeValidator = (env, triggerId, value) -> {
            FipsCodeDouble vaccinationRateFipsCodeValue = env.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINATION_RATE_PER_DAY);
            if (!triggerId.trigger().getClass().equals(ImmutableAbsoluteTimeTrigger.class)) {
                throw new RuntimeException("Vaccine uptake weight overrides only support absolute time triggers");
            }
            AbsoluteTimeTrigger trigger = (AbsoluteTimeTrigger) triggerId.trigger();
            if (!trigger.scope().hasBroaderScopeThan(vaccinationRateFipsCodeValue.scope())) {
                throw new RuntimeException("Vaccine uptake weight trigger can not have narrower scope than vaccination rate");
            }
        };
        validators.put(VaccineGlobalAndRegionProperty.VACCINE_UPTAKE_WEIGHTS, uptakeValidator);
        validators.put(VaccineGlobalAndRegionProperty.VACCINE_HIGH_RISK_UPTAKE_WEIGHTS, uptakeValidator);
        // Trigger property overrides
        List<TriggeredPropertyOverride> triggeredPropertyOverrides = environment.getGlobalPropertyValue(
                VaccineGlobalProperty.VACCINE_TRIGGER_OVERRIDES);
        Plugin.addTriggerOverrideCallbacks(triggerCallbacks, triggeredPropertyOverrides,
                Arrays.stream(VaccineGlobalAndRegionProperty.values()).collect(Collectors.toCollection(LinkedHashSet::new)),
                validators, environment);
        return triggerCallbacks;
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

        EFFECTIVENESS_FUNCTION(TypedPropertyDefinition.builder().type(EffectivenessFunction.class)
                .defaultValue(ImmutableEffectivenessFunction.builder().build()).build()),

        VACCINE_DELIVERIES(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<Double, FipsCodeDouble>>() {
                })
                .defaultValue(new HashMap<Double, FipsCodeDouble>()).build()),

        VACCINATION_START_DAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        VACCINATION_RATE_PER_DAY(TypedPropertyDefinition.builder()
                .type(FipsCodeDouble.class).defaultValue(ImmutableFipsCodeDouble.builder().build())
                .isMutable(false).build()),

        VACCINE_TRIGGER_OVERRIDES(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<List<TriggeredPropertyOverride>>() {
                })
                .defaultValue(new ArrayList<TriggeredPropertyOverride>())
                .isMutable(false).build());

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

    public enum VaccineGlobalAndRegionProperty implements DefinedGlobalAndRegionProperty {

        VACCINE_UPTAKE_WEIGHTS(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<FipsCodeValue<AgeWeights>>() {
                })
                .type(AgeWeights.class)
                .defaultValue(ImmutableFipsCodeValue.builder()
                        .defaultValue(ImmutableAgeWeights.builder().defaultValue(1.0).build())
                        .build())
                .build(),
                VaccineRegionProperty.VACCINE_UPTAKE_WEIGHTS),

        VACCINE_HIGH_RISK_UPTAKE_WEIGHTS(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<FipsCodeValue<AgeWeights>>() {
                })
                .type(AgeWeights.class)
                .defaultValue(ImmutableFipsCodeValue.builder()
                        .defaultValue(ImmutableAgeWeights.builder().defaultValue(1.0).build())
                        .build())
                .build(),
                VaccineRegionProperty.VACCINE_HIGH_RISK_UPTAKE_WEIGHTS);

        private final TypedPropertyDefinition propertyDefinition;
        private final DefinedRegionProperty regionProperty;

        VaccineGlobalAndRegionProperty(TypedPropertyDefinition propertyDefinition, DefinedRegionProperty regionProperty) {
            this.propertyDefinition = propertyDefinition;
            this.regionProperty = regionProperty;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

        @Override
        public DefinedRegionProperty getRegionProperty() {
            return regionProperty;
        }

    }

    public enum VaccineRegionProperty implements DefinedRegionProperty {

        VACCINE_UPTAKE_WEIGHTS(TypedPropertyDefinition.builder()
                .type(AgeWeights.class)
                .defaultValue(ImmutableAgeWeights.builder().defaultValue(1.0).build()).build()),

        VACCINE_HIGH_RISK_UPTAKE_WEIGHTS(TypedPropertyDefinition.builder()
                .type(AgeWeights.class)
                .defaultValue(ImmutableAgeWeights.builder().defaultValue(1.0).build()).build());

        private final TypedPropertyDefinition propertyDefinition;

        VaccineRegionProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    enum VaccineId implements ResourceId {

        VACCINE_ONE

    }

    enum VaccineProperty implements DefinedResourceProperty {
        // TODO: Determine how to use Global vs Resource properties
        VE_S(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        VE_I(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        VE_P(TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0)
                .isMutable(false).build()),

        EFFECTIVENESS_FUNCTION(TypedPropertyDefinition.builder().type(EffectivenessFunction.class)
                .defaultValue(ImmutableEffectivenessFunction.builder().build()).build());

        final TypedPropertyDefinition propertyDefinition;

        VaccineProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
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
            final FipsCodeDouble vaccinationRatePerDayFipsCodeValues = environment.getGlobalPropertyValue(
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
                environment.addPartition(Partition.builder()
                                // Partition regions by FIPS code
                                .setRegionFunction(regionId -> vaccinationRatePerDayFipsCodeValues.scope().getFipsSubCode(regionId))
                                // Partition by age group
                                .setPersonPropertyFunction(PersonProperty.AGE_GROUP_INDEX, ageGroupIndex -> ageGroups.get((int) ageGroupIndex))
                                // Partition by risk status
                                .setPersonPropertyFunction(PersonProperty.IS_HIGH_RISK, Function.identity())
                                // Partition by number of doses
                                .setPersonResourceFunction(VaccineId.VACCINE_ONE, numberOfDoses -> numberOfDoses)
                                .build(),
                        VACCINE_PARTITION_KEY);

                // Schedule vaccine deliveries
                Map<Double, FipsCodeDouble> vaccineDeliveries = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINE_DELIVERIES);
                for (Map.Entry<Double, FipsCodeDouble> entry : vaccineDeliveries.entrySet()) {
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
            FipsCodeDouble vaccinationRatePerDay = environment.getGlobalPropertyValue(
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
                FipsCodeDouble dosesFipsCodeValues = ((VaccineDeliveryPlan) plan).doses;
                Map<FipsCode, Double> dosesByFipsCode = dosesFipsCodeValues.getFipsCodeValues(environment);
                FipsCodeDouble vaccinationRatePerDay = environment.getGlobalPropertyValue(
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
                // Can use any region in the fips code as all will have the same value
                RegionId exemplarRegion = fipsCodeRegionMap.get(fipsCode).iterator().next();
                AgeWeights vaccineUptakeWeights = Plugin.getRegionalPropertyValue(environment,
                        exemplarRegion, VaccineGlobalAndRegionProperty.VACCINE_UPTAKE_WEIGHTS);
                AgeWeights vaccineHighRiskUptakeWeights = Plugin.getRegionalPropertyValue(environment,
                        exemplarRegion, VaccineGlobalAndRegionProperty.VACCINE_HIGH_RISK_UPTAKE_WEIGHTS);

                final Optional<PersonId> personId = environment.samplePartition(VACCINE_PARTITION_KEY, PartitionSampler.builder()
                        .setLabelSet(LabelSet.builder()
                                .setRegionLabel(fipsCode)
                                // Be careful to use long and not int 0
                                .setResourceLabel(VaccineId.VACCINE_ONE, 0L)
                                .build())
                        .setLabelSetWeightingFunction((observableEnvironment, labelSetInfo) -> {
                            // We know this labelSetInfo will have a label for this person property
                            //noinspection OptionalGetWithoutIsPresent
                            AgeGroup ageGroup = (AgeGroup) labelSetInfo.getPersonPropertyLabel(PersonProperty.AGE_GROUP_INDEX).get();
                            //noinspection OptionalGetWithoutIsPresent
                            boolean isHighRisk = (boolean) labelSetInfo.getPersonPropertyLabel(PersonProperty.IS_HIGH_RISK).get();
                            return vaccineUptakeWeights.getWeight(ageGroup) *
                                    (isHighRisk ? vaccineHighRiskUptakeWeights.getWeight(ageGroup) : 1.0);
                        })
                        .setRandomNumberGeneratorId(VaccineRandomId.ID)
                        .build());

                if (personId.isPresent()) {
                    // Vaccinate the person, delivering vaccine to the appropriate region just in time
                    RegionId regionId = environment.getPersonRegion(personId.get());
                    long currentDoses = vaccineDeliveries.get(fipsCodeWithResource.get());
                    vaccineDeliveries.put(fipsCodeWithResource.get(), currentDoses - 1);
                    environment.addResourceToRegion(VaccineId.VACCINE_ONE, regionId, 1);
                    environment.transferResourceToPerson(VaccineId.VACCINE_ONE, personId.get(), 1);

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

            final FipsCodeDouble doses;

            private VaccineDeliveryPlan(FipsCodeDouble doses) {
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
