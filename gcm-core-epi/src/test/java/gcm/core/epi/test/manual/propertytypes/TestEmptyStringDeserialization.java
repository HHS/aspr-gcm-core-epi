package gcm.core.epi.test.manual.propertytypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gcm.core.epi.util.loading.*;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestEmptyStringDeserialization {

    private static final String EMPTY_STRING = "''";
    private static final String EMPTY_STRING_VALUE = "A: ''";
    private static final String NONEMPTY_STRING_VALUE = "A: 'Test'";

    @Test
    public void test() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(ImmutablePropertyValueJsonList.class, new PropertyValueJsonListDeserializer());
        objectMapper.registerModule(module);

        PropertyValueJsonList simpleValue = objectMapper.readValue(EMPTY_STRING, new TypeReference<PropertyValueJsonList>() {
        });
        assertEquals(simpleValue.jsonNodeList().get(0).asText(), "");

        Map<String, PropertyValueJsonList> value = objectMapper.readValue(EMPTY_STRING_VALUE, new TypeReference<Map<String, PropertyValueJsonList>>() {
        });
        assertEquals(value.get("A").jsonNodeList().get(0).asText(), "");

        value = objectMapper.readValue(NONEMPTY_STRING_VALUE, new TypeReference<Map<String, PropertyValueJsonList>>() {
        });
        assertEquals(value.get("A").jsonNodeList().get(0).asText(), "Test");

    }

}
