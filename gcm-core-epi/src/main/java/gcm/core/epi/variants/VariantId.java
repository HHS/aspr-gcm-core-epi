package gcm.core.epi.variants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.plugin.vaccine.resourcebased.ImmutableVaccineId;
import gcm.scenario.ResourceId;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@JsonDeserialize(as = ImmutableVariantId.class)
public interface VariantId {

    @JsonCreator
    static VariantId of(String id) {
        return ImmutableVariantId.of(id);
    }

    @Value.Parameter
    String id();

}
