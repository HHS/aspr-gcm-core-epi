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

public class TestVaccineAdministratorId {

    @Test
    public void test() {
        VaccineAdministratorId vaccineAdministratorId1 = VaccineAdministratorId.of("A");
        VaccineAdministratorId vaccineAdministratorId2 = VaccineAdministratorId.of("B");
        assertTrue(vaccineAdministratorId1.ordinal() == 1);
        assertTrue(vaccineAdministratorId2.ordinal() == 2);
        assertTrue(VaccineAdministratorId.ordinal("A") == 1);
        assertTrue(VaccineAdministratorId.ordinal("B") == 2);
    }

    @Test
    public void testDeserialize() throws JsonProcessingException {
        String vaccineAdministratorListString = "- \"A\"\n" +
                "- \"B\"\n" +
                "- \"C\"\n";

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        List<VaccineAdministratorId> vaccineIdList = objectMapper.readValue(vaccineAdministratorListString,
                new TypeReference<List<VaccineAdministratorId>>() {
        });
        System.out.println(vaccineIdList);
    }

}
