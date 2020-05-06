package gcm.core.epi.components.trigger;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.trigger.*;
import gcm.scenario.RegionId;
import gcm.scenario.RegionPropertyId;
import gcm.simulation.Environment;

import java.util.Map;
import java.util.Set;

public class CompoundTriggerComponent extends AbstractComponent {

    CompoundTrigger trigger;
    Set<TriggerCallback> triggerCallbacks;

    @Override
    public void init(Environment environment) {
        TriggerId<CompoundTrigger> componentId = environment.getCurrentComponentId();
        TriggerContainer triggerContainer = environment.getGlobalPropertyValue(GlobalProperty.TRIGGER_CONTAINER);
        trigger = triggerContainer.get(componentId);

        Map<Trigger, Set<TriggerCallback>> triggersCallbacks = environment.getGlobalPropertyValue(
                GlobalProperty.TRIGGER_CALLBACKS);
        triggerCallbacks = triggersCallbacks.get(trigger);

        //noinspection OptionalGetWithoutIsPresent
        environment.observeGlobalRegionPropertyChange(true, trigger.triggeringRegionProperty().get());
    }

    @Override
    public void observeRegionPropertyChange(Environment environment, RegionId regionId, RegionPropertyId regionPropertyId) {
        for (TriggerCallback triggerCallback : triggerCallbacks) {
            triggerCallback.trigger(environment, regionId);
        }
    }

}
