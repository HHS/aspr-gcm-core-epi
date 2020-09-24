package gcm.core.epi.util.loading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = ImmutablePropertyValueJsonList.class)
public interface PropertyValueJsonList {
    List<JsonNode> jsonNodeList();
}
