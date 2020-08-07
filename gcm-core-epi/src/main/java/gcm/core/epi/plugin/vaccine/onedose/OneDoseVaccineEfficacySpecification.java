package gcm.core.epi.plugin.vaccine.onedose;

import org.immutables.value.Value;

@Value.Immutable
public interface OneDoseVaccineEfficacySpecification {

    double vES();

    double vEI();

    double vEP();

    double efficacyDelayDays();

    double efficacyDurationDays();

}
