package gcm.core.epi.util.loading;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePopulationDescriptionFileRecord.class)
@JsonDeserialize(as = ImmutablePopulationDescriptionFileRecord.class)
@JsonPropertyOrder
public interface PopulationDescriptionFileRecord {

    Integer age();

    String homeId();

    Optional<String> schoolId();

    Optional<String> workplaceId();

}
