package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import plugins.resources.support.ResourceId;

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
