package gcm.core.epi.plugin.seeding;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsCodeValues;
import gcm.core.epi.propertytypes.ImmutableFipsCodeValues;
import gcm.core.epi.propertytypes.ImmutableInfectionData;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;
import gcm.simulation.partition.LabelSet;
import gcm.simulation.partition.Partition;
import org.apache.commons.math3.distribution.ExponentialDistribution;

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

        SEEDING_START_DAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        SEEDING_END_DAY(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        INITIAL_SEEDING_RATE_PER_DAY(PropertyDefinition.builder()
                .setType(FipsCodeValues.class).setDefaultValue(
                        ImmutableFipsCodeValues.builder().build())
                .setPropertyValueMutability(false).build()),

        SEEDING_GROWTH_DOUBLING_TIME(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(Double.POSITIVE_INFINITY).setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;

        ExponentialSeedingGlobalProperty(PropertyDefinition propertyDefinition) {
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

    private enum ExponentialSeedingRandomId implements RandomNumberGeneratorId {
        ID
    }

    public static class SeedingManager extends AbstractComponent {

        private static final Object SEEDING_PARTITION_KEY = new Object();

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
                FipsCodeValues seedingRateSpecification = environment.getGlobalPropertyValue(ExponentialSeedingGlobalProperty.INITIAL_SEEDING_RATE_PER_DAY);

                // Create partition TODO: Could be redundant with ContactManager
                environment.addPopulationPartition(Partition.region(regionId -> seedingRateSpecification.scope().getFipsSubCode(regionId)),
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
                environment.removePopulationPartition(SEEDING_PARTITION_KEY);

            } else if (plan.getClass() == SeedingPlan.class) {
                // Pick random person to infect
                SeedingPlan seedingPlan = (SeedingPlan) plan;
                Optional<PersonId> personId = environment.samplePartition(SEEDING_PARTITION_KEY,
                        LabelSet.region(seedingPlan.fipsCode),
                        ExponentialSeedingRandomId.ID);
                if (personId.isPresent()) {
                    Compartment compartment = environment.getPersonCompartment(personId.get());
                    if (compartment.equals(Compartment.SUSCEPTIBLE)) {
                        environment.setPersonCompartment(personId.get(), Compartment.INFECTED);
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
