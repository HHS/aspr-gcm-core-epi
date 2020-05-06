package gcm.core.epi.trigger;

import gcm.scenario.RegionId;
import gcm.simulation.Environment;

public interface TriggerCallback {

    /**
     * Callback that is executed when a trigger has fired in response to a regional event
     *
     * @param environment The simulation environment
     * @param regionId    The region id for the region in which the event has occurred
     */
    void trigger(Environment environment, RegionId regionId);

}
