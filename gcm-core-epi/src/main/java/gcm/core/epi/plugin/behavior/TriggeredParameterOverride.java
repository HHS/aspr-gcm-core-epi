package gcm.core.epi.plugin.behavior;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableTriggeredParameterOverride.class)
public interface TriggeredParameterOverride {

    String trigger();

    String parameter();

    JsonNode value();

}
