package gcm.core.epi.util.loading;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableRegionWorkFlowFileRecord.class)
public interface RegionWorkFlowFileRecord {

    String targetRegionId();

    String sourceRegionId();

    double outflowFraction();

    double inflowFraction();

}
