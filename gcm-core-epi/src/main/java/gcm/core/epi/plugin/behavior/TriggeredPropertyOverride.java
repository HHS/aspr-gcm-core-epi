package gcm.core.epi.plugin.behavior;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonDeserialize(as = ImmutableTriggeredPropertyOverride.class)
public interface TriggeredPropertyOverride {

    String trigger();

    Map<String, JsonNode> overrides();

}
