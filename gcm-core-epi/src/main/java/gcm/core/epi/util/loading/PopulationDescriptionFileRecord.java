package gcm.core.epi.util.loading;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gcm.core.epi.util.loading.ImmutablePopulationDescriptionFileRecord;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePopulationDescriptionFileRecord.class)
@JsonDeserialize(as = ImmutablePopulationDescriptionFileRecord.class)
@JsonPropertyOrder
public interface PopulationDescriptionFileRecord {

    Integer age();

    String homeId();

    String schoolId();

    String workplaceId();

}
