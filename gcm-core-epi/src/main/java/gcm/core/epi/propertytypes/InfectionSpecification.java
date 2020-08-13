package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;
import gcm.simulation.Filter;
import org.immutables.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = ImmutableInfectionSpecification.class)
public abstract class InfectionSpecification extends AbstractFipsCodeValues {

    public Map<FipsCode, Double> getInfectionsByFipsCode(Environment environment) {
        return getFipsCodeValues(environment);
    }

    public Map<FipsCode, Filter> getFipsCodeFilters(Environment environment) {
        if (scope() == FipsScope.NATION) {
            Map<FipsCode, Filter> nationalFipsCodeFilter = new HashMap<>();
            nationalFipsCodeFilter.put(FipsCode.of(""), Filter.allPeople());
            return nationalFipsCodeFilter;
        } else {
            Set<RegionId> regionIds = environment.getRegionIds();
            Map<FipsCode, Set<RegionId>> fipsCodes = regionIds.stream()
                    .filter(regionId -> environment.getRegionPopulationCount(regionId) > 0)
                    .collect(Collectors.groupingBy(scope()::getFipsSubCode, Collectors.toSet()));
            return fipsCodes.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> Filter.region(entry.getValue())));
        }
    }

}