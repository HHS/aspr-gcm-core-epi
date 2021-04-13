package gcm.core.epi.population;

import org.immutables.value.Value;
import plugins.groups.support.GroupId;
import plugins.regions.support.RegionId;

@Value.Immutable
public interface HospitalData {

    RegionId regionId();

    int beds();

    GroupId staffWorkplaceGroup();

    GroupId patientGroup();

}
