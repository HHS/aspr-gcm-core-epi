package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable(builder = false)
@JsonDeserialize(as = ImmutableFipsCode.class)
public abstract class FipsCode {

    static public FipsCode of(String code) {
        return ImmutableFipsCode.of(code);
    }

    @Value.Parameter
    public abstract String code();

    @Value.Derived
    public FipsScope scope() {
        if (code().length() == 0) {
            return FipsScope.NATION;
        } else if (code().length() == 2) {
            return FipsScope.STATE;
        } else if (code().length() == 5) {
            return FipsScope.COUNTY;
        } else if (code().length() == 11) {
            return FipsScope.TRACT;
        } else {
            throw new RuntimeException("Invalid FIPS code length.");
        }
    }

    public Optional<FipsCode> getNextFipsCodeInHierarchy() {
        switch (scope()) {
            case NATION:
                return Optional.empty();
            case STATE:
                return Optional.of(FipsCode.of(""));
            case COUNTY:
                return Optional.of(FipsCode.of(this.code().substring(0, 2)));
            case TRACT:
                return Optional.of(FipsCode.of(this.code().substring(0, 5)));
            default:
                throw new RuntimeException("Invalid FIPS code type");
        }
    }

}
