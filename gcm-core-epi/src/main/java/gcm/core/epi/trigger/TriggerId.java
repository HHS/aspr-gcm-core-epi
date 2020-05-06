package gcm.core.epi.trigger;

import gcm.scenario.GlobalComponentId;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(allParameters = true, defaults = @Value.Immutable(builder = false))
public interface TriggerId<T extends Trigger> extends GlobalComponentId {

    String id();

    T trigger();

}
