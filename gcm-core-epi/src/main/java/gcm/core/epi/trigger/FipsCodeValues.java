package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.trigger.ImmutableFipsCodeValues;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableFipsCodeValues.class)
public abstract class FipsCodeValues extends AbstractFipsCodeValues {
    // Body is in AbstractFipsCodeValues
}
