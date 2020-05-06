package gcm.core.epi.population;

import gcm.scenario.GroupTypeId;
import gcm.scenario.RegionId;
import org.immutables.value.Value;

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
