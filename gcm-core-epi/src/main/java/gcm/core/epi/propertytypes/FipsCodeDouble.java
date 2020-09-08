package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableFipsCodeDouble.class)
public abstract class FipsCodeDouble extends AbstractFipsCodeDouble {
    // Body is in AbstractFipsCodeValues
}
