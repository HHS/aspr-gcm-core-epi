package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.Map;

public class WeightsDeserializer extends JsonDeserializer<Weights<?>> implements
        ContextualDeserializer,
        ResolvableDeserializer {

    private final JsonDeserializer<?> defaultDeserializer;
    private JavaType keyType;

    public WeightsDeserializer(JsonDeserializer<?> deserializer) {
        this.defaultDeserializer = deserializer;
    }

    @Override
    public Weights<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // Store json to use for second-round parsing
        JsonNode jsonNode = jsonParser.readValueAsTree();
        ObjectCodec codec = jsonParser.getCodec();
        // If the value is a double then use this as the default value
        if (jsonNode.isDouble()) {
            return ImmutableWeights.builder().defaultValue(jsonNode.doubleValue()).build();
        }
        // Otherwise try to parse it as a Weights<T>
        try {
            jsonParser = new TreeTraversingParser(jsonNode);
            jsonParser.setCodec(codec);
            jsonParser.nextToken();
            return (ImmutableWeights<?>) defaultDeserializer.deserialize(jsonParser, deserializationContext);
        } catch (IOException e) {
            // Try to parse it as a Map<T, Double>
            jsonParser = new TreeTraversingParser(jsonNode);
            jsonParser.setCodec(codec);
            jsonParser.nextToken();
            TypeFactory typeFactory = deserializationContext.getTypeFactory();
            // Map<T, Double> type
            JavaType javaType = typeFactory.constructMapType(Map.class, keyType, typeFactory.constructSimpleType(Double.class, null));
            //noinspection unchecked
            Map<?, Double> valuesMap = (Map<?, Double>) deserializationContext.findRootValueDeserializer(javaType).deserialize(jsonParser, deserializationContext);
            return ImmutableWeights.builder().putAllValues(valuesMap).build();
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) {
        keyType = deserializationContext.getContextualType().containedType(0);
        return this;
    }

    @Override
    public void resolve(DeserializationContext deserializationContext) throws JsonMappingException {
        ((ResolvableDeserializer) defaultDeserializer).resolve(deserializationContext);
    }

}
