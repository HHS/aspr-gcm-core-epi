package gcm.core.epi.util.loading;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import gcm.core.epi.trigger.FipsCode;

public class FipsCodeStringMapDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(String s, DeserializationContext deserializationContext) {
        return FipsCode.of(s);
    }

}
