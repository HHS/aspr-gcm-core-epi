package gcm.core.epi.test.manual.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonDeserialize(as = ImmutableMapWrapper.class)
public abstract class MapWrapper {

    @Value.Parameter
    abstract Map<Integer, Double> value();

}
