package gcm.core.epi.util.loading;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.util.loading.ImmutableAgeGroupFileRecord;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableAgeGroupFileRecord.class)
public interface AgeGroupFileRecord {

    String name();

    int maxAge();

}
