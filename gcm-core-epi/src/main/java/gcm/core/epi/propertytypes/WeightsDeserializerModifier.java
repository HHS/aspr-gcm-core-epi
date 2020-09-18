package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.type.TypeFactory;
import gcm.core.epi.population.AgeGroup;

public class WeightsDeserializerModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        if (beanDesc.getBeanClass() == ImmutableWeights.class) {
            return new WeightsDeserializer(deserializer);
        }  if (beanDesc.getBeanClass() == ImmutableAgeWeights.class) {
            return new AgeWeightsDeserializer(deserializer);
        } else {
            return deserializer;
        }
    }

}
