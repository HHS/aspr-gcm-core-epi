package gcm.core.epi.test.manual.propertytypes;

import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.ImmutableAgeGroup;
import gcm.core.epi.population.ImmutableAgeGroupPartition;
import gcm.scenario.GroupTypeId;
import gcm.scenario.RegionId;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value.Immutable
public abstract class CompactPopulationDescription {

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

    public abstract List<Integer> homeGroupIdByPersonId();

    public abstract List<Integer> workGroupIdByPersonId();

    public abstract List<Integer> schoolGroupIdByPersonId();

    //public abstract List<GroupTypeId> groupTypeByGroupId();

    /*
    This will describe the partition of the population into age groups
    */
    @Value.Default
    public AgeGroupPartition ageGroupPartition() {
        return ImmutableAgeGroupPartition.builder().addAgeGroupList(
                ImmutableAgeGroup.builder().name("All").build()).build();
    }

    /*
        This set will contain all of the regionIds being used by the members of this population
     */
    @Value.Derived
    public Set<RegionId> regionIds() {
        return populationByRegion().keySet();
    }

    /*
        This set will contain the population by regionId in this simulation
    */
    @Value.Derived
    public Map<RegionId, Long> populationByRegion() {
        return regionByPersonId().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /*
        This will report the fraction of the population in each age group
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
