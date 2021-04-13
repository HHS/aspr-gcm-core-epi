package gcm.core.epi.propertytypes;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.population.PopulationDescription;
import org.immutables.value.Value;
import plugins.gcm.agents.Environment;
import plugins.regions.support.RegionId;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toMap;

public abstract class AbstractFipsCodeDouble {

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
        if (values().keySet().stream().anyMatch(x -> x.scope() != scope())) {
            throw new IllegalStateException("FipsCodeValues has values that are of the incorrect scope");
        }
    }

    public Map<FipsCode, Double> getFipsCodeValues(Environment environment) {
        Map<FipsCode, Double> thresholdsByFipsCode;
        PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
        if (type() == ValueType.NUMBER) {
            Set<FipsCode> fipsCodes = populationDescription.regionIds().stream()
                    .map(scope()::getFipsSubCode)
                    // Force set ordering
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            thresholdsByFipsCode = fipsCodes.stream()
                    .collect(toMap(fipsCode -> fipsCode,
                            fipsCode -> values().getOrDefault(fipsCode, defaultValue()),
                            (key1, key2) -> {
                                throw new RuntimeException("Duplicate keys in threshold map");
                            },
                            // Force map ordering
                            LinkedHashMap::new));
        } else {
            Map<RegionId, Long> regionPopulations = populationDescription.populationByRegion();
            Map<FipsCode, Long> fipsCodePopulations = populationDescription.regionIds().stream()
                    .collect(Collectors.groupingBy(scope()::getFipsSubCode,
                            LinkedHashMap::new,
                            summingLong(regionId -> regionPopulations.getOrDefault(regionId, 0L))));
            thresholdsByFipsCode = fipsCodePopulations.entrySet().stream()
                    .collect(toMap(Map.Entry::getKey,
                            entry -> entry.getValue().doubleValue() *
                                    values().getOrDefault(entry.getKey(), defaultValue()),
                            (key1, key2) -> {
                                throw new RuntimeException("Duplicate keys in threshold map");
                            },
                            // Force map ordering
                            LinkedHashMap::new));
        }
        return thresholdsByFipsCode;
    }

    public enum ValueType {
        FRACTION,
        NUMBER
    }

}
