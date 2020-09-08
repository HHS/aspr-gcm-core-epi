package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

public class FipsCodeValueDeserializer extends JsonDeserializer<ImmutableFipsCodeValue<?>> implements ContextualDeserializer {

    //private DeserializationContext deserializationContext;
    private JavaType valueType;

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) throws JsonMappingException {
        //this.deserializationContext = deserializationContext;
        valueType = deserializationContext.getContextualType().containedType(0);
        return this;
    }

    @Override
    public ImmutableFipsCodeValue<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        deserializationContext.findRootValueDeserializer(deserializationContext.constructType(ImmutableFipsCodeValue.class));
        return null;
    }

}
