package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.components.Component;
import gcm.core.epi.components.trigger.IncidenceTriggerComponent;
import gcm.core.epi.trigger.ImmutableIncidenceTrigger;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableIncidenceTrigger.class)
public abstract class IncidenceTrigger extends AbstractFipsCodeValues implements Trigger {

    @Value.Default
    public FipsScope scope() {
        return FipsScope.NATION;
    }

    public abstract Metric metric();

    public abstract double interval();

    public abstract Comparison comparison();

    @Value.Default
    public double start() {
        return 0.0;
    }

    @Value.Default
    public double end() {
        return Double.POSITIVE_INFINITY;
    }

    public Class<? extends Component> triggerComponent() {
        return IncidenceTriggerComponent.class;
    }

    public enum Metric {
        INFECTIONS,
        CASES,
        HOSPITALIZATIONS,
        DEATHS
    }

    public enum Comparison {
        BELOW,
        ABOVE
    }

}
