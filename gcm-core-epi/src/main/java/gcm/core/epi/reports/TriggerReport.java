package gcm.core.epi.reports;

import gcm.core.epi.trigger.Trigger;
import gcm.core.epi.trigger.TriggerId;
import gcm.output.reports.AbstractReport;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.scenario.RegionId;
import gcm.simulation.ObservableEnvironment;

import java.util.HashSet;
import java.util.Set;

public class TriggerReport extends AbstractReport {

    private static final ReportHeader reportHeader = ReportHeader.builder()
            .add("Time")
            .add("Trigger")
            .add("Region")
            .build();

    public static ReportItem getReportItemFor(ObservableEnvironment environment, TriggerId<? extends Trigger> triggerId, RegionId regionId) {
        return ReportItem.builder()
                .setReportHeader(reportHeader)
                .setReportType(TriggerReport.class)
                .setScenarioId(environment.getScenarioId())
                .setReplicationId(environment.getReplicationId())
                .addValue(environment.getTime())
                .addValue(triggerId.id())
                .addValue(regionId)
                .build();
    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        return new HashSet<>();
    }

}
