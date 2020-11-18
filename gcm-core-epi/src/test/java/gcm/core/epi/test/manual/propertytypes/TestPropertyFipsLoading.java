package gcm.core.epi.test.manual.propertytypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gcm.core.epi.identifiers.StringRegionId;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.ImmutableAgeGroup;
import gcm.core.epi.population.ImmutableAgeGroupPartition;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import gcm.core.epi.propertytypes.FipsCodeValue;
import gcm.core.epi.propertytypes.FipsCodeValueDeserializerModifier;
import gcm.core.epi.util.loading.AgeGroupStringMapDeserializer;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import gcm.core.epi.util.loading.FipsCodeStringMapDeserializer;
import gcm.core.epi.util.property.DefinedGlobalAndRegionProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPropertyFipsLoading {

    private static final String SIMPLE_INPUT = "PROPERTY_A:\n" +
            "  - 1.0\n" +
            "  - 2.0";

    private static final String FIPS_INPUT = "PROPERTY_A:\n" +
            "  - scope: COUNTY\n" +
            "    values:\n" +
            "      00001: 3.0\n" +
            "    defaultValue: 1.0\n" +
            "  - scope: COUNTY\n" +
            "    defaultValue: 2.0";

    private static final String SIMPLE_MAP_INPUT = "PROPERTY_B:\n" +
            "  - A: 1.0\n" +
            "    B: 2.0\n" +
            "  - A: 3.0\n" +
            "    B: 4.0";

    private static final String FIPS_MAP_INPUT = "PROPERTY_B:\n" +
            "  - scope: COUNTY\n" +
            "    values:\n" +
            "      00001:\n" +
            "        A: 1.0\n" +
            "        B: 2.0\n" +
            "    defaultValue:\n" +
            "      A: 3.0\n" +
            "  - scope: COUNTY\n" +
            "    defaultValue:\n" +
            "      B: 4.0";

    private static final DefinedGlobalAndRegionProperty PROPERTY_A = new DefinedGlobalAndRegionProperty() {

        @Override
        public String toString() {
            return "PROPERTY_A";
        }

        @Override
        public DefinedRegionProperty getRegionProperty() {
            return null;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return TypedPropertyDefinition.builder().type(Double.class).defaultValue(0.0).build();
        }
    };

    private static final DefinedGlobalAndRegionProperty PROPERTY_B = new DefinedGlobalAndRegionProperty() {

        @Override
        public String toString() {
            return "PROPERTY_B";
        }

        @Override
        public DefinedRegionProperty getRegionProperty() {
            return null;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return TypedPropertyDefinition.builder()
                    .typeReference(new TypeReference<Map<String, Double>>() {})
                    .defaultValue(new HashMap<String, Double>()).build();
        }
    };

    @Test
    public void test() {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        // Create module for deserializing special objects from Strings for use in map keys during property loading
        SimpleModule deserializationModule = new SimpleModule();
        deserializationModule.addKeyDeserializer(FipsCode.class, new FipsCodeStringMapDeserializer());
        // Create AgeGroupPartition
        AgeGroupPartition ageGroupPartition = ImmutableAgeGroupPartition.builder()
                .addAgeGroupList(ImmutableAgeGroup.builder().name("A").maxAge(10).build())
                .addAgeGroupList(ImmutableAgeGroup.builder().name("B").minAge(11).build())
                .build();
        deserializationModule.addKeyDeserializer(AgeGroup.class, new AgeGroupStringMapDeserializer(ageGroupPartition));
        // TODO
        //deserializationModule.addDeserializer(ImmutableFipsCodeValue.class, new FipsCodeValueDeserializer(defaultDeserializer));
        deserializationModule.setDeserializerModifier(new FipsCodeValueDeserializerModifier());
        // Register module in objectMapper
        objectMapper.registerModule(deserializationModule);

        ObjectReader initialReader = objectMapper.readerFor(new TypeReference<Map<String, List<JsonNode>>>() {});

        try {
            Map<String, List<JsonNode>> initialParsedSimpleInput = initialReader.readValue(SIMPLE_INPUT);
            ObjectReader simpleParser = objectMapper.readerFor(Double.class);
            //Double firstDoubleValue = simpleParser.readValue(initialParsedSimpleInput.get(PROPERTY_A.toString()).get(0));
            Double firstDoubleValue = (Double) CoreEpiBootstrapUtil.getPropertyValueFromJson(
                    initialParsedSimpleInput.get(PROPERTY_A.toString()).get(0), PROPERTY_A, null);
            Double secondDoubleValue = simpleParser.readValue(initialParsedSimpleInput.get(PROPERTY_A.toString()).get(1));

            assertEquals(firstDoubleValue, 1.0, 1e-16);
            assertEquals(secondDoubleValue, 2.0, 1e-16);

            Map<String, List<JsonNode>> initialParsedFipsInput = initialReader.readValue(FIPS_INPUT);
            ObjectReader fipsParser = objectMapper.readerFor(FipsCodeDouble.class);
            FipsCodeDouble firstFipsCodeDouble = fipsParser.readValue(initialParsedFipsInput.get(PROPERTY_A.toString()).get(0));
            FipsCodeDouble secondFipsCodeDouble = fipsParser.readValue(initialParsedFipsInput.get(PROPERTY_A.toString()).get(1));

            assertEquals(firstFipsCodeDouble.defaultValue(), 1.0, 1e-16);
            assertEquals(firstFipsCodeDouble.values().get(FipsCode.of("00001")), 3.0, 1e-16);
            assertEquals(secondFipsCodeDouble.defaultValue(), 2.0, 1e-16);

            CoreEpiBootstrapUtil.getPropertyValueFromJson(initialParsedSimpleInput.get(PROPERTY_A.toString()).get(0),
                    PROPERTY_A, null);

            Map<String, List<JsonNode>> initialParsedSimpleMapInput = initialReader.readValue(SIMPLE_MAP_INPUT);
            ObjectReader simpleMapParser = objectMapper.readerFor(new TypeReference<Map<AgeGroup, Double>>() { });
            Map<AgeGroup, Double> firstMapValue = simpleMapParser.readValue(initialParsedSimpleMapInput.get(PROPERTY_B.toString()).get(0));
            Map<AgeGroup, Double> secondMapValue = simpleMapParser.readValue(initialParsedSimpleMapInput.get(PROPERTY_B.toString()).get(1));
//                    (Map<AgeGroup, Double>) CoreEpiBootstrapUtil.getPropertyValueFromJson(
//                    initialParsedSimpleMapInput.get(PROPERTY_B.toString()).get(1), PROPERTY_B, ageGroupPartition);

            assertEquals(firstMapValue.get(ageGroupPartition.getAgeGroupFromAge(5)), 1.0, 1e-16);
            assertEquals(firstMapValue.get(ageGroupPartition.getAgeGroupFromAge(15)), 2.0, 1e-16);
            assertEquals(secondMapValue.get(ageGroupPartition.getAgeGroupFromAge(5)), 3.0, 1e-16);
            assertEquals(secondMapValue.get(ageGroupPartition.getAgeGroupFromAge(15)), 4.0, 1e-16);

            Map<String, List<JsonNode>> initialParsedFipsMapInput = initialReader.readValue(FIPS_MAP_INPUT);
            ObjectReader fipsMapParser = objectMapper.readerFor(new TypeReference<FipsCodeValue<Map<AgeGroup, Double>>>() { });
            FipsCodeValue<Map<AgeGroup, Double>> firstFipsCodeValue = fipsMapParser.readValue(initialParsedFipsMapInput.get(PROPERTY_B.toString()).get(0));

            assertEquals(firstFipsCodeValue.getValue(StringRegionId.of("00001000000")).get(ageGroupPartition.getAgeGroupFromAge(5)), 1.0, 1e-16);
            assertEquals(firstFipsCodeValue.getValue(StringRegionId.of("00001000000")).get(ageGroupPartition.getAgeGroupFromAge(15)), 2.0, 1e-16);
            assertEquals(firstFipsCodeValue.getValue(StringRegionId.of("00002000000")).get(ageGroupPartition.getAgeGroupFromAge(5)), 3.0, 1e-16);

            firstFipsCodeValue = fipsMapParser.readValue(initialParsedSimpleMapInput.get(PROPERTY_B.toString()).get(0));
            assertEquals(firstFipsCodeValue.getValue(StringRegionId.of("00001000000")).get(ageGroupPartition.getAgeGroupFromAge(5)), 1.0, 1e-16);
            assertEquals(firstFipsCodeValue.getValue(StringRegionId.of("00001000000")).get(ageGroupPartition.getAgeGroupFromAge(15)), 2.0, 1e-16);
            assertEquals(firstFipsCodeValue.getValue(StringRegionId.of("00002000000")).get(ageGroupPartition.getAgeGroupFromAge(5)), 1.0, 1e-16);
            assertEquals(firstFipsCodeValue.getValue(StringRegionId.of("00002000000")).get(ageGroupPartition.getAgeGroupFromAge(15)), 2.0, 1e-16);

            FipsCodeValue<Map<AgeGroup, Double>> secondFipsCodeValue = fipsMapParser.readValue(initialParsedSimpleMapInput.get(PROPERTY_B.toString()).get(1));
            assertEquals(secondFipsCodeValue.getValue(StringRegionId.of("00001000000")).get(ageGroupPartition.getAgeGroupFromAge(5)), 3.0, 1e-16);
            assertEquals(secondFipsCodeValue.getValue(StringRegionId.of("00001000000")).get(ageGroupPartition.getAgeGroupFromAge(15)), 4.0, 1e-16);
            assertEquals(secondFipsCodeValue.getValue(StringRegionId.of("00002000000")).get(ageGroupPartition.getAgeGroupFromAge(5)), 3.0, 1e-16);
            assertEquals(secondFipsCodeValue.getValue(StringRegionId.of("00002000000")).get(ageGroupPartition.getAgeGroupFromAge(15)), 4.0, 1e-16);

            firstFipsCodeDouble = fipsParser.readValue(initialParsedSimpleInput.get(PROPERTY_A.toString()).get(0));
            assertEquals(firstFipsCodeDouble.defaultValue(), 1.0, 1e-16);
            secondFipsCodeDouble = fipsParser.readValue(initialParsedSimpleInput.get(PROPERTY_A.toString()).get(1));
            assertEquals(secondFipsCodeDouble.defaultValue(), 2.0, 1e-16);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
