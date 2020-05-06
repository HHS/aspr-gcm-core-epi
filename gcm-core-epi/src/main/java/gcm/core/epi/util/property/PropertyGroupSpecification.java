package gcm.core.epi.util.property;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.util.property.ImmutablePropertyGroupSpecification;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = ImmutablePropertyGroupSpecification.class)
public interface PropertyGroupSpecification {

    String name();

    List<String> properties();

    List<String> labels();

}
