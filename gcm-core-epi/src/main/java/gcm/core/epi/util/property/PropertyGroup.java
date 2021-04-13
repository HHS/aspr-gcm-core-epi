package gcm.core.epi.util.property;

import org.immutables.value.Value;
import plugins.globals.support.GlobalPropertyId;

@Value.Immutable(builder = false)
public interface PropertyGroup extends GlobalPropertyId {

    static PropertyGroup of(String name) {
        return ImmutablePropertyGroup.of(name);
    }

    @Value.Parameter
    String name();

}
