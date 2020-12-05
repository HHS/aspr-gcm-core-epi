package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

import java.util.List;

public class CombinedDeserializerModifier extends BeanDeserializerModifier {

    private final List<BeanDeserializerModifier> modifiers;

    public CombinedDeserializerModifier(List<BeanDeserializerModifier> modifiers) {
        this.modifiers = modifiers;
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        for (BeanDeserializerModifier modifier : modifiers) {
            deserializer = modifier.modifyDeserializer(config, beanDesc, deserializer);
        }
        return deserializer;
    }

}
