package gcm.core.epi.plugin.behavior;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.plugin.behavior.ImmutableTriggeredPropertyOverride;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableTriggeredPropertyOverride.class)
public interface TriggeredPropertyOverride {

    String trigger();

    String property();

    JsonNode value();

}
