package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.TypeFactory;
import gcm.core.epi.population.AgeGroup;

import java.io.IOException;
import java.util.Map;

public class AgeWeightsDeserializer extends JsonDeserializer<AgeWeights> implements
        ResolvableDeserializer {

    private final JsonDeserializer<?> defaultDeserializer;

    public AgeWeightsDeserializer(JsonDeserializer<?> deserializer) {
        this.defaultDeserializer = deserializer;
    }

    @Override
    public AgeWeights deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // Store json to use for second-round parsing
        JsonNode jsonNode = jsonParser.readValueAsTree();
        ObjectCodec codec = jsonParser.getCodec();
        // If the value is a double then use this as the default value
        if (jsonNode.isDouble()) {
            return ImmutableAgeWeights.builder().defaultValue(jsonNode.doubleValue()).build();
        }
        // Otherwise try to parse it as a full AgeWeights object
        try {
            jsonParser = new TreeTraversingParser(jsonNode);
            jsonParser.setCodec(codec);
            jsonParser.nextToken();
            return (ImmutableAgeWeights) defaultDeserializer.deserialize(jsonParser, deserializationContext);
        } catch (IOException e) {
            // Try to parse it as a Map<AgeGroup, Double>
            jsonParser = new TreeTraversingParser(jsonNode);
            jsonParser.setCodec(codec);
            jsonParser.nextToken();
            TypeFactory typeFactory = deserializationContext.getTypeFactory();
            // Map<AgeGroup, Double> type
            JavaType javaType = typeFactory.constructType(new TypeReference<Map<AgeGroup, Double>>() {
            });
            //noinspection unchecked
            Map<AgeGroup, Double> valuesMap = (Map<AgeGroup, Double>) deserializationContext.findRootValueDeserializer(javaType).deserialize(jsonParser, deserializationContext);
            return ImmutableAgeWeights.builder().putAllValues(valuesMap).build();
        }
    }

    @Override
    public void resolve(DeserializationContext deserializationContext) throws JsonMappingException {
        ((ResolvableDeserializer) defaultDeserializer).resolve(deserializationContext);
    }

}
