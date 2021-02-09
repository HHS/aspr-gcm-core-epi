package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.scenario.ResourceId;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@JsonDeserialize(as = ImmutableVaccineId.class)
public interface VaccineId extends ResourceId {

    @JsonCreator
    static VaccineId of(String id) {
        return ImmutableVaccineId.of(id);
    }

    @Value.Parameter
    String id();

}
