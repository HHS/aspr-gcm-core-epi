package gcm.core.epi.test.manual.propertytypes;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.ImmutableAgeGroup;
import gcm.core.epi.population.ImmutableAgeGroupPartition;
import gcm.scenario.RegionId;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value.Immutable
public abstract class ListCompactPopulationDescription {

    /*
        This will be the id used for naming the population description in toString() and output reporting
     */
    abstract String id();

    /*
        A map that stores for each person property the list of values for each person
     */
    public abstract Map<PersonProperty, List<Object>> personPropertyValueByPersonId();

    /*
        A list of the region ids for each person
    */
    public abstract List<RegionId> regionByPersonId();

    public abstract List<ContactGroupType> groupTypeByGroupId();

    public abstract List<List<Integer>> groupMembersByGroupId();

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
        List<Object> ageGroupIndexByPersonId = personPropertyValueByPersonId().get(PersonProperty.AGE_GROUP_INDEX);
        Map<AgeGroup, Long> ageGroupCounts = ageGroupIndexByPersonId.stream()
                .map(ageGroupIndex -> ageGroupPartition().getAgeGroupFromIndex((Integer) ageGroupIndex))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return ageGroupCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> (double) entry.getValue() / ageGroupIndexByPersonId.size()));
    }

    // Use the id above as the string identifier
    @Override
    public String toString() {
        return id();
    }

}
