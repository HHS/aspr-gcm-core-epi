package gcm.core.epi.propertytypes;

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

    public FipsCode getFipsSubCode(RegionId regionId) {
        return getFipsCode(regionId.toString());
    }

    public FipsCode getFipsSubCode(FipsCode fipsCode) {
        if (this.hasBroaderScopeThan(fipsCode.scope())) {
            return getFipsCode(fipsCode.toString());
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
        return environment.getRegionIds().stream()
                .collect(Collectors.groupingBy(this::getFipsSubCode, Collectors.toSet()));
    }

    public boolean hasBroaderScopeThan(FipsScope scope) {
        return this.ordinal() <= scope.ordinal();
    }

}
