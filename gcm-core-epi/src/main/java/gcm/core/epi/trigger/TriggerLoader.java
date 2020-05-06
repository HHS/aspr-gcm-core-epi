package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.JsonNode;

public interface TriggerLoader {

    void load(TriggerContainer.Builder builder, String id, JsonNode yamlData);

}
