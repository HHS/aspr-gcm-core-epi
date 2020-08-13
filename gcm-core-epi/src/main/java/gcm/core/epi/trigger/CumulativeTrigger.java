package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.components.Component;
import gcm.core.epi.components.trigger.CumulativeTriggerComponent;
import gcm.core.epi.propertytypes.AbstractFipsCodeValues;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableCumulativeTrigger.class)
public abstract class CumulativeTrigger extends AbstractFipsCodeValues implements Trigger {

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
