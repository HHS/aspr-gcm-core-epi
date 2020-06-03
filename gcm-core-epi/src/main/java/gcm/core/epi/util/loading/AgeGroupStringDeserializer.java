package gcm.core.epi.util.loading;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;

import java.io.IOException;

public class AgeGroupStringDeserializer extends JsonDeserializer<AgeGroup> {

    private final AgeGroupPartition ageGroupPartition;

    public AgeGroupStringDeserializer(AgeGroupPartition ageGroupPartition) {
        this.ageGroupPartition = ageGroupPartition;
    }

    @Override
    public AgeGroup deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        AgeGroup ageGroup = ageGroupPartition.getAgeGroupFromName(jsonParser.getText());
        if (ageGroup == null) {
            throw new IOException(jsonParser.getText() + " is not a valid AgeGroup name");
        }
        return ageGroup;
    }

}
