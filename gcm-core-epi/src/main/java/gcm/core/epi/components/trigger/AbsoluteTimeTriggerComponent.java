package gcm.core.epi.components.trigger;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.trigger.*;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AbsoluteTimeTriggerComponent extends AbstractComponent {

    Set<TriggerCallback> triggerCallbacks;

    @Override
    public void init(Environment environment) {

        TriggerId<AbsoluteTimeTrigger> componentId = environment.getCurrentComponentId();
        TriggerContainer triggerContainer = environment.getGlobalPropertyValue(GlobalProperty.TRIGGER_CONTAINER);
        Map<Trigger, Set<TriggerCallback>> triggersCallbacks = environment.getGlobalPropertyValue(
                GlobalProperty.TRIGGER_CALLBACKS);
        AbsoluteTimeTrigger trigger = triggerContainer.get(componentId);
        triggerCallbacks = triggersCallbacks.get(trigger);

        // Determine if this will repeat
        int repeats = trigger.repeats();
        double repeatInterval = trigger.repeatInterval();
        double repeatInitialPause = trigger.repeatInitialPause();
        if (repeats < 0) {
            throw new IllegalArgumentException("Number of repeats must be non-negative");
        }
        if (repeatInterval <= 0) {
            throw new IllegalArgumentException("Repeat interval must be positive");
        }
        List<Double> repeatOffsetTimes = new ArrayList<>();
        repeatOffsetTimes.add(0.0);
        if (repeats > 0 & repeatInterval < Double.POSITIVE_INFINITY) {
            repeatOffsetTimes.addAll(
                    IntStream.range(1, repeats + 1)
                            .mapToObj(x -> (double) (x - 1) * repeatInterval + (x > 0 ? repeatInitialPause : 0.0))
                            .collect(Collectors.toList())
            );
        }

        // Schedule events
        Set<RegionId> regionIds = environment.getRegionIds();
        for (RegionId regionId : regionIds) {
            FipsCode regionScopedFipsCode = trigger.scope().getFipsCode(regionId);
            double triggerTime = trigger.times().getOrDefault(regionScopedFipsCode,
                    trigger.defaultTime());
            Plan togglePlan = new ToggleRegionPropertyPlan(regionId);
            for (double repeatOffsetTime : repeatOffsetTimes) {
                environment.addPlan(togglePlan, repeatOffsetTime + triggerTime);
            }
        }

    }

    @Override
    public void executePlan(Environment environment, Plan plan) {
        ToggleRegionPropertyPlan toggleRegionPropertyPlan = (ToggleRegionPropertyPlan) plan;
        RegionId regionId = toggleRegionPropertyPlan.regionId;
        // Trigger callbacks
        for (TriggerCallback callback : triggerCallbacks) {
            callback.trigger(environment, regionId);
        }
    }

    protected static class ToggleRegionPropertyPlan implements Plan {
        final RegionId regionId;

        public ToggleRegionPropertyPlan(RegionId regionId) {
            this.regionId = regionId;
        }
    }

}
