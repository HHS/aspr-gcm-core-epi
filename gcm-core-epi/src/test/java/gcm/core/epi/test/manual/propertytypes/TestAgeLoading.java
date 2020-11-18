package gcm.core.epi.test.manual.propertytypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.ImmutableAgeGroup;
import gcm.core.epi.population.ImmutableAgeGroupPartition;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.WeightsDeserializerModifier;
import gcm.core.epi.util.loading.AgeGroupStringMapDeserializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAgeLoading {

    @Test
    public void test() throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        AgeGroupPartition ageGroupPartition = ImmutableAgeGroupPartition.builder()
                .addAgeGroupList(ImmutableAgeGroup.builder().name("A").maxAge(10).build())
                .addAgeGroupList(ImmutableAgeGroup.builder().name("B").minAge(11).build())
                .build();

        SimpleModule deserializationModule = new SimpleModule();
        deserializationModule.addKeyDeserializer(AgeGroup.class, new AgeGroupStringMapDeserializer(ageGroupPartition));
        deserializationModule.setDeserializerModifier(new WeightsDeserializerModifier());
        mapper.registerModule(deserializationModule);

        AgeWeights ageWeights;

        ageWeights = mapper.readerFor(AgeWeights.class).readValue("{\"values\": {\"A\": 3.0}, \"defaultValue\": 1.0}");
        assertEquals(ageWeights.getWeight(ageGroupPartition.getAgeGroupFromName("A")), 3.0, 1e-16);
        assertEquals(ageWeights.getWeight(ageGroupPartition.getAgeGroupFromName("B")), 1.0, 1e-16);

        ageWeights = mapper.readerFor(AgeWeights.class).readValue("1.0");
        assertEquals(ageWeights.getWeight(ageGroupPartition.getAgeGroupFromName("A")), 1.0, 1e-16);
        assertEquals(ageWeights.getWeight(ageGroupPartition.getAgeGroupFromName("B")), 1.0, 1e-16);

        ageWeights = mapper.readerFor(AgeWeights.class).readValue("{\"A\":3.0, \"B\":2.0}");
        assertEquals(ageWeights.getWeight(ageGroupPartition.getAgeGroupFromName("A")), 3.0, 1e-16);
        assertEquals(ageWeights.getWeight(ageGroupPartition.getAgeGroupFromName("B")), 2.0, 1e-16);

    }

}
