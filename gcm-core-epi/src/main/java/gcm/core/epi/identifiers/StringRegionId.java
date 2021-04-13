package gcm.core.epi.identifiers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import plugins.regions.support.RegionId;

@Value.Immutable(builder = false)
@JsonDeserialize(as = ImmutableStringRegionId.class)
public abstract class StringRegionId implements RegionId {

    public static StringRegionId of(String id) {
        return ImmutableStringRegionId.of(id);
    }

    @Value.Parameter
    abstract String id();

    @Override
    public String toString() {
        return id();
    }

}
