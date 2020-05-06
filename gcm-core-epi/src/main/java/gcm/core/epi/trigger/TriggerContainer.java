package gcm.core.epi.trigger;

import java.util.HashMap;
import java.util.Map;

public class TriggerContainer {

    private static final Map<TriggerId<? extends Trigger>, Object> triggerData = new HashMap<>();
    private static final Map<String, TriggerId<?>> triggerIdMap = new HashMap<>();

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unchecked")
    public <T extends Trigger> T get(TriggerId<T> triggerId) {
        return (T) triggerData.get(triggerId);
    }

    @SuppressWarnings("unchecked")
    public <T extends Trigger> TriggerId<T> getId(String id) {
        return (TriggerId<T>) triggerIdMap.get(id);
    }

    public static class Builder {

        private TriggerContainer triggerContainer = new TriggerContainer();

        private Builder() {

        }

        public TriggerContainer build() {
            try {
                return triggerContainer;
            } finally {
                triggerContainer = new TriggerContainer();
            }
        }

        public <T extends Trigger> Builder addTrigger(TriggerId<T> triggerId, T trigger) {
            triggerData.put(triggerId, trigger);
            triggerIdMap.put(triggerId.id(), triggerId);
            return this;
        }

    }

}
