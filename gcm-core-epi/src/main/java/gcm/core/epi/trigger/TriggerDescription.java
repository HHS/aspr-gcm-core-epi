package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.trigger.ImmutableTriggerDescription;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableTriggerDescription.class)
public interface TriggerDescription {

    String id();

    TriggerType type();

    JsonNode data();

}
