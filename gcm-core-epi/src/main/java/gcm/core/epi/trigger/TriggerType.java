package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.JsonNode;

public enum TriggerType {

    TIME((builder, id, yamlData) -> {
        // TODO consider paradigm of loading
    }, AbsoluteTimeTrigger.class),

    RELATIVE_TIME((builder, id, yamlData) -> {
        // TODO consider paradigm of loading
    }, RelativeTimeTrigger.class),

    CUMULATIVE((builder, id, yamlData) -> {
        // TODO consider paradigm of loading
    }, CumulativeTrigger.class),

    INCIDENCE((builder, id, yamlData) -> {
        // TODO consider paradigm of loading
    }, IncidenceTrigger.class),

    COMPOUND((builder, id, yamlData) -> {
        // TODO consider paradigm of loading
    }, CompoundTrigger.class);

    private final TriggerLoader loader;
    private final Class<? extends Trigger> triggerClass;

    TriggerType(TriggerLoader loader, Class<? extends Trigger> triggerClass) {
        this.loader = loader;
        this.triggerClass = triggerClass;
    }

    public void load(TriggerContainer.Builder builder, String id, JsonNode yamlData) {
        loader.load(builder, id, yamlData);
    }

    public Class<? extends Trigger> getTriggerClass() {
        return triggerClass;
    }

}
