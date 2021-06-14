package gcm.core.epi.test.manual.vaccine;

import gcm.core.epi.plugin.vaccine.resourcebased.*;
import gcm.core.epi.variants.VariantId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestVariantEfficacy {

    @Test
    public void test() {
        EfficacyFunction firstDoseEfficacyFunction = ImmutableEfficacyFunction.builder()
                .earlyPeakHeight(0.0)
                .earlyPeakTime(0.0)
                .earlyPeakDuration(7.0)
                .peakTime(14.0)
                .build();
        EfficacyFunction secondDoseEfficacyFunction = ImmutableEfficacyFunction.builder()
                .earlyPeakHeight(0.5)
                .earlyPeakTime(0.0)
                .earlyPeakDuration(7.0)
                .peakTime(14.0)
                .build();

        VariantId referenceVariant = VariantId.REFERENCE_ID;
        VariantId otherVariant = VariantId.of("Other");

        VaccineDefinition vaccineDefinition = ImmutableVaccineDefinition.builder()
                .id(VaccineId.of("Vaccine One"))
                .type(VaccineDefinition.DoseType.TWO_DOSE)
                .firstDoseEfficacyFunction(firstDoseEfficacyFunction)
                .secondDoseEfficacyFunction(secondDoseEfficacyFunction)
                .vES(1.0)
                .relativeEfficacyOfFirstDose(0.5)
                .putVariantRelativeEfficacy(otherVariant, 0.5)
                .putVariantFirstDoseRelativeEfficacy(otherVariant, 0.5)
                .variantFirstDoseRelativeEfficacyPhaseout(7.0)
                .build();

        // One dose reference
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 0.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 7.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, (7.0 + 14.0) / 2.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.25);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 14.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 28.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5);
        // Two dose reference
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 7.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, (7.0 + 14.0) / 2.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.75);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 14.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 1.0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 28.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 1.0);

        // One dose variant
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 7.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, (7.0 + 14.0) / 2.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.25 * 0.5 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 14.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 28.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5 * 0.5);

        // Two dose variant
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 7.0 / 2.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5 * 0.75);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 7.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, (7.0 + 14.0) / 2.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.75 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 14.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 1.0 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 28.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 1.0 * 0.5);

    }

}
