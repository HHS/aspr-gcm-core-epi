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

    private static final ReportHeader reportHeader = getReportHeader();

    private static ReportHeader getReportHeader() {
        ReportHeader.ReportHeaderBuilder reportHeaderBuilder = new ReportHeader.ReportHeaderBuilder();
        reportHeaderBuilder.add("Time");
        reportHeaderBuilder.add("Trigger");
        reportHeaderBuilder.add("Region");
        return reportHeaderBuilder.build();
    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        return new HashSet<>();
    }

    public static ReportItem getReportItemFor(ObservableEnvironment environment, TriggerId<? extends Trigger> triggerId, RegionId regionId) {
        final ReportItem.ReportItemBuilder reportItemBuilder = new ReportItem.ReportItemBuilder();
        reportItemBuilder.setReportHeader(reportHeader);
        reportItemBuilder.setReportType(TriggerReport.class);
        reportItemBuilder.setScenarioId(environment.getScenarioId());
        reportItemBuilder.setReplicationId(environment.getReplicationId());
        reportItemBuilder.addValue(environment.getTime());
        reportItemBuilder.addValue(triggerId.id());
        reportItemBuilder.addValue(regionId);
        return  reportItemBuilder.build();
    }

}
