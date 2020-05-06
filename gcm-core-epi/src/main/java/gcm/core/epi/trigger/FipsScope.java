package gcm.core.epi.trigger;

import gcm.scenario.RegionId;
import gcm.simulation.Environment;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum FipsScope {

    NATION,
    STATE,
    COUNTY,
    TRACT;

    public FipsCode getFipsCode(RegionId regionId) {
        switch (this) {
            case NATION:
                return FipsCode.of("");
            case STATE:
                return FipsCode.of(regionId.toString().substring(0, 2));
            case COUNTY:
                return FipsCode.of(regionId.toString().substring(0, 5));
            case TRACT:
                return FipsCode.of(regionId.toString());
            default:
                throw new RuntimeException("Unknown Fips Scope");
        }
    }

    public Set<FipsCode> getFipsCodesForRegions(Environment environment) {
        return environment.getRegionIds().stream()
                .map(this::getFipsCode)
                .collect(Collectors.toSet());
    }

    public Map<FipsCode, Set<RegionId>> getFipsCodeRegionMap(Environment environment) {
        return environment.getRegionIds().stream()
                .collect(Collectors.groupingBy(this::getFipsCode, Collectors.toSet()));
    }

}
