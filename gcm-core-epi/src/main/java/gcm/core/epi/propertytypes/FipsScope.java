package gcm.core.epi.propertytypes;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.population.PopulationDescription;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum FipsScope {

    NATION,
    STATE,
    COUNTY,
    TRACT;

    public FipsCode getFipsSubCode(RegionId regionId) {
        return getFipsCode(regionId.toString());
    }

    public FipsCode getFipsSubCode(FipsCode fipsCode) {
        if (this.hasBroaderScopeThan(fipsCode.scope())) {
            return getFipsCode(fipsCode.code());
        } else {
            throw new IllegalArgumentException("FIPS code must have narrower scope than refinement");
        }
    }

    private FipsCode getFipsCode(String string) {
        switch (this) {
            case NATION:
                return FipsCode.of("");
            case STATE:
                return FipsCode.of(string.substring(0, 2));
            case COUNTY:
                return FipsCode.of(string.substring(0, 5));
            case TRACT:
                return FipsCode.of(string);
            default:
                throw new RuntimeException("Unknown Fips Scope");
        }
    }

    public Map<FipsCode, Set<RegionId>> getFipsCodeRegionMap(Environment environment) {
        // Restrict to region ids being used in the simulation
        PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
        return populationDescription.regionIds().stream()
                .collect(Collectors.groupingBy(this::getFipsSubCode,
                        // Force map order
                        LinkedHashMap::new,
                        Collectors.toCollection(LinkedHashSet::new)));
    }

    public boolean hasBroaderScopeThan(FipsScope scope) {
        return this.ordinal() <= scope.ordinal();
    }

}
