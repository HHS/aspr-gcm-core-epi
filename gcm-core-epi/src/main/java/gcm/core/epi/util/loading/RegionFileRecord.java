package gcm.core.epi.util.loading;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRegionFileRecord.class)
@JsonDeserialize(as = ImmutableRegionFileRecord.class)
@JsonPropertyOrder
public interface RegionFileRecord {

    String id();

    double lon();

    double lat();

    int population();

}
