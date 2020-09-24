package gcm.core.epi.util.loading;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class EmptyStringListDeserializer extends JsonDeserializer<List<?>> implements ContextualDeserializer,
        ResolvableDeserializer {

    private final JsonDeserializer<?> defaultDeserializer;
    private JavaType valueType;

    public EmptyStringListDeserializer(JsonDeserializer<?> defaultDeserializer) {
        this.defaultDeserializer = defaultDeserializer;
    }

    @Override
    public List<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // Return empty strings as a List<?> where ? is parsed from the empty string
        if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING && jsonParser.getText().equals("")) {
            return Arrays.asList(deserializationContext.findRootValueDeserializer(valueType).deserialize(jsonParser, deserializationContext));
        } else {
            return (List<?>) defaultDeserializer.deserialize(jsonParser, deserializationContext);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) throws JsonMappingException {
        valueType = deserializationContext.getContextualType().containedType(0);
        return this;
    }

    @Override
    public void resolve(DeserializationContext deserializationContext) throws JsonMappingException {
        ((ResolvableDeserializer) defaultDeserializer).resolve(deserializationContext);
    }
}
