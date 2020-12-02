package gcm.core.epi.components.trigger;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsScope;
import gcm.core.epi.trigger.*;
import gcm.scenario.*;
import gcm.simulation.Environment;

import java.util.*;
import java.util.stream.Collectors;


public class CumulativeTriggerComponent extends AbstractComponent {

    private final Set<TriggerCallback> triggerCallbacks = new HashSet<>();
    private final Map<FipsCode, Double> thresholds = new HashMap<>();
    private final Map<FipsCode, Counter> counterMap = new HashMap<>();
    private CumulativeTrigger cumulativeTrigger;

    @Override
    public void init(Environment environment) {
        TriggerId<CumulativeTrigger> componentId = environment.getCurrentComponentId();
        TriggerContainer triggerContainer = environment.getGlobalPropertyValue(GlobalProperty.TRIGGER_CONTAINER);
        Map<Trigger, Set<TriggerCallback>> triggersCallbacks = environment.getGlobalPropertyValue(
                GlobalProperty.TRIGGER_CALLBACKS);
        cumulativeTrigger = triggerContainer.get(componentId);
        // Initialize
        initializeCountersAndObservations(environment);
        // Store callbacks
        triggerCallbacks.addAll(triggersCallbacks.get(cumulativeTrigger));
        // Store thresholds
        thresholds.putAll(cumulativeTrigger.getFipsCodeValues(environment));
    }

    private void initializeCountersAndObservations(Environment environment) {
        FipsScope scope = cumulativeTrigger.scope();
        // Initialize counters
        Set<FipsCode> fipsCodes = environment.getRegionIds().stream()
                .map(scope::getFipsSubCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (FipsCode fipsCode : fipsCodes) {
            counterMap.put(fipsCode, new Counter());
        }

        // Register to observe events
        switch (cumulativeTrigger.metric()) {
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
        FipsCode fipsCode = cumulativeTrigger.scope().getFipsSubCode(personRegionId);
        Counter counter = counterMap.get(fipsCode);
        if (counter != null) {
            counter.count++;
            // Check if we have crossed the threshold
            double threshold = thresholds.get(fipsCode);
            if (counter.count >= threshold) {
                TriggerId<CompoundTrigger> componentId = environment.getCurrentComponentId();
                for (RegionId regionId : getRegionIdsInScope(environment, personRegionId)) {
                    // Trigger callbacks
                    for (TriggerCallback callback : triggerCallbacks) {
                        Trigger.performCallback(componentId, callback, environment, regionId);
                    }
                }
                counterMap.remove(fipsCode);
            }
        }
    }

    private Set<RegionId> getRegionIdsInScope(Environment environment, RegionId regionId) {
        FipsScope scope = cumulativeTrigger.scope();
        FipsCode regionIdFipsCode = scope.getFipsSubCode(regionId);
        return environment.getRegionIds().stream()
                .filter(x -> scope.getFipsSubCode(x).equals(regionIdFipsCode))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private final static class Counter {
        int count;
    }

}
