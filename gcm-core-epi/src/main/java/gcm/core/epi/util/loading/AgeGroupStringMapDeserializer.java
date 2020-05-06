package gcm.core.epi.util.loading;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;

import java.io.IOException;

public class AgeGroupStringMapDeserializer extends KeyDeserializer {

    private final AgeGroupPartition ageGroupPartition;

    public AgeGroupStringMapDeserializer(AgeGroupPartition ageGroupPartition) {
        this.ageGroupPartition = ageGroupPartition;
    }

    @Override
    public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
        AgeGroup ageGroup = ageGroupPartition.getAgeGroupFromName(s);
        if (ageGroup == null) {
            throw new IOException("jsonParser.getText() is not a valid AgeGroup name");
        }
        return ageGroup;
    }

}
