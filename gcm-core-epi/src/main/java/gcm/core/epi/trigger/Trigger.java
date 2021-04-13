package gcm.core.epi.trigger;

import gcm.core.epi.reports.TriggerReport;
import gcm.core.epi.util.property.DefinedRegionProperty;
import plugins.components.agents.Component;
import plugins.gcm.agents.Environment;
import plugins.regions.support.RegionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Trigger {

    static void performCallback(TriggerId<? extends Trigger> triggerId, TriggerCallback callback, Environment environment, RegionId regionId) {
        if (environment.isActiveReport(TriggerReport.class)) {
            environment.releaseOutput(TriggerReport.getReportItemFor(environment.getTime(), triggerId, regionId));
        }
        callback.trigger(environment, regionId);
    }

    Class<? extends Component> triggerComponent();

    default List<String> startingTriggers() {
        return new ArrayList<>();
    }

    default Optional<DefinedRegionProperty> triggeringRegionProperty() {
        return Optional.empty();
    }

}
