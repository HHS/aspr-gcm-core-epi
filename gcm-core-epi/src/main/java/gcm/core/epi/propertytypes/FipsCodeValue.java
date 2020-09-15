package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.scenario.RegionId;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonDeserialize(as = ImmutableFipsCodeValue.class)
public abstract class FipsCodeValue<T> {

    @Value.Default
    public FipsScope scope() {
        return FipsScope.NATION;
    }

    public abstract Map<FipsCode, T> values();

    public abstract T defaultValue();

    public T getValue(RegionId regionId) {
        FipsCode fipsCode = scope().getFipsSubCode(regionId);
        return values().getOrDefault(fipsCode, defaultValue());
    }

    @JsonCreator
    public FipsCodeValue<T> of(T value) {
        return ImmutableFipsCodeValue.<T>builder().defaultValue(value).build();
    }

}
