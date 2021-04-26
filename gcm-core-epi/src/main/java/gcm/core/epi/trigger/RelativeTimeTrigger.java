package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.components.trigger.RelativeTimeTriggerComponent;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsScope;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import nucleus.AgentContext;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

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

    public Consumer<AgentContext> triggerInit() {
        return new RelativeTimeTriggerComponent()::init;
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
    public TypedPropertyDefinition getPropertyDefinition() {
        return TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build();
    }

}
