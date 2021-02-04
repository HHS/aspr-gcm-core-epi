package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import gcm.core.epi.propertytypes.ImmutableAgeWeights;
import gcm.core.epi.propertytypes.ImmutableFipsCodeDouble;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableVaccineAdministrator.class)
public abstract class VaccineAdministrator {

    public abstract VaccineAdministratorId id();

    @Value.Default
    public FipsCodeDouble vaccinationRatePerDay() {
        return ImmutableFipsCodeDouble.builder().build();
    }

    @Value.Default
    public AgeWeights vaccineUptakeWeights() {
        return ImmutableAgeWeights.builder().defaultValue(1.0).build();
    }

    @Value.Default
    public AgeWeights vaccineHighRiskUptakeWeights()  {
        return ImmutableAgeWeights.builder().defaultValue(1.0).build();
    }

    @Value.Default
    public boolean reserveSecondDoses() {
        return true;
    }

    @Value.Default
    public double fractionReturnForSecondDose() {
        return 1.0;
    }

}
