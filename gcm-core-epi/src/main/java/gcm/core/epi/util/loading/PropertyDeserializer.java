package gcm.core.epi.util.loading;

import com.fasterxml.jackson.core.type.TypeReference;

public class PropertyDeserializer {

    private final TypeReference<?> typeReference;

    public PropertyDeserializer(TypeReference<?> typeReference) {
        this.typeReference = typeReference;
    }

    TypeReference<?> getTypeReference() {
        return typeReference;
    }

}
