package gcm.core.epi.population;

import org.immutables.value.Value;
import plugins.groups.support.GroupTypeId;
import plugins.regions.support.RegionId;

import java.util.List;
import java.util.Optional;

/*
    This object is an immutable representation of the data needed to instantiate a group in the simulation
 */
@Value.Immutable
public interface GroupSpecification {

    /*
        The type of the group
     */
    GroupTypeId groupType();

    /*
        The list of simulation PersonIds of people that are members of this group
     */
    List<Integer> groupMembers();

    /*
        The optional RegionId of the group
     */
    Optional<RegionId> regionId();

}
