package gcm.core.epi.plugin.behavior;

import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.PropertyDefinition;
import gcm.simulation.Environment;

import java.util.*;

public class TriggerTestBehaviorPlugin extends BehaviorPlugin {

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(TriggerTestGlobalProperty.class));
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        return new HashSet<>(EnumSet.allOf(TriggerTestRegionProperty.class));
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        // Trigger 1
        String triggerId = environment.getGlobalPropertyValue(TriggerTestGlobalProperty.START_TRIGGER_ONE);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, TriggerTestRegionProperty.TRIGGER_CONDITION_ONE_START);
        triggerId = environment.getGlobalPropertyValue(TriggerTestGlobalProperty.END_TRIGGER_ONE);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, TriggerTestRegionProperty.TRIGGER_CONDITION_ONE_END);
        // Trigger 2
        triggerId = environment.getGlobalPropertyValue(TriggerTestGlobalProperty.START_TRIGGER_TWO);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, TriggerTestRegionProperty.TRIGGER_CONDITION_TWO_START);
        triggerId = environment.getGlobalPropertyValue(TriggerTestGlobalProperty.END_TRIGGER_TWO);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, TriggerTestRegionProperty.TRIGGER_CONDITION_TWO_END);
        return triggerCallbacks;
    }

    public enum TriggerTestGlobalProperty implements DefinedGlobalProperty {

        START_TRIGGER_ONE(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        END_TRIGGER_ONE(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        START_TRIGGER_TWO(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        END_TRIGGER_TWO(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;

        TriggerTestGlobalProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

    }

    public enum TriggerTestRegionProperty implements DefinedRegionProperty {

        TRIGGER_CONDITION_ONE_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),
        TRIGGER_CONDITION_ONE_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),
        TRIGGER_CONDITION_TWO_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build()),
        TRIGGER_CONDITION_TWO_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false).build());

        private final PropertyDefinition propertyDefinition;

        TriggerTestRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

}
