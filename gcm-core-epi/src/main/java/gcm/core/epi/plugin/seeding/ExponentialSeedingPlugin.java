package gcm.core.epi.plugin.seeding;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.core.epi.components.ContactManager;
import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import gcm.core.epi.propertytypes.ImmutableFipsCodeDouble;
import gcm.core.epi.propertytypes.ImmutableInfectionData;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.core.epi.variants.VariantId;
import gcm.core.epi.variants.VariantsDescription;
import nucleus.Plan;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.util.Pair;
import plugins.gcm.agents.AbstractComponent;
import plugins.gcm.agents.Environment;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.globals.support.GlobalComponentId;
import plugins.partitions.support.LabelSet;
import plugins.partitions.support.Partition;
import plugins.partitions.support.PartitionSampler;
import plugins.people.support.PersonId;
import plugins.regions.support.RegionId;
import plugins.regions.support.RegionLabeler;
import plugins.stochastics.support.RandomNumberGeneratorId;

import java.util.*;

public class ExponentialSeedingPlugin implements Plugin {

    final GlobalComponentId SEEDING_MANAGER_ID = new GlobalComponentId() {
        @Override
        public String toString() {
            return "SEEDING_MANAGER_ID";
        }
    };

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(ExponentialSeedingGlobalProperty.class));
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(ExponentialSeedingRandomId.ID);
        return randomIds;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        Plugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalComponentId(SEEDING_MANAGER_ID, SeedingManager.class);
    }

    public enum ExponentialSeedingGlobalProperty implements DefinedGlobalProperty {

        SEEDING_START_DAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        SEEDING_END_DAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),

        INITIAL_SEEDING_RATE_PER_DAY(TypedPropertyDefinition.builder()
                .type(FipsCodeDouble.class).defaultValue(
                        ImmutableFipsCodeDouble.builder().build())
                .isMutable(false).build()),

        SEEDING_GROWTH_DOUBLING_TIME(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(Double.POSITIVE_INFINITY).isMutable(false).build()),

        SEEDING_VARIANT_PREVALENCE(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<VariantId, Double>>() {
                }).defaultValue(new HashMap<>()).build()),

        SEEDING_VARIANT_START_DAY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(Double.POSITIVE_INFINITY).isMutable(false).build());

        private final TypedPropertyDefinition propertyDefinition;

        ExponentialSeedingGlobalProperty(TypedPropertyDefinition propertyDefinition) {
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

    private enum ExponentialSeedingRandomId implements RandomNumberGeneratorId {
        ID
    }

    public static class SeedingManager extends AbstractComponent {

        private static final Object SEEDING_PARTITION_KEY = new Object();
        private EnumeratedDistribution<Integer> variantSamplingDistribution;

        private void planNextSeeding(Environment environment, FipsCode fipsCode, double currentSeedingRate) {
            double seedingEndDay = environment.getGlobalPropertyValue(ExponentialSeedingGlobalProperty.SEEDING_END_DAY);
            double seedingGrowthDoublingTime = environment.getGlobalPropertyValue(ExponentialSeedingGlobalProperty.SEEDING_GROWTH_DOUBLING_TIME);
            double nextSeedingDelayTime = new ExponentialDistribution(environment.getRandomGeneratorFromId(ExponentialSeedingRandomId.ID),
                    1 / currentSeedingRate).sample();
            double nextSeedingTime = environment.getTime() + nextSeedingDelayTime;
            if (nextSeedingTime < seedingEndDay) {
                double nextSeedingRate = currentSeedingRate * Math.pow(2.0, nextSeedingDelayTime / seedingGrowthDoublingTime);
                environment.addPlan(new SeedingPlan(fipsCode, nextSeedingRate), nextSeedingTime);
            }
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            if (plan.getClass() == StartSeedingPlan.class) {
                FipsCodeDouble seedingRateSpecification = environment.getGlobalPropertyValue(ExponentialSeedingGlobalProperty.INITIAL_SEEDING_RATE_PER_DAY);

                // Create partition TODO: Could be redundant with ContactManager
                environment.addPartition(Partition.builder()
                                .addLabeler(new RegionLabeler(regionId -> seedingRateSpecification.scope().getFipsSubCode(regionId)))
                                .build(),
                        SEEDING_PARTITION_KEY);

                // Start Seeding
                Map<FipsCode, Double> seedingRatesPerDay = seedingRateSpecification.getFipsCodeValues(environment);
                seedingRatesPerDay.forEach((key, value) -> planNextSeeding(environment, key, value));

                // Schedule stop seeding plan
                double seedingEndDay = environment.getGlobalPropertyValue(ExponentialSeedingGlobalProperty.SEEDING_END_DAY);
                environment.addPlan(new StopSeedingPlan(), seedingEndDay);

            } else if (plan.getClass() == StopSeedingPlan.class) {
                // End Seeding
                // Seeding plans already time-limited so nothing to cancel

                // Remove partition
                environment.removePartition(SEEDING_PARTITION_KEY);

            } else if (plan.getClass() == SeedingPlan.class) {
                // Pick random person to infect
                SeedingPlan seedingPlan = (SeedingPlan) plan;
                Optional<PersonId> personId = environment.samplePartition(SEEDING_PARTITION_KEY, PartitionSampler.builder()
                        .setLabelSet(LabelSet.builder().setLabel(RegionId.class, seedingPlan.fipsCode).build())
                        .setRandomNumberGeneratorId(ExponentialSeedingRandomId.ID)
                        .build());
                if (personId.isPresent()) {
                    Compartment compartment = environment.getPersonCompartment(personId.get());
                    if (compartment.equals(Compartment.SUSCEPTIBLE)) {
                        // Select strain and infect
                        double variantSeedingStartDay = environment.getGlobalPropertyValue(ExponentialSeedingGlobalProperty.SEEDING_VARIANT_START_DAY);
                        if (environment.getTime() >= variantSeedingStartDay) {
                            int variantIndex = variantSamplingDistribution.sample();
                            ContactManager.infectPerson(environment, personId.get(), variantIndex);
                        } else {
                            ContactManager.infectPerson(environment, personId.get(), 0);
                        }
                        environment.setGlobalPropertyValue(GlobalProperty.MOST_RECENT_INFECTION_DATA,
                                Optional.of(ImmutableInfectionData.builder()
                                        .targetPersonId(personId)
                                        .transmissionSetting(ContactGroupType.GLOBAL)
                                        .transmissionOccurred(true)
                                        .build()));
                    }
                }
                // Plan next seeding event
                planNextSeeding(environment, seedingPlan.fipsCode, seedingPlan.seedingRatePerDay);
            }
        }

        @Override
        public void init(Environment environment) {
            // Start seeding plan
            double seedingStartTime = environment.getGlobalPropertyValue(ExponentialSeedingGlobalProperty.SEEDING_START_DAY);
            environment.addPlan(new StartSeedingPlan(), seedingStartTime);

            // Validate variant seeding prevalence and store distribution
            Map<VariantId, Double> variantSeedingPrevalence = environment.getGlobalPropertyValue(
                    ExponentialSeedingGlobalProperty.SEEDING_VARIANT_PREVALENCE);
            if (variantSeedingPrevalence.values().stream().anyMatch(x -> x < 0)) {
                throw new RuntimeException("Negative variant seeding prevalence");
            }
            double total = variantSeedingPrevalence.values().stream().mapToDouble(x -> x).sum();
            if (total > 1.0) {
                throw new RuntimeException("Total variant prevalence is too high");
            }
            List<Pair<Integer, Double>> variantIndexSamplingWeights = new ArrayList<>();
            VariantsDescription variantsDescription = environment.getGlobalPropertyValue(GlobalProperty.VARIANTS_DESCRIPTION);
            for (VariantId variantId : variantsDescription.variantIdList()) {
                int variantIndex = variantsDescription.getVariantIndex(variantId);
                if (variantId.equals(VariantId.REFERENCE_ID) && total < 1.0) {
                    variantIndexSamplingWeights.add(new Pair<>(variantIndex, 1.0 - total));
                } else {
                    double variantPrevalence = variantSeedingPrevalence.getOrDefault(variantId, 0.0);
                    if (variantPrevalence > 0) {
                        variantIndexSamplingWeights.add(new Pair<>(variantIndex, variantPrevalence));
                    }
                }
            }
            variantSamplingDistribution = new EnumeratedDistribution<>(
                    environment.getRandomGeneratorFromId(ExponentialSeedingRandomId.ID),
                    variantIndexSamplingWeights);
        }

        private static class StartSeedingPlan implements Plan {
        }

        private static class StopSeedingPlan implements Plan {
        }

        private static class SeedingPlan implements Plan {
            private final FipsCode fipsCode;
            private final double seedingRatePerDay;

            private SeedingPlan(FipsCode fipsCode, double seedingRatePerDay) {
                this.fipsCode = fipsCode;
                this.seedingRatePerDay = seedingRatePerDay;
            }
        }

    }

}
