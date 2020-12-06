package gcm.core.epi.trigger;

import gcm.components.Component;
import gcm.core.epi.reports.TriggerReport;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Trigger {

    static void performCallback(TriggerId<? extends Trigger> triggerId, TriggerCallback callback, Environment environment, RegionId regionId) {
        if (environment.isActiveReport(TriggerReport.class)) {
            environment.releaseOutputItem(TriggerReport.getReportItemFor(environment.getObservableEnvironment(), triggerId, regionId));
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
