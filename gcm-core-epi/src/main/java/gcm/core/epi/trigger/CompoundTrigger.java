package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.components.Component;
import gcm.core.epi.components.trigger.CompoundTriggerComponent;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.PropertyDefinition;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(as = ImmutableCompoundTrigger.class)
public abstract class CompoundTrigger implements Trigger, DefinedRegionProperty {

    public abstract List<String> triggers();

    public Class<? extends Component> triggerComponent() {
        return CompoundTriggerComponent.class;
    }

    @Override
    public List<String> startingTriggers() {
        return triggers();
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
