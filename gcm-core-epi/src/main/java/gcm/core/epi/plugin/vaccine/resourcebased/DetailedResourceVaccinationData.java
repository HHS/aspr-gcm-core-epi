package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.scenario.RegionId;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableDetailedResourceVaccinationData.class)
public interface DetailedResourceVaccinationData {

    RegionId regionId();
    VaccineAdministratorId vaccineAdministratorId();
    VaccineId vaccineId();
    DoseType doseType ();

    enum DoseType {
        FIRST_DOSE,
        SECOND_DOSE
    }

}
