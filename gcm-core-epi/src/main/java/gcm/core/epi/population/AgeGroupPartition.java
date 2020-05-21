package gcm.core.epi.population;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value.Immutable()
@JsonDeserialize(as = ImmutableAgeGroupPartition.class)
public abstract class AgeGroupPartition {

    @Value.Parameter
    abstract List<AgeGroup> ageGroupList();

    @Derived
    protected Map<String, Integer> ageGroupMap() {
        Map<String, Integer> ageGroupMap = new HashMap<>();
        int i = 0;
        for (AgeGroup ageGroup : ageGroupList()) {
            ageGroupMap.put(ageGroup.name(), i);
            i++;
        }
        return ageGroupMap;
    }

    public AgeGroup getAgeGroupFromName(String name) {
        return ageGroupList().get(getAgeGroupIndexFromName(name));
    }

    public Integer getAgeGroupIndexFromName(String name) {
        return ageGroupMap().get(name);
    }

    public AgeGroup getAgeGroupFromIndex(Integer index) {
        return ageGroupList().get(index);
    }

    public AgeGroup getAgeGroupFromAge(Integer age) {
        // TODO: This is slow, so consider converting to a method that sorts the age groups
        return ageGroupList().
                stream().
                filter(x -> x.contains(age))
                .findFirst()
                .get();
    }

    public Integer getAgeGroupIndexFromAge(Integer age) {
        return getAgeGroupIndexFromName(getAgeGroupFromAge(age).name());
    }

    /*
        TODO: Validate that this partitions all age ranges
     */

}
