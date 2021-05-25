package gcm.core.epi.reports;

import gcm.core.epi.trigger.Trigger;
import gcm.core.epi.trigger.TriggerId;
import plugins.regions.support.RegionId;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;

public class TriggerReport {

    private static final ReportHeader reportHeader = ReportHeader.builder()
            .add("Time")
            .add("Trigger")
            .add("Region")
            .build();

    public static ReportItem getReportItemFor(double time, TriggerId<? extends Trigger> triggerId, RegionId regionId) {
        return ReportItem.builder()
                .setReportHeader(reportHeader)
                .setReportId(CustomReport.TRIGGER_REPORT)
                .addValue(time)
                .addValue(triggerId.id())
                .addValue(regionId)
                .build();
    }

}
