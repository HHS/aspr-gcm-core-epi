package gcm.core.epi.components;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.*;
import gcm.core.epi.plugin.behavior.BehaviorPlugin;
import gcm.core.epi.plugin.infection.InfectionPlugin;
import gcm.core.epi.plugin.transmission.TransmissionPlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.ImmutableInfectionData;
import gcm.core.epi.propertytypes.TransmissionStructure;
import gcm.scenario.*;
import gcm.simulation.*;
import gcm.util.MultiKey;
import gcm.util.geolocator.GeoLocator;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ContactManager extends AbstractComponent {

    private static final Logger logger = LoggerFactory.getLogger(ContactManager.class);

    public static Optional<PersonId> getGlobalContactFor(Environment environment, PersonId sourcePersonId,
                                                         RandomNumberGeneratorId randomId) {
        double fractionOfGlobalContactsInHomeRegion =
                environment.getGlobalPropertyValue(GlobalProperty.FRACTION_OF_GLOBAL_CONTACTS_IN_HOME_REGION);
        RegionId sourceRegionId = environment.getPersonRegion(sourcePersonId);
        RegionId targetRegionId;
        if (environment.getRandomGeneratorFromId(RandomId.CONTACT_MANAGER).nextDouble() <
                fractionOfGlobalContactsInHomeRegion) {
            targetRegionId = sourceRegionId;
        } else {
            // Get a sample from the radiation flow distribution
            Map<RegionId, EnumeratedDistribution<RegionId>> radiationTargetDistributions =
                    environment.getGlobalPropertyValue(GlobalProperty.RADIATION_FLOW_TARGET_DISTRIBUTIONS);
            EnumeratedDistribution<RegionId> targetDistribution = radiationTargetDistributions.get(sourceRegionId);
            if (targetDistribution != null) {
                targetRegionId = targetDistribution.sample();
            } else {
                targetRegionId = sourceRegionId;
            }
        }

        // Use a weighting by age if provided, otherwise choose uniformly at random
        TransmissionStructure transmissionStructure = environment.getGlobalPropertyValue(GlobalProperty.TRANSMISSION_STRUCTURE);
        Map<AgeGroup, Map<AgeGroup, Double>> biWeightingFunctionMap = transmissionStructure
                .groupBiWeightingFunctionsMap().get(ContactGroupType.GLOBAL);
        if (biWeightingFunctionMap != null) {
            PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                    GlobalProperty.POPULATION_DESCRIPTION);
            AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
            AgeGroup sourceAgeGroup = ageGroupPartition.getAgeGroupFromIndex(
                    environment.getPersonPropertyValue(sourcePersonId, PersonProperty.AGE_GROUP_INDEX));
            Map<AgeGroup, Double> ageGroupSelectionWeights = biWeightingFunctionMap.get(sourceAgeGroup);

            Set<AgeGroup> ageGroups = populationDescription.ageGroupDistribution().keySet();
            // Lazy index creation
            if (!environment.populationIndexExists(new MultiKey(targetRegionId, ageGroups.iterator().next()))) {
                Filter regionFilter = Filter.region(targetRegionId);
                for (AgeGroup ageGroup : ageGroups) {
                    int ageGroupIndex = populationDescription.ageGroupPartition().getAgeGroupIndexFromName(ageGroup.name());
                    Filter regionAgeGroupFilter = regionFilter.and(Filter.property(PersonProperty.AGE_GROUP_INDEX,
                            Equality.EQUAL, ageGroupIndex));
                    environment.addPopulationIndex(regionAgeGroupFilter, new MultiKey(targetRegionId, ageGroup));
                }
            }
            // Get targets
            List<Pair<AgeGroup, Double>> ageGroupTargetWeights = ageGroups
                    .stream()
                    .map(ageGroup -> new Pair<>(ageGroup,
                            (double) environment.getIndexSize(new MultiKey(targetRegionId, ageGroup)) *
                                    ageGroupSelectionWeights.getOrDefault(ageGroup, 0.0)))
                    .collect(Collectors.toList());

            EnumeratedDistribution<AgeGroup> ageGroupDistribution =
                    new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(randomId),
                            ageGroupTargetWeights);
            AgeGroup targetAgeGroup = ageGroupDistribution.sample();

            return environment.getRandomIndexedPersonWithExclusionFromGenerator(
                    sourcePersonId,
                    new MultiKey(targetRegionId, targetAgeGroup),
                    RandomId.CONTACT_MANAGER);

        } else {
            // Lazy index creation
            if (!environment.populationIndexExists(targetRegionId)) {
                Filter regionFilter = Filter.region(targetRegionId);
                environment.addPopulationIndex(regionFilter, targetRegionId);
            }
            return environment.getRandomIndexedPersonWithExclusionFromGenerator(
                    sourcePersonId,
                    targetRegionId,
                    RandomId.CONTACT_MANAGER);
        }
    }

    @Override
    public void init(Environment environment) {

        // Register to observe people becoming and ceasing to be infectious
        environment.observeGlobalPersonPropertyChange(true, PersonProperty.IS_INFECTIOUS);

        // Age Group Distribution
        PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);
        Map<AgeGroup, Double> ageGroupDistribution = populationDescription.ageGroupDistribution();

        // Calculate ratio of infectious contacts by age group
        double averageTransmissionRatio = environment.getGlobalPropertyValue(
                GlobalProperty.AVERAGE_TRANSMISSION_RATIO);

        TransmissionStructure transmissionStructure = environment.getGlobalPropertyValue(
                GlobalProperty.TRANSMISSION_STRUCTURE);

        AgeWeights fractionSymptomatic = environment.getGlobalPropertyValue(GlobalProperty.FRACTION_SYMPTOMATIC);
        double asymptomaticInfectiousness = environment.getGlobalPropertyValue(GlobalProperty.ASYMPTOMATIC_INFECTIOUSNESS);

        // Normalizing Transmission
        double transmissionRatioNormalizationFactor = ageGroupDistribution.entrySet().stream()
                .map(entry -> entry.getValue() *
                        transmissionStructure.transmissionWeights().apply(entry.getKey()) *
                        (fractionSymptomatic.getWeight(entry.getKey()) +
                                asymptomaticInfectiousness * (1 - fractionSymptomatic.getWeight(entry.getKey()))))
                .reduce(0.0, Double::sum) /
                ageGroupDistribution.values().stream()
                        .reduce(0.0, Double::sum);

        // Transmission ratios by age group
        Map<AgeGroup, Double> transmissionRatios = new HashMap<>();
        for (AgeGroup ageGroup : ageGroupDistribution.keySet()) {
            transmissionRatios.put(ageGroup, averageTransmissionRatio *
                    transmissionStructure.transmissionWeights().apply(ageGroup) /
                    transmissionRatioNormalizationFactor);
        }
        environment.setGlobalPropertyValue(GlobalProperty.TRANSMISSION_RATIOS, transmissionRatios);

        // Build radiation model target sampling distributions for each region and add indexes for sampling
        Set<RegionId> regionIds =
                ((PopulationDescription) environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION))
                        .regionIds();

        Map<RegionId, EnumeratedDistribution<RegionId>> radiationTargetDistributions = new HashMap<>();

        // Build GeoLocator for radiation flow
        GeoLocator.Builder<RegionId> geoLocatorBuilder = GeoLocator.builder();

        for (RegionId regionId : regionIds) {
            // Add to GeoLocator
            double lat = environment.getRegionPropertyValue(regionId, RegionProperty.LAT);
            double lon = environment.getRegionPropertyValue(regionId, RegionProperty.LON);
            geoLocatorBuilder.addLocation(lat, lon, regionId);
        }

        GeoLocator<RegionId> geoLocator = geoLocatorBuilder.build();
        environment.setGlobalPropertyValue(GlobalProperty.RADIATION_FLOW_GEOLOCATOR, geoLocator);

        double radiationFlowMaxRadiusKM = environment.getGlobalPropertyValue(GlobalProperty.RADIATION_FLOW_MAX_RADIUS_KM);

        for (RegionId regionId : regionIds) {
            EnumeratedDistribution<RegionId> radiationTargetDistribution = new EnumeratedDistribution<>(
                    environment.getRandomGeneratorFromId(RandomId.CONTACT_MANAGER),
                    getRadiationModelFlowData(environment, regionId, radiationFlowMaxRadiusKM)
            );
            radiationTargetDistributions.put(regionId, radiationTargetDistribution);
        }

        environment.setGlobalPropertyValue(GlobalProperty.RADIATION_FLOW_TARGET_DISTRIBUTIONS,
                radiationTargetDistributions);

        // Register to observe the transmission ratio for a person changing (due to behavior)
        environment.observeGlobalPersonPropertyChange(true, PersonProperty.ACTIVITY_LEVEL_CHANGED);

    }

    /**
     * Gets the flow probabilities from a radiation model
     *
     * @param environment    The simulation environment
     * @param sourceRegionId The source region for radiation flow
     * @param maxRadiusKM    The maximum radius in KM from the source region to search for targets
     * @return A List of pairs of target region Ids together with flow probabilities (not normalized to 1).
     * If the source population is zero or there is no target region within maxRadiusKM will
     * default to the source region,
     */
    private List<Pair<RegionId, Double>> getRadiationModelFlowData(Environment environment, RegionId sourceRegionId, double maxRadiusKM) {
        double sourceRegionLat = environment.getRegionPropertyValue(sourceRegionId, RegionProperty.LAT);
        double sourceRegionLon = environment.getRegionPropertyValue(sourceRegionId, RegionProperty.LON);

        PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
        Map<RegionId, Long> regionPopulations = populationDescription.populationByRegion();

        GeoLocator<RegionId> radiationFlowGeolocator = environment.getGlobalPropertyValue(
                GlobalProperty.RADIATION_FLOW_GEOLOCATOR);
        List<Pair<RegionId, Double>> targetRegionIds =
                radiationFlowGeolocator.getPrioritizedLocations(sourceRegionLat, sourceRegionLon,
                        maxRadiusKM);

        List<Pair<RegionId, Double>> radiationTargetProbabilities = new ArrayList<>(targetRegionIds.size());

        if (targetRegionIds.size() == 1) {
            logger.warn("Warning: RegionId " + sourceRegionId + " has no neighbors within " + maxRadiusKM + " KM");
            // Default to contact only within source region
            radiationTargetProbabilities.add(new Pair<>(sourceRegionId, 1.0));
        }

        long sourceRegionPopulation = regionPopulations.get(sourceRegionId);
        long cumulativePopulation = 0;
        for (Pair<RegionId, Double> targetRegionIdWithDistance : targetRegionIds) {
            RegionId targetRegionId = targetRegionIdWithDistance.getFirst();
            long targetRegionPopulation = regionPopulations.get(targetRegionId);
            if (cumulativePopulation > 0) {
                double radiationWeight = (double) sourceRegionPopulation * targetRegionPopulation /
                        (cumulativePopulation * (double) (cumulativePopulation + targetRegionPopulation));
                radiationTargetProbabilities.add(new Pair<>(targetRegionId, radiationWeight));
            }
            cumulativePopulation += targetRegionPopulation;
        }

        // Default to contact only within source region
        if (cumulativePopulation == 0) {
            radiationTargetProbabilities.add(new Pair<>(sourceRegionId, 1.0));
        }

        return radiationTargetProbabilities;
    }

    @Override
    public void observePersonPropertyChange(Environment environment, PersonId personId, PersonPropertyId personPropertyId) {
        if (personPropertyId.equals(PersonProperty.IS_INFECTIOUS)) {
            boolean isInfectious = environment.getPersonPropertyValue(personId, personPropertyId);
            if (isInfectious) {
                /*
                 *  This is called when a person is first infectious, so we need to schedule an infectious contact for them
                 */
                scheduleRandomInfectiousContact(environment, personId);

            } else {
                /*
                 *  This should only be called for people leaving the Infected state and heading to Recovered,
                 *  in which case we will remove the planned infectious contact for this person
                 */
                environment.removePlan(personId);
            }
        } else if (personPropertyId.equals(PersonProperty.ACTIVITY_LEVEL_CHANGED)) {
            Optional<InfectiousContactPlan> infectiousContactPlan = environment.getPlan(personPropertyId);
            if (infectiousContactPlan.isPresent()) {
                double scheduledTransmissionRatio = infectiousContactPlan.get().transmissionRatio;
                double currentTransmissionRatio = getTransmissionRatio(environment, personId);
                // If current transmission ratio is higher, need to reschedule next contact
                if (currentTransmissionRatio > scheduledTransmissionRatio) {
                    environment.removePlan(personId);
                    scheduleRandomInfectiousContact(environment, personId);
                }
            }
        } else {
            throw new RuntimeException("ContactManager Error: Unexpected property change observation");
        }
    }

    private void scheduleRandomInfectiousContact(Environment environment, PersonId personId) {
        InfectionPlugin infectionModule = environment.getGlobalPropertyValue(GlobalProperty.INFECTION_PLUGIN);
        double transmissionRatio = getTransmissionRatio(environment, personId);
        double nextContactTime = environment.getTime() +
                infectionModule.getNextTransmissionTime(environment, personId, transmissionRatio);
        environment.addPlan(new InfectiousContactPlan(personId, transmissionRatio), nextContactTime, personId);
    }

    private double getTransmissionRatio(Environment environment, PersonId personId) {
        // Behavior
        Optional<BehaviorPlugin> behaviorPlugin = environment.getGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN);
        double relativeActivityLevelFromBehavior = behaviorPlugin.map(
                module -> module.getRelativeActivityLevel(environment, personId)
        ).orElse(1.0);
        // Age
        PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);
        Integer ageGroupIndex = environment.getPersonPropertyValue(personId, PersonProperty.AGE_GROUP_INDEX);
        AgeGroup ageGroup = populationDescription.ageGroupPartition().getAgeGroupFromIndex(ageGroupIndex);
        Map<AgeGroup, Double> transmissionRatios = environment.getGlobalPropertyValue(
                GlobalProperty.TRANSMISSION_RATIOS);
        // Symptomatic
        boolean willBeSymptomatic = environment.getPersonPropertyValue(personId, PersonProperty.WILL_BE_SYMPTOMATIC);
        double asymptomaticInfectiousness = environment.getGlobalPropertyValue(GlobalProperty.ASYMPTOMATIC_INFECTIOUSNESS);
        AgeWeights fractionSymptomaticByAge = environment.getGlobalPropertyValue(GlobalProperty.FRACTION_SYMPTOMATIC);
        double fractionSymptomatic = fractionSymptomaticByAge.getWeight(ageGroup);
        double symptomaticTransmissibility = 1 / (fractionSymptomatic + (1 - fractionSymptomatic) * asymptomaticInfectiousness);
        double relativeTransmissibilityFromSymptomaticStatus = willBeSymptomatic ?
                symptomaticTransmissibility : asymptomaticInfectiousness * symptomaticTransmissibility;

        return transmissionRatios.get(ageGroup) * relativeActivityLevelFromBehavior *
                relativeTransmissibilityFromSymptomaticStatus;
    }

    @Override
    public void executePlan(Environment environment, Plan plan) {
        /*
            This is only called for an InfectiousContactPlan
         */
        InfectiousContactPlan infectiousContactPlan = (InfectiousContactPlan) plan;
        PersonId sourcePersonId = infectiousContactPlan.sourcePersonId;

        double transmissionRatio = getTransmissionRatio(environment, sourcePersonId);

        if (transmissionRatio > infectiousContactPlan.transmissionRatio) {
            throw new RuntimeException("ContactManager Error: Planned transmission ratio is lower than current value");
        }

        // Handle if behavior change has lowered the transmission ratio since the contact plan was created
        if (transmissionRatio == infectiousContactPlan.transmissionRatio ||
                environment.getRandomGeneratorFromId(RandomId.CONTACT_MANAGER).nextDouble() <
                        transmissionRatio / infectiousContactPlan.transmissionRatio) {

            // Get contact group
            Optional<ContactGroupType> optionalContactGroupType = getContactGroupType(environment, sourcePersonId);
            if (optionalContactGroupType.isPresent()) {
                ContactGroupType contactGroupType = optionalContactGroupType.get();
                Optional<PersonId> targetPersonId;
                // Handle global contacts separately
                if (contactGroupType == ContactGroupType.GLOBAL) {

                    targetPersonId = getGlobalContactFor(environment, sourcePersonId, RandomId.CONTACT_MANAGER);

                } else {

                    List<GroupId> contactGroupId = environment.getGroupsForGroupTypeAndPerson(contactGroupType, sourcePersonId);
                    if (contactGroupId.size() != 1) {
                        throw new RuntimeException("ContactManager Error: random contact group selected for Person with ID: "
                                + sourcePersonId +
                                " expected to be of length 1, but was actually "
                                + contactGroupId.size());
                    }

                    TransmissionStructure transmissionStructure = environment.getGlobalPropertyValue(
                            GlobalProperty.TRANSMISSION_STRUCTURE);

                    // If a person is in a home by themselves, substitute a global contact with some probability
                    if (contactGroupType == ContactGroupType.HOME &
                            environment.getPersonCountForGroup(contactGroupId.get(0)) == 1) {
                        if (environment.getRandomGeneratorFromId(RandomId.CONTACT_MANAGER).nextDouble() <
                                transmissionStructure.singleHomeGlobalSubstitutionProbability()) {
                            // Take a global contact
                            targetPersonId = getGlobalContactFor(environment, sourcePersonId, RandomId.CONTACT_MANAGER);
                            contactGroupType = ContactGroupType.GLOBAL;
                        } else {
                            // Nobody else in the household to attempt to infect
                            targetPersonId = Optional.empty();
                        }
                    } else {
                        // Use a weighting function if provided, otherwise choose uniformly at random
                        BiWeightingFunction biWeightingFunction = transmissionStructure.groupBiWeightingFunctions().get(contactGroupType);
                        if (biWeightingFunction != null) {
                            targetPersonId = environment.getBiWeightedGroupContactFromGenerator(contactGroupId.get(0), sourcePersonId, true,
                                    transmissionStructure.groupBiWeightingFunctions().get(contactGroupType),
                                    RandomId.CONTACT_MANAGER);
                        } else {
                            targetPersonId = environment.getNonWeightedGroupContactWithExclusionFromGenerator(
                                    contactGroupId.get(0), sourcePersonId, RandomId.CONTACT_MANAGER);
                        }
                    }

                }

                // Track attempted infections for reporting
                ImmutableInfectionData.Builder infectionDataBuilder = ImmutableInfectionData.builder()
                        .sourcePersonId(sourcePersonId)
                        .targetPersonId(targetPersonId)
                        .transmissionSetting(contactGroupType)
                        .transmissionOccurred(false);

                // If the contact target is susceptible, then mark them as having an infectious contact
                if (targetPersonId.isPresent()) {

                    Compartment contactCompartment = environment.getPersonCompartment(targetPersonId.get());

                    if (contactCompartment == Compartment.SUSCEPTIBLE) {

                        // TODO: Re-incorporate vaccine, antiviral, and potentially other plugins
                        double probabilityVaccineFails = 1.0;
                        double probabilityAntiviralsFail = 1.0;
                        // What is their residual immunity (if any)?
                        double residualImmunity = (boolean) environment.getPersonPropertyValue(targetPersonId.get(), PersonProperty.IMMUNITY_WANED) ?
                                environment.getGlobalPropertyValue(GlobalProperty.IMMUNITY_WANES_RESIDUAL_IMMUNITY) :
                                0.0;

                        // Behavior effect via module
                        Optional<BehaviorPlugin> behaviorPlugin =
                                environment.getGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN);
                        final ContactGroupType contactSetting = contactGroupType;
                        double infectionProbability = behaviorPlugin
                                .map(plugin -> plugin.getInfectionProbability(environment, contactSetting, targetPersonId.get()))
                                .orElse(1.0);

                        // Transmission reduction effect via module
                        Optional<TransmissionPlugin> transmissionPlugin =
                                environment.getGlobalPropertyValue(GlobalProperty.TRANSMISSION_PLUGIN);
                        double infectionProbabilityFromTransmissionPlugin = transmissionPlugin
                                .map(plugin -> plugin.getInfectionProbability(environment, targetPersonId.get()))
                                .orElse(1.0);

                        // Randomly draw to determine if vaccine and/or antivirals prevent the transmission
                        if (environment.getRandomGeneratorFromId(RandomId.CONTACT_MANAGER).nextDouble() <=
                                probabilityVaccineFails * probabilityAntiviralsFail *
                                        (1.0 - residualImmunity) *
                                        infectionProbability * infectionProbabilityFromTransmissionPlugin) {
                            environment.setPersonPropertyValue(targetPersonId.get(), PersonProperty.HAD_INFECTIOUS_CONTACT, true);
                            // Flag that the infection occurred
                            infectionDataBuilder.transmissionOccurred(true);
                        }

                    }

                }

                // Store the data about this infection event for reporting
                environment.setGlobalPropertyValue(GlobalProperty.MOST_RECENT_INFECTION_DATA,
                        Optional.of(infectionDataBuilder.build()));
            }

        }

        // Schedule the next random infectious contact
        scheduleRandomInfectiousContact(environment, sourcePersonId);

    }

    private Optional<ContactGroupType> getContactGroupType(Environment environment, PersonId sourcePersonId) {

        List<ContactGroupType> contactGroupTypes = environment.getGroupTypesForPerson(sourcePersonId);
        contactGroupTypes.add(ContactGroupType.GLOBAL);

        PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);
        Integer sourceAgeGroupIndex = environment.getPersonPropertyValue(sourcePersonId, PersonProperty.AGE_GROUP_INDEX);
        AgeGroup sourceAgeGroup = populationDescription.ageGroupPartition().getAgeGroupFromIndex(sourceAgeGroupIndex);

        // Contact group sampling
        TransmissionStructure transmissionStructure = environment.getGlobalPropertyValue(
                GlobalProperty.TRANSMISSION_STRUCTURE);
        List<Pair<ContactGroupType, Double>> sourcePersonGroupWeights = contactGroupTypes
                .stream()
                .map(groupType -> new Pair<>(groupType,
                        transmissionStructure
                                .contactGroupSelectionWeights()
                                .get(sourceAgeGroup)
                                .apply(groupType)))
                .collect(Collectors.toList());

        EnumeratedDistribution<ContactGroupType> contactGroupDistribution =
                new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(RandomId.CONTACT_MANAGER),
                        sourcePersonGroupWeights);

        // Select a contact group
        ContactGroupType contactGroupType = contactGroupDistribution.sample();

        // Allow for behavioral modification
        Optional<BehaviorPlugin> behaviorPlugin = environment.getGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN);
        if (behaviorPlugin.isPresent()) {
            return behaviorPlugin.get().getSubstitutedContactGroup(environment, sourcePersonId, contactGroupType);
        } else {
            return Optional.of(contactGroupType);
        }
    }

    private static class InfectiousContactPlan implements Plan {
        final PersonId sourcePersonId;
        final double transmissionRatio;

        InfectiousContactPlan(PersonId sourcePersonId, double transmissionRatio) {
            this.sourcePersonId = sourcePersonId;
            this.transmissionRatio = transmissionRatio;
        }
    }
}
