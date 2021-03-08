package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@JsonDeserialize(as = ImmutableVaccineAdministratorDefinitionOverride.class)
public interface VaccineAdministratorDefinitionOverride {

    @Nullable
    FipsCodeDouble vaccinationRatePerDay();

    @Nullable
    AgeWeights vaccineUptakeWeights();

    @Nullable
    VaccineAdministratorDefinition.UptakeNormalization vaccineUptakeNormalization();

    @Nullable
    AgeWeights vaccineHighRiskUptakeWeights();

    // Don't think this needs overrides
    // @Nullable
    // Boolean reserveSecondDoses();

    @Nullable
    Double fractionReturnForSecondDose();

}
