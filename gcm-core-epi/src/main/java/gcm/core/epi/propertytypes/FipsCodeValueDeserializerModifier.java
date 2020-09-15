package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

public class FipsCodeValueDeserializerModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        if (beanDesc.getBeanClass() == ImmutableFipsCodeValue.class) {
            return new FipsCodeValueDeserializer(deserializer);
        } else if (beanDesc.getBeanClass() == ImmutableFipsCodeDouble.class) {
            return new FipsCodeDoubleDeserializer(deserializer);
        } else {
            return deserializer;
        }
    }

}
