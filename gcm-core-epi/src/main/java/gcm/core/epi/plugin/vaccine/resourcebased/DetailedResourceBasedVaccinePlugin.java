package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.plugin.behavior.TriggeredPropertyOverride;
import gcm.core.epi.plugin.vaccine.VaccinePlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import gcm.core.epi.propertytypes.FipsScope;
import gcm.core.epi.trigger.*;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.DefinedResourceProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.core.epi.variants.VariantId;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;
import gcm.simulation.partition.LabelSet;
import gcm.simulation.partition.Partition;
import gcm.simulation.partition.PartitionSampler;
import gcm.util.MultiKey;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DetailedResourceBasedVaccinePlugin implements VaccinePlugin {

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        Set<DefinedGlobalProperty> globalProperties = new HashSet<>(EnumSet.allOf(VaccineGlobalProperty.class));
        return globalProperties;
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(VaccineRandomId.ID);
        return randomIds;
    }

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>(EnumSet.allOf(VaccinePersonProperty.class));
    }

    @Override
    public double getVES(Environment environment, PersonId personId, VariantId variantId) {
        return getEfficacyFunctionValue(environment, personId, variantId, VaccineDefinition.EfficacyType.VE_S);
    }

    @Override
    public double getVEI(Environment environment, PersonId personId, VariantId variantId) {
        return getEfficacyFunctionValue(environment, personId, variantId, VaccineDefinition.EfficacyType.VE_I);
    }

    @Override
    public double getVEP(Environment environment, PersonId personId, VariantId variantId) {
        return getEfficacyFunctionValue(environment, personId, variantId, VaccineDefinition.EfficacyType.VE_P);
    }

    private double getEfficacyFunctionValue(Environment environment, PersonId personId, VariantId variantId,
                                            VaccineDefinition.EfficacyType efficacyType) {
        long numberOfDoses = environment.getPersonResourceLevel(personId, VaccineResourceId.VACCINE);
        if (numberOfDoses > 0) {
            List<VaccineDefinition> vaccineDefinitions = environment.getGlobalPropertyValue(VaccineGlobalProperty.VACCINE_DEFINITIONS);
            int vaccineIndex = environment.getPersonPropertyValue(personId, VaccinePersonProperty.VACCINE_INDEX);
            if (vaccineIndex < 0 || vaccineIndex > vaccineDefinitions.size()) {
                throw new RuntimeException("Invalid vaccine index");
            }
            VaccineDefinition vaccineDefinition = vaccineDefinitions.get(vaccineIndex);
            double vaccinationTime = environment.getPersonResourceTime(personId, VaccineResourceId.VACCINE);
            double relativeTime = environment.getTime() - vaccinationTime;
            return vaccineDefinition.getVaccineEfficacy(numberOfDoses, relativeTime, variantId, efficacyType);
        } else {
            return 0.0;
        }
    }

    @Override
    public Map<ResourceId, Set<DefinedResourceProperty>> getResourcesAndProperties() {
        Map<ResourceId, Set<DefinedResourceProperty>> resourcePropertyMap =
                new HashMap<>();
        // Vaccine has no properties
        resourcePropertyMap.put(VaccineResourceId.VACCINE, new HashSet<>());
        return resourcePropertyMap;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        VaccinePlugin.super.load(experimentBuilder);
        // Track when last dose of vaccine was received
        experimentBuilder.setResourceTimeTracking(VaccineResourceId.VACCINE, TimeTrackingPolicy.TRACK_TIME);
        experimentBuilder.addGlobalComponentId(VACCINE_MANAGER_IDENTIFIER, VaccineManager.class);
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        // The following code is very similar to the Plugin overrides but has been modified to only use a single Global Property
        List<TriggeredPropertyOverride> triggeredPropertyOverrides = environment.getGlobalPropertyValue(
                VaccineGlobalProperty.VACCINE_TRIGGER_OVERRIDES);
        for (TriggeredPropertyOverride override : triggeredPropertyOverrides) {
            String triggerStringId = override.trigger();
            // Get the trigger for validation
            TriggerContainer triggerContainer = environment.getGlobalPropertyValue(GlobalProperty.TRIGGER_CONTAINER);
            TriggerId<Trigger> triggerId = triggerContainer.getId(triggerStringId);
            if (triggerId == null) {
                throw new RuntimeException("Override trigger id is invalid: " + triggerStringId);
            }
            if (!triggerId.trigger().getClass().equals(ImmutableAbsoluteTimeTrigger.class)) {
                throw new RuntimeException("Detailed resource-based vaccine overrides only support absolute time triggers");
            }
            AbsoluteTimeTrigger trigger = (AbsoluteTimeTrigger) triggerId.trigger();
            if (!(trigger.scope() == FipsScope.NATION)) {
                throw new RuntimeException("Detailed resource-based vaccine overrides can only be NATIONAL scope");
            }
            // Parse the values and setup callbacks
            PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
            AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
            Map<String, JsonNode> propertyOverrides = override.overrides();
            propertyOverrides.forEach(
                    (propertyIdString, valueJson) -> {
                        final VaccineGlobalProperty overrideProperty;
                        if (propertyIdString.equals(VaccineGlobalProperty.VACCINE_ADMINISTRATOR_SETTINGS.toString())) {
                            overrideProperty = VaccineGlobalProperty.VACCINE_ADMINISTRATOR_SETTINGS;
                        } else if (propertyIdString.equals(VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION.toString())) {
                            overrideProperty = VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION_OVERRIDES;
                        } else {
                            throw new RuntimeException("Unhandled override property");
                        }
                        try {
                            Object overrideValue = CoreEpiBootstrapUtil.getPropertyValueFromJson(valueJson,
                                    overrideProperty.getPropertyDefinition().overrideJavaType(), ageGroupPartition);
                            TriggerUtils.addCallback(triggerCallbacks, triggerStringId, (env, regionId) -> {
                                // Only set global property once for each callback by checking current value
                                Object currentValue = environment.getGlobalPropertyValue(overrideProperty);
                                if (!overrideValue.equals(currentValue)) {
                                    environment.setGlobalPropertyValue(overrideProperty, overrideValue);
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException("Property override value cannot be parsed from: " + valueJson);
                        }
                    }
            );
        }
        return triggerCallbacks;
    }

    public enum VaccinePersonProperty implements DefinedPersonProperty {

        VACCINE_INDEX(TypedPropertyDefinition.builder()
                .type(Integer.class).defaultValue(-1).build());

        private final TypedPropertyDefinition propertyDefinition;

        VaccinePersonProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    enum VaccineResourceId implements ResourceId {

        VACCINE

    }

    /*
        The global properties added to the simulation by this plugin
     */
    public enum VaccineGlobalProperty implements DefinedGlobalProperty {

        VACCINE_DEFINITIONS(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<List<VaccineDefinition>>() {
                })
                .defaultValue(new ArrayList<>())
                .isMutable(false).build()),

        VACCINE_ADMINISTRATORS(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<List<VaccineAdministratorDefinition>>() {
                })
                .defaultValue(new ArrayList<>())
                .build()),

        VACCINE_ADMINISTRATOR_ALLOCATION(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<VaccineId, Map<VaccineAdministratorId, Double>>>() {
                })
                .defaultValue(new HashMap<>()).build()),

        VACCINE_DELIVERIES(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<Double, Map<VaccineId, FipsCodeDouble>>>() {
                })
                .defaultValue(new HashMap<Double, Map<VaccineId, FipsCodeDouble>>())
                .isMutable(false).build()),

        VACCINATION_START_DAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        // Used for overriding properties only
        VACCINE_ADMINISTRATOR_SETTINGS(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<VaccineAdministratorId, VaccineAdministratorDefinitionOverride>>() {
                })
                .defaultValue(new HashMap<>())
                .build(),
                false),

        // Used for overriding properties only
        VACCINE_ADMINISTRATOR_ALLOCATION_OVERRIDES(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<VaccineId, Map<VaccineAdministratorId, Double>>>() {
                })
                .defaultValue(new HashMap<>())
                .build(),
                false),

        VACCINE_TRIGGER_OVERRIDES(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<List<TriggeredPropertyOverride>>() {
                })
                .defaultValue(new ArrayList<TriggeredPropertyOverride>())
                .isMutable(false).build()),

        VACCINATION_FIPS_CODE_RESTART_TIME(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<FipsCode, Double>>() {
                })
                .defaultValue(new LinkedHashMap<>())
                .build(),
                false),

        MOST_RECENT_VACCINATION_DATA(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Optional<DetailedResourceVaccinationData>>() {
                })
                .defaultValue(Optional.empty())
                .build(),
                false);

        private final TypedPropertyDefinition propertyDefinition;
        private final boolean isExternalProperty;

        VaccineGlobalProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
            isExternalProperty = true;
        }

        VaccineGlobalProperty(TypedPropertyDefinition propertyDefinition, boolean isExternalProperty) {
            this.propertyDefinition = propertyDefinition;
            this.isExternalProperty = isExternalProperty;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return isExternalProperty;
        }
    }

    public static class VaccineManager extends AbstractComponent {

        // Key for vaccine partition
        private static final Object VACCINE_PARTITION_KEY = new Object();
        // Vaccines ready to be distributed
        private final Map<VaccineAdministratorId, Map<VaccineId, VaccineDoseFipsContainer>> vaccineDeliveries = new HashMap<>();
        // Random distributions for vaccine delays
        private final Map<VaccineAdministratorId, Map<FipsCode, RealDistribution>> interVaccinationDelayDistributions = new LinkedHashMap<>();
        // Map holding prioritized people for second doses by VaccineAdministrator
        private final Map<VaccineAdministratorId, Map<VaccineId, ArrayDeque<PersonId>>> secondDosePriorityMap = new HashMap<>();
        // Map indicating the set of FipsCodes in what at the moment there are no more people to whom to give first doses
        private final Map<VaccineAdministratorId, Set<FipsCode>> fipsCodesWithNoFirstDosesPeople = new HashMap<>();
        // Map for VaccineAdministrators by id
        private final Map<VaccineAdministratorId, VaccineAdministratorDefinition> vaccineAdministratorMap = new LinkedHashMap<>();
        // Map for Vaccines by id
        private final Map<VaccineId, VaccineDefinition> vaccineDefinitionMap = new HashMap<>();
        // Map from Vaccine ID to index in the VACCINE_DEFINITION global property
        private final Map<VaccineId, Integer> vaccineIndexMap = new HashMap<>();
        // Map giving regions in a given FipsCode - note all VaccineAdministrators must share a scope
        private Map<FipsCode, Set<RegionId>> fipsCodeRegionMap;
        private FipsScope administrationScope;
        // Flag for seeing if we are already observing region arrivals
        private Map<FipsCode, Boolean> alreadyObservingRegionArrivals = new HashMap<>();

        @Override
        public void init(Environment environment) {
            // Store vaccine administrators by ID
            List<VaccineAdministratorDefinition> vaccineAdministratorDefinitions = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINE_ADMINISTRATORS);
            vaccineAdministratorDefinitions.forEach(
                    vaccineAdministratorDefinition -> vaccineAdministratorMap.put(vaccineAdministratorDefinition.id(),
                            vaccineAdministratorDefinition)
            );

            // Store vaccine definitions by ID and add storage for each administrator
            List<VaccineDefinition> vaccineDefinitions = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINE_DEFINITIONS);
            AtomicInteger indexCounter = new AtomicInteger(0);
            vaccineDefinitions.forEach(
                    vaccineDefinition -> {
                        vaccineDefinitionMap.put(vaccineDefinition.id(), vaccineDefinition);
                        vaccineIndexMap.put(vaccineDefinition.id(), indexCounter.getAndIncrement());
                        vaccineAdministratorDefinitions.forEach(
                                vaccineAdministratorDefinition -> {
                                    Map<VaccineId, VaccineDoseFipsContainer> vaccineDoseFipsContainerMap =
                                            vaccineDeliveries.computeIfAbsent(vaccineAdministratorDefinition.id(),
                                                    x -> new HashMap<>());
                                    vaccineDoseFipsContainerMap.put(vaccineDefinition.id(), new VaccineDoseFipsContainer());
                                    Map<VaccineId, ArrayDeque<PersonId>> secondDosePriorityMapForAdministrator =
                                            secondDosePriorityMap.computeIfAbsent(vaccineAdministratorDefinition.id(),
                                                    x -> new HashMap<>());
                                    secondDosePriorityMapForAdministrator.put(vaccineDefinition.id(), new ArrayDeque<>());
                                }
                        );
                    }
            );

            // Determine if we are vaccinating and if so set up plans and distributions
            final double vaccinationStartDay = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINATION_START_DAY);
            final List<TriggeredPropertyOverride> overrideList = environment.getGlobalPropertyValue(
                    VaccineGlobalProperty.VACCINE_TRIGGER_OVERRIDES);
            // Presume if we have overrides we will be vaccinating at some point
            boolean usingVaccine = overrideList.size() > 0;

            for (VaccineAdministratorDefinition vaccineAdministratorDefinition : vaccineAdministratorDefinitions) {
                Map<FipsCode, Double> vaccinationRatePerDayByFipsCode = vaccineAdministratorDefinition
                        .vaccinationRatePerDay().getFipsCodeValues(environment);

                Map<FipsCode, RealDistribution> interVaccinationDelayDistributionForAdministrator = new HashMap<>();
                for (Map.Entry<FipsCode, Double> entry : vaccinationRatePerDayByFipsCode.entrySet()) {
                    FipsCode fipsCode = entry.getKey();
                    double vaccinationRatePerDay = entry.getValue();
                    if (vaccinationRatePerDay > 0) {
                        usingVaccine = true;
                        // Make distribution for inter-vaccination time delays
                        final RandomGenerator randomGenerator = environment.getRandomGeneratorFromId(VaccineRandomId.ID);
                        interVaccinationDelayDistributionForAdministrator.put(fipsCode, new ExponentialDistribution(randomGenerator,
                                1 / vaccinationRatePerDay));

                        // Schedule first vaccination event
                        environment.addPlan(new VaccinationPlan(vaccineAdministratorDefinition.id(), fipsCode), vaccinationStartDay,
                                new MultiKey(vaccineAdministratorDefinition.id(), fipsCode));
                    }
                }
                interVaccinationDelayDistributions.put(vaccineAdministratorDefinition.id(), interVaccinationDelayDistributionForAdministrator);
            }

            if (usingVaccine) {

                // Check all administrators have the same scope
                List<FipsScope> scopes = vaccineAdministratorDefinitions.stream()
                        .map(x -> x.vaccinationRatePerDay().scope())
                        .distinct()
                        .collect(Collectors.toList());
                if (scopes.size() > 1) {
                    throw new RuntimeException("All vaccine administrators must share the same scope");
                }
                administrationScope = scopes.get(0);
                fipsCodeRegionMap = administrationScope.getFipsCodeRegionMap(environment);

                // Set up population partition
                PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                        GlobalProperty.POPULATION_DESCRIPTION);
                List<AgeGroup> ageGroups = populationDescription.ageGroupPartition().ageGroupList();
                environment.addPartition(Partition.builder()
                                // Partition regions by FIPS code
                                .setRegionFunction(regionId -> administrationScope.getFipsSubCode(regionId))
                                // Partition by age group
                                .setPersonPropertyFunction(PersonProperty.AGE_GROUP_INDEX, ageGroupIndex -> ageGroups.get((int) ageGroupIndex))
                                // Partition by risk status
                                .setPersonPropertyFunction(PersonProperty.IS_HIGH_RISK, Function.identity())
                                // Partition by number of doses
                                .setPersonResourceFunction(VaccineResourceId.VACCINE, numberOfDoses -> numberOfDoses)
                                .build(),
                        VACCINE_PARTITION_KEY);

                // Normalize vaccine administrator allocation
                Map<VaccineId, Map<VaccineAdministratorId, Double>> vaccineAdministratorAllocation =
                        environment.getGlobalPropertyValue(VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION);
                for (VaccineId vaccineId : vaccineDefinitionMap.keySet()) {
                    Map<VaccineAdministratorId, Double> vaccineAllocation = vaccineAdministratorAllocation.computeIfAbsent(
                            vaccineId, x -> new LinkedHashMap<>());
                    // If no allocation is given assume equal weighting
                    if (vaccineAllocation.isEmpty()) {
                        vaccineAdministratorDefinitions.forEach(vaccineAdministratorDefinition ->
                                vaccineAllocation.put(vaccineAdministratorDefinition.id(), 1.0));
                    }
                    final double vaccineAllocationTotal = vaccineAllocation.values().stream()
                            .mapToDouble(x -> x).sum();
                    vaccineAllocation.replaceAll(
                            (i, v) -> vaccineAllocation.get(i) / vaccineAllocationTotal);
                }
                environment.setGlobalPropertyValue(VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION,
                        vaccineAdministratorAllocation);


                // Schedule vaccine deliveries
                Map<Double, Map<VaccineId, FipsCodeDouble>> vaccineDeliveries = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINE_DELIVERIES);
                for (Map.Entry<Double, Map<VaccineId, FipsCodeDouble>> entry : vaccineDeliveries.entrySet()) {
                        environment.addPlan(new VaccineDeliveryPlan(entry.getValue()), entry.getKey());
                }

                // Register to observe changes for triggered property overrides that may restart vaccination
                environment.observeGlobalPropertyChange(true,
                        VaccineGlobalProperty.VACCINE_ADMINISTRATOR_SETTINGS);
                environment.observeGlobalPropertyChange(true,
                        VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION_OVERRIDES);
            }

        }

        @Override
        public void observeGlobalPropertyChange(Environment environment, GlobalPropertyId globalPropertyId) {
            if (globalPropertyId.equals(VaccineGlobalProperty.VACCINE_ADMINISTRATOR_SETTINGS)) {
                Map<VaccineAdministratorId, VaccineAdministratorDefinitionOverride> overrides =
                        environment.getGlobalPropertyValue(globalPropertyId);
                // Track changes that require reconsideration of vaccination plans
                Set<VaccineAdministratorId> administratorsRateChanged = new LinkedHashSet<>();
                Set<VaccineAdministratorId> administratorsUptakeChanged = new LinkedHashSet<>();
                // First recompute the vaccine administrator definitions
                for (VaccineAdministratorId vaccineAdministratorId : vaccineAdministratorMap.keySet()) {
                    if (overrides.containsKey(vaccineAdministratorId)) {
                        VaccineAdministratorDefinitionOverride definitionOverride = overrides.get(vaccineAdministratorId);
                        ImmutableVaccineAdministratorDefinition.Builder builder = ImmutableVaccineAdministratorDefinition.builder();
                        builder.from(vaccineAdministratorMap.get(vaccineAdministratorId));
                        if (definitionOverride.vaccinationRatePerDay() != null) {
                            if (definitionOverride.vaccinationRatePerDay().scope() !=
                                    vaccineAdministratorMap.get(vaccineAdministratorId).vaccinationRatePerDay().scope()) {
                                throw new RuntimeException("Cannot change scope of vaccine administration in override");
                            }
                            builder.vaccinationRatePerDay(definitionOverride.vaccinationRatePerDay());
                            // Requires reconsideration of vaccine administration rate
                            administratorsRateChanged.add(vaccineAdministratorId);
                        }
                        if (definitionOverride.vaccineUptakeWeights() != null) {
                            builder.vaccineUptakeWeights(definitionOverride.vaccineUptakeWeights());
                            administratorsUptakeChanged.add(vaccineAdministratorId);
                        }
                        if (definitionOverride.vaccineHighRiskUptakeWeights() != null) {
                            builder.vaccineHighRiskUptakeWeights(definitionOverride.vaccineHighRiskUptakeWeights());
                            administratorsUptakeChanged.add(vaccineAdministratorId);
                        }
                        if (definitionOverride.fractionReturnForSecondDose() != null) {
                            builder.fractionReturnForSecondDose(definitionOverride.fractionReturnForSecondDose());
                            // Does not require reconsideration of vaccination plan
                        }
                        vaccineAdministratorMap.put(vaccineAdministratorId, builder.build());
                    }
                }

                // Recompute vaccination rates and stop/restart vaccination events
                final double vaccinationStartDay = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINATION_START_DAY);
                for (VaccineAdministratorId vaccineAdministratorId : administratorsRateChanged) {
                    // Recompute administration rates from fips code values object
                    VaccineAdministratorDefinition vaccineAdministratorDefinition = vaccineAdministratorMap.get(vaccineAdministratorId);
                    Map<FipsCode, Double> vaccinationRatePerDayByFipsCode = vaccineAdministratorDefinition
                            .vaccinationRatePerDay().getFipsCodeValues(environment);
                    // Recompute sampling distributions for vaccine administration
                    Map<FipsCode, RealDistribution> interVaccinationDelayDistributionForAdministrator = new HashMap<>();
                    for (Map.Entry<FipsCode, Double> entry : vaccinationRatePerDayByFipsCode.entrySet()) {
                        FipsCode fipsCode = entry.getKey();
                        Object vaccinationPlanKey = new MultiKey(vaccineAdministratorId, fipsCode);
                        environment.removePlan(vaccinationPlanKey);
                        double vaccinationRatePerDay = entry.getValue();
                        if (vaccinationRatePerDay > 0) {
                            // Make distribution for inter-vaccination time delays
                            final RandomGenerator randomGenerator = environment.getRandomGeneratorFromId(VaccineRandomId.ID);
                            RealDistribution interVaccinationDelayDistribution = new ExponentialDistribution(randomGenerator,
                                    1 / vaccinationRatePerDay);
                            interVaccinationDelayDistributionForAdministrator.put(fipsCode, interVaccinationDelayDistribution);

                            // Reschedule vaccination event
                            environment.addPlan(new VaccinationPlan(vaccineAdministratorDefinition.id(), fipsCode),
                                    environment.getTime() + interVaccinationDelayDistribution.sample(),
                                    vaccinationPlanKey);
                        }
                    }
                    interVaccinationDelayDistributions.put(vaccineAdministratorId, interVaccinationDelayDistributionForAdministrator);
                }

                // Restart vaccination if needed due to uptake changes
                if (environment.getTime() >= vaccinationStartDay) {
                    for (VaccineAdministratorId vaccineAdministratorId : administratorsUptakeChanged) {
                        fipsCodesWithNoFirstDosesPeople.put(vaccineAdministratorId, new HashSet<>());
                        // Skip those whose rate changed as we have already reconsidered all vaccination events
                        if (!administratorsRateChanged.contains(vaccineAdministratorId)) {
                            for (FipsCode fipsCode : fipsCodeRegionMap.keySet()) {
                                if (!environment.getPlan(new MultiKey(vaccineAdministratorId, fipsCode)).isPresent()) {
                                    vaccinateAndScheduleNext(environment, vaccineAdministratorId, fipsCode);
                                }
                            }
                        }
                    }
                }

            } else if (globalPropertyId.equals(VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION_OVERRIDES)) {
                // Normalize and update allocation
                Map<VaccineId, Map<VaccineAdministratorId, Double>> allocation = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION);
                Map<VaccineId, Map<VaccineAdministratorId, Double>> allocationOverrides = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION_OVERRIDES);
                allocationOverrides.forEach(
                        (vaccineId, vaccineAllocation) -> {
                            final double vaccineAllocationTotal = vaccineAllocation.values().stream()
                                    .mapToDouble(x -> x).sum();
                            vaccineAllocation.replaceAll(
                                    (i, v) -> vaccineAllocation.get(i) / vaccineAllocationTotal);
                            allocation.put(vaccineId, vaccineAllocation);
                        }
                );
                environment.setGlobalPropertyValue(VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION, allocation);
            } else {
                throw new RuntimeException("VaccineManager observed unexpected global property change");
            }
        }

        @Override
        public void observeRegionPersonArrival(Environment environment, PersonId personId) {
            /*
             * A new person has been added to the region and we must have
             * already started vaccinating because we only start observing arrivals
             * after vaccination has begun
             */
            RegionId regionId = environment.getPersonRegion(personId);
            FipsCode fipsCode = administrationScope.getFipsSubCode(regionId);

            // See if any of the vaccine administrators need to be restarted
            for (Map.Entry<VaccineAdministratorId, Map<FipsCode, RealDistribution>> entry :
                    interVaccinationDelayDistributions.entrySet()) {
                if (entry.getValue().containsKey(fipsCode) &&
                        !environment.getPlan(new MultiKey(entry.getKey(), fipsCode)).isPresent()) {
                    fipsCodesWithNoFirstDosesPeople.get(entry.getKey()).remove(fipsCode);
                    vaccinateAndScheduleNext(environment, entry.getKey(), fipsCode);
                }
            }
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            if (plan.getClass() == VaccinationPlan.class) {
                // VaccinationPlan means we should vaccinate a random person and schedule the next vaccination
                VaccinationPlan vaccinationPlan = ((VaccinationPlan) plan);
                vaccinateAndScheduleNext(environment, vaccinationPlan.vaccineAdministratorId, vaccinationPlan.fipsCode);
            } else if (plan.getClass() == VaccineDeliveryPlan.class) {
                Map<VaccineId, FipsCodeDouble> dosesFipsCodeValues = ((VaccineDeliveryPlan) plan).doses;
                Map<VaccineId, Map<VaccineAdministratorId, Double>> vaccineAdministratorAllocation = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINE_ADMINISTRATOR_ALLOCATION);
                Set<VaccineAdministratorId> vaccineAdministratorsToRestart = new LinkedHashSet<>();
                dosesFipsCodeValues.forEach(
                        (vaccineId, fipsCodeDouble) -> {
                            Map<FipsCode, Double> dosesByFipsCode = fipsCodeDouble.getFipsCodeValues(environment);
                            Map<VaccineAdministratorId, Double> administratorAllocation = vaccineAdministratorAllocation.get(vaccineId);
                            dosesByFipsCode.forEach(
                                    (fipsCode, doubleNewDoses) -> {
                                        for (VaccineAdministratorId vaccineAdministratorId : vaccineAdministratorMap.keySet()) {
                                            long newDoses = Math.round(doubleNewDoses * administratorAllocation
                                                    .getOrDefault(vaccineAdministratorId, 0.0));
                                            if (newDoses > 0) {
                                                // Ignore finer scope for vaccine deliveries than the vaccination administration scope
                                                if (!fipsCode.scope().hasBroaderScopeThan(administrationScope)) {
                                                    fipsCode = administrationScope.getFipsSubCode(fipsCode);
                                                }
                                                VaccineDoseFipsContainer vaccineDoseFipsContainer = vaccineDeliveries
                                                        .get(vaccineAdministratorId).get(vaccineId);
                                                // Check if this delivery could trigger vaccination to restart
                                                if (vaccineDoseFipsContainer.getDosesAvailableTo(fipsCode) == 0L) {
                                                    vaccineAdministratorsToRestart.add(vaccineAdministratorId);
                                                }
                                                // Deliver doses
                                                vaccineDeliveries.get(vaccineAdministratorId).get(vaccineId)
                                                        .deliverDosesTo(fipsCode, newDoses);
                                            }
                                        }
                                    }
                            );
                        }
                );

                // Restart vaccination if needed
                final double vaccinationStartDay = environment.getGlobalPropertyValue(
                        VaccineGlobalProperty.VACCINATION_START_DAY);
                if (environment.getTime() >= vaccinationStartDay) {
                    vaccineAdministratorsToRestart.forEach(
                            (vaccineAdministratorId) -> {
                                Map<FipsCode, RealDistribution> interVaccinationDelayDistribution =
                                        interVaccinationDelayDistributions.get(vaccineAdministratorId);
                                for (FipsCode fipsCode : fipsCodeRegionMap.keySet()) {
                                    // Make sure rate of vaccination is nonzero and not already planned
                                    if (interVaccinationDelayDistribution.containsKey(fipsCode) &&
                                            !environment.getPlan(new MultiKey(vaccineAdministratorId, fipsCode)).isPresent()) {
                                        vaccinateAndScheduleNext(environment, vaccineAdministratorId, fipsCode);
                                    }
                                }
                            }
                    );
                }
            } else if (plan.getClass() == SecondDoseQueuePlan.class) {
                SecondDoseQueuePlan secondDoseQueuePlan = (SecondDoseQueuePlan) plan;
                VaccineAdministratorId vaccineAdministratorId = secondDoseQueuePlan.vaccineAdministratorId;
                secondDosePriorityMap.get(vaccineAdministratorId).get(secondDoseQueuePlan.vaccineId)
                        .addLast(secondDoseQueuePlan.personId);
                // Restart vaccination if needed
                RegionId regionId = environment.getPersonRegion(secondDoseQueuePlan.personId);
                FipsCode fipsCode = administrationScope.getFipsSubCode(regionId);
                MultiKey multiKey = new MultiKey(vaccineAdministratorId, fipsCode);
                if (!environment.getPlan(multiKey).isPresent()) {
                    vaccinateAndScheduleNext(environment, vaccineAdministratorId, fipsCode);
                }
            } else {
                throw new RuntimeException("Unhandled Vaccine Plan");
            }
        }

        private void vaccinateAndScheduleNext(Environment environment, VaccineAdministratorId vaccineAdministratorId, FipsCode fipsCode) {
            // First check that at least some vaccine is available in the FIPS hierarchy
            // We will ignore vaccines for which there is nobody to use that vaccine
            final AtomicBoolean someoneToGiveFirstDosesTo = new AtomicBoolean(!fipsCodesWithNoFirstDosesPeople
                    .computeIfAbsent(vaccineAdministratorId, x -> new HashSet<>())
                    .contains(fipsCode));
            Map<VaccineId, Boolean> someoneToGiveSecondDosesTo = vaccineDefinitionMap.keySet().stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            vaccineId ->
                                    !secondDosePriorityMap
                                            .computeIfAbsent(vaccineAdministratorId, x -> new HashMap<>())
                                            .computeIfAbsent(vaccineId, x -> new ArrayDeque<>())
                                            .isEmpty()

                    ));
            Map<VaccineId, VaccineDoseFipsContainer> vaccineDeliveriesForAdministrator = vaccineDeliveries
                    .computeIfAbsent(vaccineAdministratorId, x -> new LinkedHashMap<>());
            Map<VaccineId, Double> regimensByType = vaccineDeliveriesForAdministrator.entrySet().stream()
                    // Skip if one dose and nobody to vaccinate or two dose and nobody for either first or second dose
                    .filter(entry -> someoneToGiveFirstDosesTo.get() || someoneToGiveSecondDosesTo.get(entry.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> (double) entry.getValue().getDosesAvailableTo(fipsCode) /
                                    vaccineDefinitionMap.get(entry.getKey()).dosesPerRegimen(),
                            (key1, key2) -> {
                                throw new RuntimeException("Duplicate keys in threshold map");
                            },
                            // Force map ordering
                            LinkedHashMap::new)
                    );
            final boolean hasResource = regimensByType.values().stream().anyMatch(x -> x > 0.0);

            if (hasResource) {
                VaccineAdministratorDefinition vaccineAdministratorDefinition = vaccineAdministratorMap.get(vaccineAdministratorId);
                // Get a random person to vaccinate, if possible, taking into account vaccine uptake weights
                AgeWeights vaccineUptakeWeights = vaccineAdministratorDefinition.vaccineUptakeWeights();
                AgeWeights vaccineHighRiskUptakeWeights = vaccineAdministratorDefinition.vaccineHighRiskUptakeWeights();

                // First select which type of vaccine to use
                List<Pair<VaccineId, Double>> regimensByTypeForSampling = regimensByType.entrySet().stream()
                        .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
                VaccineId vaccineId = new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(VaccineRandomId.ID),
                        regimensByTypeForSampling).sample();
                VaccineDefinition vaccineDefinition = vaccineDefinitionMap.get(vaccineId);

                // Next select the person to vaccinate
                final Optional<PersonId> personId;
                // If this is a two-dose vaccine need to choose between vaccinating a new person and giving the second dose
                //  We randomly proportionally allocate effort taking into account the fraction that will return for a second dose
                if (!someoneToGiveFirstDosesTo.get() || (vaccineDefinition.type() == VaccineDefinition.DoseType.TWO_DOSE &&
                        (environment.getRandomGeneratorFromId(VaccineRandomId.ID).nextDouble() <
                                (vaccineAdministratorDefinition.fractionReturnForSecondDose() /
                                        (1.0 + vaccineAdministratorDefinition.fractionReturnForSecondDose()))))) {
                    // Vaccinate a person with a second dose if needed
                    ArrayDeque<PersonId> secondDosePriority = secondDosePriorityMap.get(vaccineAdministratorId).get(vaccineId);
                    if (secondDosePriority.isEmpty()) {
                        personId = Optional.empty();
                    } else {
                        personId = Optional.of(secondDosePriority.removeFirst());
                    }
                } else {
                    // Check to make sure not all of the available doses are in reserve
                    double regimensInReserve = (double) vaccineDeliveriesForAdministrator.get(vaccineId)
                            .getReservedDosesFor(fipsCode) / vaccineDefinition.dosesPerRegimen();
                    if (regimensByType.get(vaccineId) > regimensInReserve) {
                        // Need to select a person to give the new dose to
                        personId = environment.samplePartition(VACCINE_PARTITION_KEY, PartitionSampler.builder()
                                .setLabelSet(LabelSet.builder()
                                        .setRegionLabel(fipsCode)
                                        // Be careful to use long and not int 0
                                        .setResourceLabel(VaccineResourceId.VACCINE, 0L)
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
                        if (!personId.isPresent()) {
                            // Note that there are no more people to whom to give first doses
                            fipsCodesWithNoFirstDosesPeople.get(vaccineAdministratorId).add(fipsCode);
                            someoneToGiveFirstDosesTo.set(false);
                        }
                    } else {
                        personId = Optional.empty();
                    }
                }

                if (personId.isPresent()) {
                    // Vaccinate the person, delivering vaccine to the appropriate region just in time
                    RegionId regionId = environment.getPersonRegion(personId.get());
                    boolean isSecondDose = environment.getPersonResourceLevel(personId.get(),
                            VaccineResourceId.VACCINE) > 0L;
                    boolean useReserve = vaccineAdministratorDefinition.reserveSecondDoses() && isSecondDose;
                    VaccineDoseFipsContainer vaccineDoseFipsContainer = vaccineDeliveries
                            .get(vaccineAdministratorId).get(vaccineId);
                    vaccineDoseFipsContainer.removeDoseFrom(fipsCode, useReserve);
                    environment.addResourceToRegion(VaccineResourceId.VACCINE, regionId, 1);
                    environment.transferResourceToPerson(VaccineResourceId.VACCINE, personId.get(), 1);
                    // Reporting data
                    environment.setGlobalPropertyValue(VaccineGlobalProperty.MOST_RECENT_VACCINATION_DATA,
                            Optional.of(
                                    ImmutableDetailedResourceVaccinationData.builder()
                                            .regionId(regionId)
                                            .vaccineAdministratorId(vaccineAdministratorId)
                                            .vaccineId(vaccineId)
                                            .doseType(isSecondDose ? DetailedResourceVaccinationData.DoseType.SECOND_DOSE :
                                                    DetailedResourceVaccinationData.DoseType.FIRST_DOSE)
                                            .build()
                            ));
                    if (!isSecondDose) {
                        environment.setPersonPropertyValue(personId.get(), VaccinePersonProperty.VACCINE_INDEX,
                                vaccineIndexMap.get(vaccineId));
                        // Determine if person will return for second dose and schedule
                        if (vaccineDefinition.type() == VaccineDefinition.DoseType.TWO_DOSE &&
                                (environment.getRandomGeneratorFromId(VaccineRandomId.ID).nextDouble() <
                                        vaccineAdministratorDefinition.fractionReturnForSecondDose())) {
                            double secondDoseDelay = vaccineDefinition.secondDoseDelay();
                            if (secondDoseDelay > 0) {
                                environment.addPlan(new SecondDoseQueuePlan(vaccineAdministratorId, personId.get(), vaccineId),
                                        environment.getTime() + secondDoseDelay);
                            } else {
                                secondDosePriorityMap.get(vaccineAdministratorId).get(vaccineId).addLast(personId.get());
                            }
                            // Reserve second doses if applicable
                            if (vaccineAdministratorDefinition.reserveSecondDoses()) {
                                vaccineDoseFipsContainer.reserveDoseIfPossible(fipsCode);
                            }
                        }
                    }

                }

                if (!someoneToGiveFirstDosesTo.get() & !alreadyObservingRegionArrivals.getOrDefault(fipsCode, false)) {
                    // Nobody left to give first doses to for now, so register to observe new arrivals
                    toggleFipsCodePersonArrivalObservation(environment, fipsCode, true);
                }

                if (someoneToGiveFirstDosesTo.get() || someoneToGiveSecondDosesTo.containsValue(true)) {
                    // Schedule next vaccination
                    final double vaccinationTime = environment.getTime() +
                            interVaccinationDelayDistributions.get(vaccineAdministratorId).get(fipsCode).sample();
                    environment.addPlan(new VaccinationPlan(vaccineAdministratorId, fipsCode), vaccinationTime,
                            new MultiKey(vaccineAdministratorId, fipsCode));
                }

            }
            // No vaccine available, so pause vaccinating for now and wait for vaccine delivery
        }

        private void toggleFipsCodePersonArrivalObservation(Environment environment, FipsCode fipsCode, boolean observe) {
            if (!alreadyObservingRegionArrivals.getOrDefault(fipsCode, false)) {
                for (RegionId regionId : fipsCodeRegionMap.get(fipsCode)) {
                    environment.observeRegionPersonArrival(observe, regionId);
                }
                alreadyObservingRegionArrivals.put(fipsCode, true);
            }
        }

        /*
            A plan to add vaccine to the simulation
         */
        private static class VaccineDeliveryPlan implements Plan {

            final Map<VaccineId, FipsCodeDouble> doses;

            private VaccineDeliveryPlan(Map<VaccineId, FipsCodeDouble> doses) {
                this.doses = doses;
            }

        }

        /*
            A plan to vaccinate a random person from the population
         */
        private static class VaccinationPlan implements Plan {

            private final VaccineAdministratorId vaccineAdministratorId;
            private final FipsCode fipsCode;

            public VaccinationPlan(VaccineAdministratorId vaccineAdministratorId, FipsCode fipsCode) {
                this.vaccineAdministratorId = vaccineAdministratorId;
                this.fipsCode = fipsCode;
            }

        }

        /*
            A plan to add a person to the queue of people needing second doses
         */
        private static class SecondDoseQueuePlan implements Plan {

            private final VaccineAdministratorId vaccineAdministratorId;
            private final PersonId personId;
            private final VaccineId vaccineId;

            private SecondDoseQueuePlan(VaccineAdministratorId vaccineAdministratorId, PersonId personId, VaccineId vaccineId) {
                this.vaccineAdministratorId = vaccineAdministratorId;
                this.personId = personId;
                this.vaccineId = vaccineId;
            }
        }

    }

}
