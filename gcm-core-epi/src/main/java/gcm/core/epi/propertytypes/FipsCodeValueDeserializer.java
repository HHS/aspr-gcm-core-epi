package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

public class FipsCodeValueDeserializer extends JsonDeserializer<ImmutableFipsCodeValue<?>> implements
        ContextualDeserializer,
        ResolvableDeserializer {

    private final JsonDeserializer<?> defaultDeserializer;
    private JavaType valueType;

    public FipsCodeValueDeserializer(JsonDeserializer<?> defaultDeserializer) {
        this.defaultDeserializer = defaultDeserializer;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) throws JsonMappingException {
        valueType = deserializationContext.getContextualType().containedType(0);
        return this;
    }

    @Override
    public ImmutableFipsCodeValue<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // Store json to use for second-round parsing
        JsonNode jsonNode = jsonParser.readValueAsTree();
        try {
            jsonParser = new TreeTraversingParser(jsonNode);
            jsonParser.nextToken();
            return (ImmutableFipsCodeValue<?>) defaultDeserializer.deserialize(jsonParser, deserializationContext);
        } catch (IOException e) {
            jsonParser = new TreeTraversingParser(jsonNode);
            jsonParser.nextToken();
            return ImmutableFipsCodeValue.builder().defaultValue(deserializationContext.findRootValueDeserializer(valueType).deserialize(jsonParser, deserializationContext)).build();
        }
    }

    @Override
    public void resolve(DeserializationContext deserializationContext) throws JsonMappingException {
        ((ResolvableDeserializer) defaultDeserializer).resolve(deserializationContext);
    }

}
