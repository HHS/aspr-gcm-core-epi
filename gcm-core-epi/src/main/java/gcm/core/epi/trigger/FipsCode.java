package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

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

}
