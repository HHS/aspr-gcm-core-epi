package gcm.core.epi.population;

import gcm.scenario.RegionId;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
    This class represents an immutable description of all of the people and their associated groups for a simulation
 */
@Value.Immutable(prehash = true)
public abstract class PopulationDescription {

    public static final Integer NO_GROUP_ASSIGNED = -1;

    /*
        This will be the id used for naming the population description in toString() and output reporting
     */
    abstract String id();

    /*
        A list of the age group indexes for each person
     */
    public abstract List<Integer> ageGroupIndexByPersonId();

    /*
        A list of the region ids for each person
    */
    public abstract List<RegionId> regionByPersonId();

    /*
        The assignment of people to homes by integer id
            Ids are to be unique across all contact group types
            New ids are assumed to occur sequentially in the order of home, school, and then work group.
            Use NO_GROUP_ASSIGNED (and not null) when not applicable - annotation below is used to preserve interning
     */
    @AllowNulls
    public abstract List<Integer> homeGroupIdByPersonId();

    /*
    The assignment of people to schools by integer id
        Ids are to be unique across all contact group types
        New ids are assumed to occur sequentially in the order of home, school, and then work group.
        Use NO_GROUP_ASSIGNED (and not null) when not applicable - annotation below is used to preserve interning
 */
    @AllowNulls
    public abstract List<Integer> schoolGroupIdByPersonId();

    /*
        The assignment of people to work groups by integer id
            Ids are to be unique across all contact group types
            New ids are assumed to occur sequentially in the order of home, school, and then work group.
            Use NO_GROUP_ASSIGNED (and not null) when not applicable - annotation below is used to preserve interning
     */
    @AllowNulls
    public abstract List<Integer> workGroupIdByPersonId();

    //TODO: If desired, support workplace region specification
    // @AllowNulls
    // public abstract List<RegionId> workGroupRegionByGroupId();

    /*
        The partition of the population into age groups
    */
    @Value.Default
    public AgeGroupPartition ageGroupPartition() {
        return ImmutableAgeGroupPartition.builder().addAgeGroupList(
                ImmutableAgeGroup.builder().name("All").build()).build();
    }

    /*
        The set of regionIds being used by the members of this population
     */
    @Value.Derived
    public Set<RegionId> regionIds() {
        return populationByRegion().keySet();
    }

    /*
        The population count by regionId
    */
    @Value.Derived
    public Map<RegionId, Long> populationByRegion() {
        return regionByPersonId().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /*
        The fraction of the population in each age group
     */
    @Value.Derived
    public Map<AgeGroup, Double> ageGroupDistribution() {
        Map<AgeGroup, Long> ageGroupCounts = ageGroupIndexByPersonId().stream()
                .map(ageGroupIndex -> ageGroupPartition().getAgeGroupFromIndex(ageGroupIndex))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return ageGroupCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> (double) entry.getValue() / ageGroupIndexByPersonId().size()));
    }

    // Use the id above as the string identifier
    @Override
    public String toString() {
        return id();
    }

}
