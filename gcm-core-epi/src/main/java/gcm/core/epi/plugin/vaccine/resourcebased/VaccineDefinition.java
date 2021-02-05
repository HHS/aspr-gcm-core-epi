package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableVaccineDefinition.class)
public abstract class VaccineDefinition {

    enum DoseType {
        ONE_DOSE,
        TWO_DOSE
    }

    enum EfficacyType {
        VE_S,
        VE_I,
        VE_P
    }

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
    double secondDoseDelay() { return 0.0; }

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

    double getVaccineEfficacy(long doses, double timeSinceLastDose, EfficacyType efficacyType) {
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
            default:
                efficacyTypeMultiplier = 0.0;
        }
        switch (type()) {
            case ONE_DOSE:
                if (doses == 1) {
                    return efficacyTypeMultiplier * firstDoseEfficacyFunction().getValue(timeSinceLastDose);
                }
                throw new RuntimeException("Unhandled number of doses");
            case TWO_DOSE:
                if (doses == 1) {
                    return efficacyTypeMultiplier * firstDoseEfficacyFunction().getValue(timeSinceLastDose)
                            * relativeEfficacyOfFirstDose();
                }
                if (doses == 2) {
                    return efficacyTypeMultiplier * secondDoseEfficacyFunction().getValue(timeSinceLastDose);
                }
                throw new RuntimeException("Unhandled number of doses");
            default:
                throw new RuntimeException("Unhandled dose type");
        }
    }

    public int dosesPerRegimen() {
        switch (type()) {
            case ONE_DOSE: return 1;
            case TWO_DOSE: return 2;
            default: throw new RuntimeException("Unhandled dose type");
        }
    }

}
