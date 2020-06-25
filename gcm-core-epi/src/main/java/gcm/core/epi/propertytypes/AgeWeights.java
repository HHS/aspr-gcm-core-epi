package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.population.AgeGroup;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableAgeWeights.class)
public abstract class AgeWeights extends Weights<AgeGroup> {

}
