package gcm.core.epi.trigger;

import plugins.gcm.agents.Environment;
import plugins.regions.support.RegionId;
import plugins.regions.support.RegionPropertyId;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TriggerUtils {

    public static boolean checkIfTriggerIsInEffect(Environment environment,
                                                   RegionId regionId,
                                                   RegionPropertyId triggerStartProperty,
                                                   RegionPropertyId triggerEndProperty) {
        boolean triggerHasStarted = environment.getRegionPropertyValue(regionId, triggerStartProperty);
        double triggerStartTime = environment.getRegionPropertyTime(regionId, triggerStartProperty);
        boolean triggerHasEnded = environment.getRegionPropertyValue(regionId,
                triggerEndProperty);
        double triggerEndTime = environment.getRegionPropertyTime(regionId,
                triggerEndProperty);
        return triggerHasStarted && !(triggerHasEnded && triggerEndTime >= triggerStartTime);
    }

    public static void mergeCallbacks(Map<String, Set<TriggerCallback>> into, Map<String, Set<TriggerCallback>> from) {
        from.forEach(
                (trigger, callbacks) -> {
                    Set<TriggerCallback> existingCallbacks = into.computeIfAbsent(trigger,
                            key -> new HashSet<>());
                    existingCallbacks.addAll(callbacks);
                }
        );
    }

    public static void addCallback(Map<String, Set<TriggerCallback>> into, String triggerId, TriggerCallback triggerCallback) {
        into.computeIfAbsent(triggerId, x -> new HashSet<>()).add(triggerCallback);
    }

    public static void addBooleanCallback(Map<String, Set<TriggerCallback>> into, String triggerId, RegionPropertyId regionPropertyId) {
        into.computeIfAbsent(triggerId, x -> new HashSet<>()).add((env, regionId) ->
                env.setRegionPropertyValue(regionId, regionPropertyId, true));
    }

}
