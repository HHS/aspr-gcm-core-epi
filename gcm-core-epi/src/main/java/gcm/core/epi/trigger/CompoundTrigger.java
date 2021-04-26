package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.components.trigger.CompoundTriggerComponent;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import nucleus.AgentContext;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Value.Immutable
@JsonDeserialize(as = ImmutableCompoundTrigger.class)
public abstract class CompoundTrigger implements Trigger, DefinedRegionProperty {

    public abstract List<String> triggers();

    public Consumer<AgentContext> triggerInit() {
        return new CompoundTriggerComponent()::init;
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
    public TypedPropertyDefinition getPropertyDefinition() {
        return TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build();
    }

}
