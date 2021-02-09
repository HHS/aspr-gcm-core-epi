package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@JsonDeserialize(as = ImmutableVaccineAdministratorId.class)
public interface VaccineAdministratorId {

    @JsonCreator
    static VaccineAdministratorId of(String id) {
        return ImmutableVaccineAdministratorId.of(id);
    }

    @Value.Parameter
    String id();

}
