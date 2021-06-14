package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.variants.VariantId;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonDeserialize(as = ImmutableVaccineDefinition.class)
public abstract class VaccineDefinition {

    public abstract VaccineId id();

    @Value.Default
    public DoseType type() {
        return DoseType.ONE_DOSE;
    }

    @Value.Default
    EfficacyFunction firstDoseEfficacyFunction() {
        return ImmutableEfficacyFunction.builder().build();
    }

    // Only used for TWO_DOSE type
    @Value.Default
    EfficacyFunction secondDoseEfficacyFunction() {
        return ImmutableEfficacyFunction.builder().build();
    }

    // Only used for TWO_DOSE type
    @Value.Default
    double relativeEfficacyOfFirstDose() {
        return 1.0;
    }

    // Only used for TWO_DOSE type
    @Value.Default
    double secondDoseDelay() {
        return 0.0;
    }

    @Value.Default
    double vES() {
        return 0;
    }

    @Value.Default
    double vEI() {
        return 0;
    }

    @Value.Default
    double vEP() {
        return 0;
    }

    @Value.Default
    double vED() {
        return 0;
    }

    public abstract Map<VariantId, Double> variantRelativeEfficacy();

    // Only used for TWO_DOSE type - this is in addition to the overall variant relative efficacy
    public abstract Map<VariantId, Double> variantFirstDoseRelativeEfficacy();

    // Only used for TWO_DOSE type
    @Value.Default
    public double variantFirstDoseRelativeEfficacyPhaseout() { return 0; }

    public double getVaccineEfficacy(long doses, double timeSinceLastDose, VariantId variantId, EfficacyType efficacyType) {
        if (doses == 0) {
            return 0;
        }
        double efficacyTypeMultiplier;
        switch (efficacyType) {
            case VE_S:
                efficacyTypeMultiplier = vES();
                break;
            case VE_I:
                efficacyTypeMultiplier = vEI();
                break;
            case VE_P:
                efficacyTypeMultiplier = vEP();
                break;
            case VE_D:
                efficacyTypeMultiplier = vED();
                break;
            default:
                efficacyTypeMultiplier = 0.0;
        }
        // Account for strain
        efficacyTypeMultiplier *= variantRelativeEfficacy().getOrDefault(variantId, 1.0);
        // Account for dose and time effect
        switch (type()) {
            case ONE_DOSE:
                if (doses == 1) {
                    return efficacyTypeMultiplier * firstDoseEfficacyFunction().getValue(timeSinceLastDose);
                }
                throw new RuntimeException("Unhandled number of doses");
            case TWO_DOSE:
                if (doses == 1) {
                    double firstDoseEfficacyTypeMultiplier = variantFirstDoseRelativeEfficacy().getOrDefault(variantId, 1.0);
                    return efficacyTypeMultiplier * firstDoseEfficacyTypeMultiplier *
                            firstDoseEfficacyFunction().getValue(timeSinceLastDose) * relativeEfficacyOfFirstDose();
                }
                if (doses == 2) {
                    if (variantFirstDoseRelativeEfficacyPhaseout() > 0 ) {
                        double firstDoseEfficacyTypeMultiplier = variantFirstDoseRelativeEfficacy()
                                .getOrDefault(variantId, 1.0);
                        double adjustedFirstDoseEfficacyTypeMultiplier = firstDoseEfficacyTypeMultiplier +
                                Math.min(timeSinceLastDose / variantFirstDoseRelativeEfficacyPhaseout(), 1.0) *
                                        (1.0 - firstDoseEfficacyTypeMultiplier);
                        return efficacyTypeMultiplier * adjustedFirstDoseEfficacyTypeMultiplier *
                                secondDoseEfficacyFunction().getValue(timeSinceLastDose);
                    } else {
                        return efficacyTypeMultiplier * secondDoseEfficacyFunction().getValue(timeSinceLastDose);
                    }
                }
                throw new RuntimeException("Unhandled number of doses");
            default:
                throw new RuntimeException("Unhandled dose type");
        }
    }

    public int dosesPerRegimen() {
        switch (type()) {
            case ONE_DOSE:
                return 1;
            case TWO_DOSE:
                return 2;
            default:
                throw new RuntimeException("Unhandled dose type");
        }
    }

    public enum DoseType {
        ONE_DOSE,
        TWO_DOSE
    }

    public enum EfficacyType {
        VE_S,
        VE_I,
        VE_P,
        VE_D
    }

}
