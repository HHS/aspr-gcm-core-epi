package gcm.core.epi.test.manual.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableDoubleWrapper.class)
public abstract class DoubleWrapper {

    @Value.Parameter
    abstract double value();

}
