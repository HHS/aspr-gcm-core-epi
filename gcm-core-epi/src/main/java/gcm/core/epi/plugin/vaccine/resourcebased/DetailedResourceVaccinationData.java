package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.population.AgeGroup;
import org.immutables.value.Value;
import plugins.regions.support.RegionId;

@Value.Immutable
@JsonDeserialize(as = ImmutableDetailedResourceVaccinationData.class)
public interface DetailedResourceVaccinationData {

    RegionId regionId();

    VaccineAdministratorId vaccineAdministratorId();

    VaccineId vaccineId();

    DoseType doseType();

    AgeGroup ageGroup();

    enum DoseType {
        FIRST_DOSE,
        SECOND_DOSE
    }

}
