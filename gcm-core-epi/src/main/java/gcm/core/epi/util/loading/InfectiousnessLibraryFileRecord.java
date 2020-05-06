package gcm.core.epi.util.loading;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gcm.core.epi.util.loading.ImmutableInfectiousnessLibraryFileRecord;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableInfectiousnessLibraryFileRecord.class)
@JsonDeserialize(as = ImmutableInfectiousnessLibraryFileRecord.class)
@JsonPropertyOrder
public interface InfectiousnessLibraryFileRecord {

    int id();

    double time();

    double value();

}
