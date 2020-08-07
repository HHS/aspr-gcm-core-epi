package gcm.core.epi.plugin.vaccine.twodose;

import org.immutables.value.Value;

@Value.Immutable
public interface TwoDoseVaccineEfficacySpecification {

    double doseOneVES();

    double doseOneVEI();

    double doseOneVEP();

    double doseTwoVES();

    double doseTwoVEI();

    double doseTwoVEP();

    double efficacyDelayDays();

    double efficacyDurationDays();

    double interDoseDelayDays();

}
