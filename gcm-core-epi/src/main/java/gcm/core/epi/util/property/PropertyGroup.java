package gcm.core.epi.util.property;

import gcm.scenario.GlobalPropertyId;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
public interface PropertyGroup extends GlobalPropertyId {

    static PropertyGroup of(String name) {
        return ImmutablePropertyGroup.of(name);
    }

    @Value.Parameter
    String name();

}
