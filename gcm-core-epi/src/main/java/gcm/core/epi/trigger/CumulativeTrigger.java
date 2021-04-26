package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.components.trigger.CumulativeTriggerComponent;
import gcm.core.epi.propertytypes.AbstractFipsCodeDouble;
import nucleus.AgentContext;
import org.immutables.value.Value;

import java.util.function.Consumer;

@Value.Immutable
@JsonDeserialize(as = ImmutableCumulativeTrigger.class)
public abstract class CumulativeTrigger extends AbstractFipsCodeDouble implements Trigger {

    public abstract Metric metric();

    public Consumer<AgentContext> triggerInit() {
        return new CumulativeTriggerComponent()::init;
    }

    public enum Metric {
        INFECTIONS,
        CASES,
        HOSPITALIZATIONS,
        DEATHS
    }

}
