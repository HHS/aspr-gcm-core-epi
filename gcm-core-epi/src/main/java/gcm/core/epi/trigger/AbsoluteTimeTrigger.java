package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.components.Component;
import gcm.core.epi.components.trigger.AbsoluteTimeTriggerComponent;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsScope;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonDeserialize(as = ImmutableAbsoluteTimeTrigger.class)
public abstract class AbsoluteTimeTrigger implements Trigger {

    @Value.Default
    public FipsScope scope() {
        return FipsScope.NATION;
    }

    public abstract Map<FipsCode, Double> times();

    public abstract double defaultTime();

    @Value.Default
    public int repeats() {
        return 0;
    }
    // TODO: Validate repeats >= 0

    @Value.Default
    public double repeatInterval() {
        return Double.POSITIVE_INFINITY;
    }

    @Value.Default
    public double repeatInitialPause() {
        return 0.0;
    }

    public Class<? extends Component> triggerComponent() {
        return AbsoluteTimeTriggerComponent.class;
    }

}
