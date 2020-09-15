package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

public class FipsCodeDoubleDeserializer extends JsonDeserializer<FipsCodeDouble> implements
        ResolvableDeserializer {

    private final JsonDeserializer<?> defaultDeserializer;

    public FipsCodeDoubleDeserializer(JsonDeserializer<?> defaultDeserializer) {
        this.defaultDeserializer = defaultDeserializer;
    }

    @Override
    public ImmutableFipsCodeDouble deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // Store json to use for second-round parsing
        JsonNode jsonNode = jsonParser.readValueAsTree();
        try {
            jsonParser = new TreeTraversingParser(jsonNode);
            jsonParser.nextToken();
            return (ImmutableFipsCodeDouble) defaultDeserializer.deserialize(jsonParser, deserializationContext);
        } catch (IOException e) {
            jsonParser = new TreeTraversingParser(jsonNode);
            jsonParser.nextToken();
            return ImmutableFipsCodeDouble.builder()
                    .defaultValue(deserializationContext.readValue(jsonParser, Double.class)).build();
        }
    }

    @Override
    public void resolve(DeserializationContext deserializationContext) throws JsonMappingException {
        ((ResolvableDeserializer) defaultDeserializer).resolve(deserializationContext);
    }

}
