package gcm.core.epi.trigger;

import org.immutables.value.Value;
import plugins.globals.support.GlobalComponentId;

@Value.Immutable
@Value.Style(allParameters = true, defaults = @Value.Immutable(builder = false))
public interface TriggerId<T extends Trigger> extends GlobalComponentId {

    String id();

    T trigger();

}
