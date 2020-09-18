package gcm.core.epi.test.manual.propertytypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.ImmutableAgeGroup;
import gcm.core.epi.population.ImmutableAgeGroupPartition;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.Weights;
import gcm.core.epi.propertytypes.WeightsDeserializerModifier;
import gcm.core.epi.util.loading.AgeGroupStringMapDeserializer;
import org.junit.Test;

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

        //mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        AgeWeights ageWeights;
        
        ageWeights = mapper.readerFor(AgeWeights.class).readValue("{\"values\": {\"A\": 3.0}, \"defaultValue\": 1.0}");
        System.out.println(ageWeights);
        System.out.println(ageWeights.values().containsKey(ageGroupPartition.getAgeGroupFromName("A")));
        System.out.println(ageWeights.getWeight(ageGroupPartition.getAgeGroupFromName("B")));

        ageWeights = mapper.readerFor(AgeWeights.class).readValue("1.0");
        System.out.println(ageWeights);

        // Will throw exception
        ageWeights = mapper.readerFor(AgeWeights.class).readValue("{\"A\":3.0, \"B\":2.0}");
        System.out.println(ageWeights);

//        ageWeights = mapper.readerFor(AgeWeights.class).readValue("{\"A\":3.0, \"defaultValue\": 1.0}");
//        System.out.println(ageWeights);

//        DoubleWrapper doubleWrapper = mapper.readerFor(DoubleWrapper.class).readValue("1.0");
//        System.out.println(doubleWrapper);
//
//        // Will throw exception
//        MapWrapper mapWrapper = mapper.readerFor(MapWrapper.class).readValue("{\"1\": 2.0}");
//        System.out.println(mapWrapper);

    }

}
