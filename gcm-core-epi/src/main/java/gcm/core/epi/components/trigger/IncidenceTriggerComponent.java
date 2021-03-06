package gcm.core.epi.components.trigger;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.trigger.*;
import gcm.scenario.*;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IncidenceTriggerComponent extends AbstractComponent {

    private final Set<TriggerCallback> triggerCallbacks = new HashSet<>();
    private final Map<FipsCode, Double> thresholds = new HashMap<>();
    private final Map<FipsCode, Counter> counterMap = new HashMap<>();
    private final Map<FipsCode, Boolean> triggerActive = new HashMap<>();
    Map<FipsCode, Set<RegionId>> fipsCodeRegionMap;
    private IncidenceTrigger incidenceTrigger;
    private boolean flushPlanExists;

    @Override
    public void init(Environment environment) {
        TriggerId<IncidenceTrigger> componentId = environment.getCurrentComponentId();
        TriggerContainer triggerContainer = environment.getGlobalPropertyValue(GlobalProperty.TRIGGER_CONTAINER);
        Map<Trigger, Set<TriggerCallback>> triggersCallbacks = environment.getGlobalPropertyValue(
                GlobalProperty.TRIGGER_CALLBACKS);
        incidenceTrigger = triggerContainer.get(componentId);
        // Store properties
        triggerCallbacks.addAll(triggersCallbacks.get(incidenceTrigger));
        // Initialize
        initializeCountersAndObservations(environment);
        // Store thresholds
        thresholds.putAll(incidenceTrigger.getFipsCodeValues(environment));
    }

    private void initializeCountersAndObservations(Environment environment) {
        FipsScope scope = incidenceTrigger.scope();
        // Initialize counters
        Set<FipsCode> fipsCodes = environment.getRegionIds().stream()
                .map(scope::getFipsCode)
                .collect(Collectors.toSet());
        for (FipsCode fipsCode : fipsCodes) {
            counterMap.put(fipsCode, new Counter());
        }

        // Register to observe events
        switch (incidenceTrigger.metric()) {
            case INFECTIONS:
                environment.observeCompartmentPersonArrival(true, Compartment.INFECTED);
                break;
            case CASES:
                environment.observeGlobalPersonPropertyChange(true, PersonProperty.IS_SYMPTOMATIC);
                break;
            case HOSPITALIZATIONS:
                environment.observeGlobalPersonResourceChange(true, Resource.HOSPITAL_BED);
                environment.observeGlobalPersonPropertyChange(true, PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED);
                break;
            case DEATHS:
                environment.observeGlobalPersonPropertyChange(true, PersonProperty.IS_DEAD);
                break;
            default:
                throw new RuntimeException("Unhandled cumulative trigger type");
        }

        // Initialize FIPS code region map
        fipsCodeRegionMap = incidenceTrigger.scope().getFipsCodeRegionMap(environment);

        // Determine if trigger is already active
        for (FipsCode fipsCode : fipsCodeRegionMap.keySet()) {
            if (incidenceTrigger.comparison() == IncidenceTrigger.Comparison.BELOW) {
                triggerRegionProperties(environment, fipsCode);
            } else {
                triggerActive.put(fipsCode, false);
            }
        }

        // Wait to schedule a flush plan until needed
        flushPlanExists = false;
    }

    @Override
    public void observeCompartmentPersonArrival(Environment environment, PersonId personId) {
        CompartmentId compartment = environment.getPersonCompartment(personId);
        if (compartment == Compartment.INFECTED) {
            RegionId regionId = environment.getPersonRegion(personId);
            handleIncrement(environment, regionId);
        } else {
            throw new RuntimeException("Trigger Manager observed unexpected person compartment change");
        }
    }

    @Override
    public void observePersonPropertyChange(Environment environment, PersonId personId, PersonPropertyId personPropertyId) {
        if (personPropertyId == PersonProperty.IS_SYMPTOMATIC) {
            RegionId regionId = environment.getPersonRegion(personId);
            boolean isSymptomatic = environment.getPersonPropertyValue(personId, personPropertyId);
            if (isSymptomatic) {
                handleIncrement(environment, regionId);
            }
        } else if (personPropertyId == PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED) {
            RegionId regionId = environment.getPersonRegion(personId);
            handleIncrement(environment, regionId);
        } else if (personPropertyId == PersonProperty.IS_DEAD) {
            RegionId regionId = environment.getPersonRegion(personId);
            handleIncrement(environment, regionId);
        } else {
            throw new RuntimeException("Trigger Manager observed change for unexpected person property: " +
                    personPropertyId);
        }
    }

    @Override
    public void observePersonResourceChange(Environment environment, PersonId personId, ResourceId resourceId) {
        if (resourceId == Resource.HOSPITAL_BED) {
            RegionId regionId = environment.getPersonRegion(personId);
            if (environment.getPersonResourceLevel(personId, resourceId) > 0) {
                handleIncrement(environment, regionId);
            }
        } else {
            throw new RuntimeException("Trigger Manager observed unexpected person resource change");
        }
    }

    private void handleIncrement(Environment environment, RegionId personRegionId) {
        if (!flushPlanExists) {
            addFlushPlan(environment);
        }
        FipsCode fipsCode = incidenceTrigger.scope().getFipsCode(personRegionId);
        Counter counter = counterMap.get(fipsCode);
        if (counter != null) {
            counter.count++;
        }
    }

    @Override
    public void executePlan(Environment environment, Plan plan) {
        // Only called for FlushPlan
        flush(environment);
    }

    private void flush(Environment environment) {
        double time = environment.getTime();
        if (time >= incidenceTrigger.start() && time < incidenceTrigger.end()) {
            // Check if we have crossed the threshold for each FIPS code
            for (Map.Entry<FipsCode, Counter> entry : counterMap.entrySet()) {
                FipsCode fipsCode = entry.getKey();
                Counter counter = entry.getValue();
                double threshold = thresholds.get(fipsCode);
                switch (incidenceTrigger.comparison()) {
                    case ABOVE:
                        if (counter.count > threshold && !triggerActive.get(fipsCode)) {
                            // Trigger should be activated
                            triggerRegionProperties(environment, fipsCode);
                        } else if (counter.count <= threshold && triggerActive.get(fipsCode)) {
                            triggerActive.put(fipsCode, false);
                        }
                        break;
                    case BELOW:
                        if (counter.count < threshold && !triggerActive.get(fipsCode)) {
                            // Trigger should be activated
                            triggerRegionProperties(environment, fipsCode);
                        } else if (counter.count >= threshold && triggerActive.get(fipsCode)) {
                            triggerActive.put(fipsCode, false);
                        }
                        break;
                    default:
                        throw new RuntimeException("Unhandled Incidence Trigger Comparison");
                }
            }
        }
        // Reset counters
        for (Counter counter : counterMap.values()) {
            counter.count = 0;
        }
        flushPlanExists = false;
    }

    private void addFlushPlan(Environment environment) {
        double time = environment.getTime();
        double intervalsElapsed = Math.floor(time / incidenceTrigger.interval());
        environment.addPlan(new FlushPlan(), (intervalsElapsed + 1) * incidenceTrigger.interval());
        flushPlanExists = true;
    }

    private void triggerRegionProperties(Environment environment, FipsCode fipsCode) {
        for (RegionId regionId : fipsCodeRegionMap.get(fipsCode)) {
            // Trigger callbacks
            for (TriggerCallback callback : triggerCallbacks) {
                callback.trigger(environment, regionId);
            }
        }
        triggerActive.put(fipsCode, true);
    }

    private final static class Counter {
        int count;
    }

    private static class FlushPlan implements Plan {

    }

}
