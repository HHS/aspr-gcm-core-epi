package gcm.core.epi.population;

import gcm.core.epi.identifiers.PersonProperty;
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

    /*
        This will be the id used for naming the population description in toString() and output reporting
     */
    abstract String id();

    /*
        This list will store all of the data needed to instantiate a person in the simulation
        Each person is represented by an element in the list, with implied IDs in order of the list
     */
    public abstract List<PersonData> dataByPersonId();

    /*
        This will describe the partition of the population into age groups
     */
    @Value.Default
    public AgeGroupPartition ageGroupPartition() {
        return ImmutableAgeGroupPartition.builder().addAgeGroupList(
                ImmutableAgeGroup.builder().name("All").build()).build();
    }

    /*
        This list will store all of the data needed to instantiate the groups in the simulation
        Each group is represented by an element in the list, with implied ID in order of the list

        The GroupSpecification will refer to members by ID as they appear in the list above
     */
    public abstract List<GroupSpecification> groupSpecificationByGroupId();

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
        return dataByPersonId().stream()
                .collect(Collectors.groupingBy(PersonData::regionId, Collectors.counting()));
    }

    /*
        This will report the fraction of the population in each age group
     */
    @Value.Derived
    public Map<AgeGroup, Double> ageGroupDistribution() {
        Map<AgeGroup, Long> ageGroupCounts = dataByPersonId().stream()
                .map(personData -> ageGroupPartition().getAgeGroupFromIndex(
                        (Integer) personData.personPropertyValues().get(PersonProperty.AGE_GROUP_INDEX)))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return ageGroupCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> (double) entry.getValue() / dataByPersonId().size()));
    }

    // Use the id above as the string identifier
    @Override
    public String toString() {
        return id();
    }

}
