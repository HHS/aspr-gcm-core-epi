package gcm.core.epi.test.manual.vaccine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gcm.core.epi.plugin.vaccine.resourcebased.VaccineAdministratorId;
import gcm.core.epi.plugin.vaccine.resourcebased.VaccineId;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import gcm.core.epi.util.loading.FipsCodeStringMapDeserializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestVaccineId {

    @Test
    public void test() {
        VaccineId vaccineId1 = VaccineId.of("Vaccine A");
        VaccineId vaccineId2 = VaccineId.of("Vaccine B");
        assertTrue(vaccineId1.ordinal() == 1);
        assertTrue(vaccineId2.ordinal() == 2);
        assertTrue(VaccineId.ordinal("Vaccine A") == 1);
        assertTrue(VaccineId.ordinal("Vaccine B") == 2);
    }

    @Test
    public void testDeserialize() throws JsonProcessingException {
        String vaccineListString = "- \"Vaccine A\"\n" +
                "- \"Vaccine B\"\n" +
                "- \"Vaccine C\"\n";

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        List<VaccineId> vaccineIdList = objectMapper.readValue(vaccineListString, new TypeReference<List<VaccineId>>() {
        });
        System.out.println(vaccineIdList);
    }

    @Test
    public void testDeserializeMapDouble() throws JsonProcessingException {
        String vaccineListString = "A: 1.0\n" +
                "B: 2.0\n" +
                "C: 3.0\n";

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Map<VaccineId, Double> vaccineIdMap = objectMapper.readValue(vaccineListString,
                new TypeReference<Map<VaccineId, Double>>() {
                });
        System.out.println(vaccineIdMap);
    }

    @Test
    public void testDeserializeMapFipsCodeDouble() throws JsonProcessingException {
        String vaccineListString = "A: \n" +
                "  type: FRACTION\n" +
                "  scope: COUNTY\n" +
                "  values:\n" +
                "    23005: 1.0\n" +
                "    23031: 1.0\n" +
                "    23003: 1.0";

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        SimpleModule deserializationModule = new SimpleModule();
        deserializationModule.addKeyDeserializer(FipsCode.class, new FipsCodeStringMapDeserializer());
        objectMapper.registerModule(deserializationModule);

        Map<VaccineId, FipsCodeDouble> vaccineIdMap = objectMapper.readValue(vaccineListString,
                new TypeReference<Map<VaccineId, FipsCodeDouble>>() {
                });
        System.out.println(vaccineIdMap);
    }

    @Test
    public void testDeserializeVaccineDeliveries() throws JsonProcessingException {
        String vaccineListString =
                "0.0: \n" +
                        "  A: \n" +
                        "    type: FRACTION\n" +
                        "    scope: COUNTY\n" +
                        "    values:\n" +
                        "      23005: 1.0\n" +
                        "      23031: 1.0\n" +
                        "      23003: 1.0\n" +
                        "30.0: \n" +
                        "  A: \n" +
                        "    type: FRACTION\n" +
                        "    defaultValue: 1.0";

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        SimpleModule deserializationModule = new SimpleModule();
        deserializationModule.addKeyDeserializer(FipsCode.class, new FipsCodeStringMapDeserializer());
        objectMapper.registerModule(deserializationModule);

        Map<Double, Map<VaccineId, FipsCodeDouble>> vaccineIdMap = objectMapper.readValue(vaccineListString,
                new TypeReference<Map<Double, Map<VaccineId, FipsCodeDouble>>>() {
                });
        System.out.println(vaccineIdMap);
    }

    @Test
    public void testDeserializeVaccineDeliveriesJson() throws JsonProcessingException {
        String vaccineListString =
                "{0.0={Vaccine A={type=FRACTION, scope=COUNTY, values={23005=1.0, 23031=1.0, 23003=1.0}, defaultValue=0.0}}, " +
                "30.0={Vaccine A={type=FRACTION, defaultValue=1.0}}}";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule deserializationModule = new SimpleModule();
        deserializationModule.addKeyDeserializer(FipsCode.class, new FipsCodeStringMapDeserializer());
        objectMapper.registerModule(deserializationModule);

        Map<Double, Map<VaccineId, FipsCodeDouble>> vaccineIdMap = objectMapper.readValue(vaccineListString,
                new TypeReference<Map<Double, Map<VaccineId, FipsCodeDouble>>>() {
                });
        System.out.println(vaccineIdMap);
    }

}
