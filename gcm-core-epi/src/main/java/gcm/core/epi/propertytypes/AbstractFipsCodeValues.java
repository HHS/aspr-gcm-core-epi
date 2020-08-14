package gcm.core.epi.propertytypes;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.population.PopulationDescription;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toMap;

public abstract class AbstractFipsCodeValues {

    @Value.Default
    public FipsScope scope() {
        return FipsScope.NATION;
    }

    @Value.Default
    public ValueType type() {
        return ValueType.NUMBER;
    }

    public abstract Map<FipsCode, Double> values();

    @Value.Default
    public Double defaultValue() {
        return 0.0;
    }

    @Value.Check
    protected void check() {
        if (values().keySet().stream()
                .filter(x -> x.scope() != scope())
                .findFirst()
                .isPresent()) {
            throw new IllegalStateException("FipsCodeValues has values that are of the incorrect scope");
        }
    }

    public Map<FipsCode, Double> getFipsCodeValues(Environment environment) {
        Map<FipsCode, Double> thresholdsByFipsCode;
        if (type() == ValueType.NUMBER) {
            Set<FipsCode> fipsCodes = environment.getRegionIds().stream()
                    .map(scope()::getFipsSubCode)
                    .collect(Collectors.toSet());
            thresholdsByFipsCode = fipsCodes.stream()
                    .collect(toMap(fipsCode -> fipsCode, fipsCode -> values().getOrDefault(fipsCode, defaultValue())));
        } else {
            PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
            Map<RegionId, Long> regionPopulations = populationDescription.populationByRegion();
            Map<FipsCode, Long> fipsCodePopulations = environment.getRegionIds().stream()
                    .collect(Collectors.groupingBy(scope()::getFipsSubCode,
                            summingLong(regionId -> regionPopulations.getOrDefault(regionId, 0L))));
            thresholdsByFipsCode = fipsCodePopulations.entrySet().stream()
                    .collect(toMap(Map.Entry::getKey,
                            entry -> entry.getValue().doubleValue() *
                                    values().getOrDefault(entry.getKey(), defaultValue())));
        }
        return thresholdsByFipsCode;
    }

    public enum ValueType {
        FRACTION,
        NUMBER
    }

}
