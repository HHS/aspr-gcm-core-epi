package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.components.trigger.CumulativeTriggerComponent;
import gcm.core.epi.propertytypes.AbstractFipsCodeDouble;
import org.immutables.value.Value;
import plugins.components.agents.Component;

@Value.Immutable
@JsonDeserialize(as = ImmutableCumulativeTrigger.class)
public abstract class CumulativeTrigger extends AbstractFipsCodeDouble implements Trigger {

    public abstract Metric metric();

    public Class<? extends Component> triggerComponent() {
        return CumulativeTriggerComponent.class;
    }

    public enum Metric {
        INFECTIONS,
        CASES,
        HOSPITALIZATIONS,
        DEATHS
    }

}
