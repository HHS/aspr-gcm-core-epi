package gcm.core.epi.test.manual.vaccine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gcm.core.epi.plugin.vaccine.resourcebased.VaccineId;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class TestVaccineId {

    @Test
    void test() throws JsonProcessingException {
        String mapTest = "A: B";
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Map<VaccineId, String> map = objectMapper.readValue(mapTest, new TypeReference<Map<VaccineId, String>>() {
        });
        System.out.println(map);
    }

}
