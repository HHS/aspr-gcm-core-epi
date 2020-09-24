package gcm.core.epi.util.loading;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

public class PropertyValueJsonListDeserializer extends JsonDeserializer<ImmutablePropertyValueJsonList> {

    @Override
    public ImmutablePropertyValueJsonList deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING && jsonParser.getText().equals("")) {
            // If input is a blank string, deserialize this to a list with a single JsonNode for this string
            return ImmutablePropertyValueJsonList.builder()
                    .addJsonNodeList(deserializationContext.readTree(jsonParser))
                    .build();
        } else {
            // Parse as List<JsonNode>
            JavaType type = deserializationContext.getTypeFactory().constructType(new TypeReference<List<JsonNode>>(){});
            return ImmutablePropertyValueJsonList.builder()
                    .jsonNodeList(deserializationContext.readValue(jsonParser, type))
                    .build();
        }
    }

}
