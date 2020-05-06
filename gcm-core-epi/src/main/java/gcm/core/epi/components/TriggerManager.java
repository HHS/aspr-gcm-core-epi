package gcm.core.epi.components;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.behavior.BehaviorPlugin;
import gcm.core.epi.trigger.Trigger;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerContainer;
import gcm.core.epi.trigger.TriggerId;
import gcm.simulation.Environment;

import java.util.*;
import java.util.stream.Collectors;

public class TriggerManager extends AbstractComponent {

    @Override
    public void init(Environment environment) {
        TriggerContainer triggerContainer = environment.getGlobalPropertyValue(GlobalProperty.TRIGGER_CONTAINER);
        Map<Trigger, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        Optional<BehaviorPlugin> behaviorPlugin = environment.getGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN);

        // Get triggers and properties from each behavior plugin
        if (behaviorPlugin.isPresent()) {
            Map<String, Set<TriggerCallback>> triggerCallbacksForPlugin = behaviorPlugin.get().getTriggerCallbacks(environment);
            for (Map.Entry<String, Set<TriggerCallback>> triggerCallbackEntry : triggerCallbacksForPlugin.entrySet()) {
                String triggerName = triggerCallbackEntry.getKey();
                // Empty string means 'never'
                if (!triggerName.equals("")) {
                    TriggerId<Trigger> triggerId = triggerContainer.getId(triggerName);
                    if (triggerId != null) {
                        Set<TriggerCallback> triggerCallbackSet = getTriggerCallbacksAndInitialize(environment,
                                triggerCallbacks, triggerId);
                        triggerCallbackSet.addAll(triggerCallbackEntry.getValue());
                    } else {
                        throw new RuntimeException("TriggerManager cannot find a trigger defined with the name: '" +
                                triggerName + "'");
                    }
                }
            }
            environment.setGlobalPropertyValue(GlobalProperty.TRIGGER_CALLBACKS, triggerCallbacks);
        }

        // Handle triggers that start after other triggers
        Set<Trigger> triggersToAdd = triggerCallbacks.keySet().stream()
                .filter(x -> x.startingTriggers().size() > 0)
                .collect(Collectors.toSet());

        // TODO: Need to deal with self-referential triggers and loop garbage like that
        while (triggersToAdd.size() > 0) {
            Trigger trigger = triggersToAdd.iterator().next();
            for (String id : trigger.startingTriggers()) {
                TriggerId<Trigger> startingTriggerId = triggerContainer.getId(id);
                Set<TriggerCallback> startingTriggerCallbacks = getTriggerCallbacksAndInitialize(environment,
                        triggerCallbacks, startingTriggerId);
                //noinspection OptionalGetWithoutIsPresent
                startingTriggerCallbacks.add((env, regionId) -> env.setRegionPropertyValue(regionId, trigger.triggeringRegionProperty().get(), true));
                triggersToAdd.remove(trigger);
                Trigger startingTrigger = triggerContainer.get(startingTriggerId);
                if (startingTrigger.startingTriggers().size() > 0) {
                    triggersToAdd.add(startingTrigger);
                }
            }
        }

    }

    Set<TriggerCallback> getTriggerCallbacksAndInitialize(Environment environment,
                                                          Map<Trigger, Set<TriggerCallback>> triggerCallbacks,
                                                          TriggerId<Trigger> triggerId) {
        return triggerCallbacks.computeIfAbsent(
                triggerId.trigger(),
                trigger -> {
                    // Generate a new global component for this trigger
                    environment.addGlobalComponent(triggerId, triggerId.trigger().triggerComponent());
                    return new HashSet<>();
                }
        );
    }

}
