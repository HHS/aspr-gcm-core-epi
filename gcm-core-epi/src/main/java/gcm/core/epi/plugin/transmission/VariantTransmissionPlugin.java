package gcm.core.epi.plugin.transmission;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.propertytypes.InfectionData;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

import java.util.*;

public class VariantTransmissionPlugin implements TransmissionPlugin {

    static final GlobalComponentId VARIANT_MANAGER_ID = new GlobalComponentId() {
        @Override
        public String toString() {
            return "SCHOOL_CLOSURE_MANAGER_ID";
        }
    };

    @Override
    public double getRelativeTransmissibility(Environment environment, PersonId personId) {
        double relativeTransmissibilty = environment.getGlobalPropertyValue(VariantGlobalProperty.VARIANT_RELATIVE_TRANSMISSIBILITY);
        boolean hasVariant = environment.getPersonPropertyValue(personId, VariantPersonProperty.HAS_VARIANT);
        return hasVariant ? relativeTransmissibilty : 1.0;
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(VariantRandomId.VARIANT_RANDOM_ID);
        return randomIds;
    }

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>(EnumSet.allOf(VariantPersonProperty.class));
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

    public enum VariantPersonProperty implements DefinedPersonProperty {
        HAS_VARIANT(TypedPropertyDefinition.builder()
                .type(Boolean.class).defaultValue(false).build());

        private final TypedPropertyDefinition propertyDefinition;

        VariantPersonProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }
    }

    public enum VariantGlobalProperty implements DefinedGlobalProperty {

        VARIANT_INITIAL_PREVALENCE(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).build()),

        VARIANT_SEEDING_START(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).build()),

        VARIANT_RELATIVE_TRANSMISSIBILITY(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).build());

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
            // Only called to seed variant
            // First seed the variant
            double variantInitialPrevalence = environment.getGlobalPropertyValue(VariantGlobalProperty.VARIANT_INITIAL_PREVALENCE);
            for (PersonId personId : environment.getPeopleInCompartment(Compartment.INFECTED)) {
                if (environment.getRandomGeneratorFromId(VariantRandomId.VARIANT_RANDOM_ID).nextDouble() < variantInitialPrevalence) {
                    environment.setPersonPropertyValue(personId, VariantPersonProperty.HAS_VARIANT, true);
                    // Change their transmissibility from that point forward
                    environment.setPersonPropertyValue(personId, PersonProperty.ACTIVITY_LEVEL_CHANGED, true);
                }
            }

            // Register to see people having infectious contact
            environment.observeGlobalPersonPropertyChange(true, PersonProperty.HAD_INFECTIOUS_CONTACT);
        }

        @Override
        public void observePersonPropertyChange(Environment environment, PersonId personId, PersonPropertyId personPropertyId) {
            // Only called when person had infectious contact
            Optional<InfectionData> infectionData = environment.getGlobalPropertyValue(GlobalProperty.MOST_RECENT_INFECTION_DATA);
            if (infectionData.isPresent()) {
                boolean sourceHadVariant = infectionData.get().sourcePersonId()
                        .map(sourcePersonId -> (boolean) environment.getPersonPropertyValue(sourcePersonId, VariantPersonProperty.HAS_VARIANT))
                        .orElse(false);
                if (sourceHadVariant) {
                    environment.setPersonPropertyValue(personId, VariantPersonProperty.HAS_VARIANT, true);
                }
            }
        }

        private static final class VariantSeedingPlan implements Plan {
        }
    }

}
