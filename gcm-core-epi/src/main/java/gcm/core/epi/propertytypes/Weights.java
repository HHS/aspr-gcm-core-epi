package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableWeights.class)
public abstract class Weights<T> extends AbstractWeights<T> {

}
