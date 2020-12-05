package gcm.core.epi.plugin;

import gcm.core.epi.propertytypes.FipsCodeValue;
import gcm.core.epi.trigger.TriggerId;
import gcm.simulation.Environment;

// Used to reject invalid property values with runtime exceptions
@FunctionalInterface
public interface TriggerOverrideValidator {

    void validate(Environment environment, TriggerId<?> triggerId, FipsCodeValue<?> value) throws RuntimeException;

}
