package gcm.core.epi.plugin.transmission;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.propertytypes.InfectionData;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.core.epi.variants.VariantId;
import gcm.core.epi.variants.VariantsDescription;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.*;

public class VariantTransmissionPlugin implements TransmissionPlugin {

    static final GlobalComponentId VARIANT_MANAGER_ID = new GlobalComponentId() {
        @Override
        public String toString() {
            return "SCHOOL_CLOSURE_MANAGER_ID";
        }
    };

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(VariantRandomId.VARIANT_RANDOM_ID);
        return randomIds;
    }

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(VariantGlobalProperty.class));
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        TransmissionPlugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalComponentId(VARIANT_MANAGER_ID, VariantManager.class);
    }

    public enum VariantRandomId implements RandomNumberGeneratorId {
        VARIANT_RANDOM_ID
    }

    public enum VariantGlobalProperty implements DefinedGlobalProperty {

        VARIANT_INITIAL_PREVALENCE(TypedPropertyDefinition.builder()
                .typeReference(new TypeReference<Map<VariantId, Double>>() {
                }).defaultValue(new HashMap<>()).build()),

        VARIANT_SEEDING_START(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).build());

        private final TypedPropertyDefinition propertyDefinition;

        VariantGlobalProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

    public static class VariantManager extends AbstractComponent {

        @Override
        public void init(Environment environment) {
            double variantSeedingStart = environment.getGlobalPropertyValue(VariantGlobalProperty.VARIANT_SEEDING_START);
            environment.addPlan(new VariantSeedingPlan(), variantSeedingStart);
        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            // Only called to seed variants

            // Compute and validate weights
            Map<VariantId, Double> variantInitialPrevalence = environment.getGlobalPropertyValue(
                    VariantGlobalProperty.VARIANT_INITIAL_PREVALENCE);
            if (variantInitialPrevalence.values().stream().anyMatch(x -> x < 0)) {
                throw new RuntimeException("Negative initial prevalence");
            }
            double total = variantInitialPrevalence.values().stream().mapToDouble(x -> x).sum();
            if (total > 1.0) {
                throw new RuntimeException("Total initial prevalence is too high");
            }

            // Seed the variants as needed
            if (total > 0) {
                List<Pair<VariantId, Double>> variantSamplingWeights = new ArrayList<>();
                VariantsDescription variantsDescription = environment.getGlobalPropertyValue(GlobalProperty.VARIANTS_DESCRIPTION);
                for (VariantId variantId : variantsDescription.variantIdList()) {
                    if (variantId.equals(VariantId.REFERENCE_ID) && total < 1.0) {
                        variantSamplingWeights.add(new Pair<>(VariantId.REFERENCE_ID, 1.0 - total));
                    } else {
                        double variantPrevalence = variantInitialPrevalence.getOrDefault(variantId, 0.0);
                        if (variantPrevalence > 0) {
                            variantSamplingWeights.add(new Pair<>(variantId, variantPrevalence));
                        }
                    }
                }
                EnumeratedDistribution<VariantId> variantIdDistribution = new EnumeratedDistribution<>(
                        environment.getRandomGeneratorFromId(VariantRandomId.VARIANT_RANDOM_ID),
                        variantSamplingWeights);

                for (PersonId personId : environment.getPeopleInCompartment(Compartment.INFECTED)) {
                    // Update strain
                    VariantId variantId = variantIdDistribution.sample();
                    // If not reference, update and re-assess transmissibility
                    if (!variantId.equals(VariantId.REFERENCE_ID)) {
                        int strainIndex = variantsDescription.getVariantIndex(variantId);
                        environment.setPersonPropertyValue(personId, PersonProperty.PRIOR_INFECTION_STRAIN_INDEX_1, strainIndex);
                        environment.setPersonPropertyValue(personId, PersonProperty.ACTIVITY_LEVEL_CHANGED, true);
                    }
                }
            }


        }

        private static final class VariantSeedingPlan implements Plan {
        }
    }

}
