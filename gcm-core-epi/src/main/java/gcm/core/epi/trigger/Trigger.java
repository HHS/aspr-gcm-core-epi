package gcm.core.epi.trigger;

import gcm.components.Component;
import gcm.core.epi.util.property.DefinedRegionProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Trigger {

    Class<? extends Component> triggerComponent();

    default List<String> startingTriggers() {
        return new ArrayList<>();
    }

    default Optional<DefinedRegionProperty> triggeringRegionProperty() {
        return Optional.empty();
    }

}
