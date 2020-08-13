package gcm.core.epi.components.trigger;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.trigger.*;
import gcm.scenario.RegionId;
import gcm.scenario.RegionPropertyId;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

import java.util.Map;
import java.util.Set;

public class RelativeTimeTriggerComponent extends AbsoluteTimeTriggerComponent {

    private RelativeTimeTrigger trigger;

    @Override
    public void init(Environment environment) {
        TriggerId<RelativeTimeTrigger> componentId = environment.getCurrentComponentId();
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
        FipsCode regionScopedFipsCode = trigger.scope().getFipsSubCode(regionId);
        double startTime = environment.getTime() + trigger.times().getOrDefault(regionScopedFipsCode, trigger.defaultTime());
        Plan startPlan = new ToggleRegionPropertyPlan(regionId);
        environment.addPlan(startPlan, startTime);
    }

}
