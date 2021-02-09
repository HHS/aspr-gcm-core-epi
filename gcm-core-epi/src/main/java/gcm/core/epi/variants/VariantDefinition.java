package gcm.core.epi.variants;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableVariantDefinition.class)
public abstract class VariantDefinition {

    /*
        The relative probability that a person infected with this strain will transmit infection
     */
    @Value.Default
    public double relativeTransmissibility() {
        return 1.0;
    }

    /*
        The relative probability that a person infected with this strain will become hospitalized or die
     */
    @Value.Default
    public double relativeSeverity() {
        return 1.0;
    }

}
