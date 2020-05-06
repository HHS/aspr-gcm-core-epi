package gcm.core.epi.population;

import gcm.scenario.GroupId;
import gcm.scenario.RegionId;
import org.immutables.value.Value;

@Value.Immutable
public interface HospitalData {

    RegionId regionId();

    int beds();

    GroupId staffWorkplaceGroup();

    GroupId patientGroup();

}
