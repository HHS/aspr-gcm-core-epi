package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.components.Component;
import gcm.core.epi.components.trigger.RelativeTimeTriggerComponent;
import gcm.core.epi.trigger.ImmutableRelativeTimeTrigger;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.PropertyDefinition;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(as = ImmutableRelativeTimeTrigger.class)
public abstract class RelativeTimeTrigger implements Trigger, DefinedRegionProperty {

    @Value.Default
    public FipsScope scope() {
        return FipsScope.NATION;
    }

    public abstract String start();

    public abstract Map<FipsCode, Double> times();

    public abstract double defaultTime();

    public Class<? extends Component> triggerComponent() {
        return RelativeTimeTriggerComponent.class;
    }

    @Override
    public List<String> startingTriggers() {
        List<String> startingTriggers = new ArrayList<>();
        startingTriggers.add(start());
        return startingTriggers;
    }

    @Override
    public Optional<DefinedRegionProperty> triggeringRegionProperty() {
        return Optional.of(this);
    }

    @Override
    public PropertyDefinition getPropertyDefinition() {
        return PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build();
    }

}
