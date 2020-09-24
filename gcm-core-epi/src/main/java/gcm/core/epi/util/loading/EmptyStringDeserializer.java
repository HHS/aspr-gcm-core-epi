package gcm.core.epi.util.loading;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class EmptyStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // Return empty strings as the empty string
        if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING && jsonParser.getText().equals("")) {
            return "";
        } else {
            return StringDeserializer.instance.deserialize(jsonParser, deserializationContext);
        }
    }
}
