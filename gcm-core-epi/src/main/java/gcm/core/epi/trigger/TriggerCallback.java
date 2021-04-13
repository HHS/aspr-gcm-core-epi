package gcm.core.epi.trigger;

import plugins.gcm.agents.Environment;
import plugins.regions.support.RegionId;

public interface TriggerCallback {

    /**
     * Callback that is executed when a trigger has fired in response to a regional event
     *
     * @param environment The simulation environment
     * @param regionId    The region id for the region in which the event has occurred
     */
    void trigger(Environment environment, RegionId regionId);

}
