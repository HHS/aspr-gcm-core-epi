package gcm.core.epi.reports;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.output.reports.AbstractReport;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.scenario.GroupId;
import gcm.scenario.PersonId;
import gcm.simulation.ObservableEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupMembershipReport extends AbstractReport {

    private ReportHeader reportHeader;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
            reportHeader = reportHeaderBuilder
                    .add("GroupId")
                    .add("GroupType")
                    .add("PersonId")
                    .build();
        }
        return reportHeader;
    }

    @Override
    public void close(ObservableEnvironment observableEnvironment) {
        for (ContactGroupType contactGroupType : ContactGroupType.values()) {
            List<GroupId> groupIds = observableEnvironment.getGroupsForGroupType(contactGroupType);
            for (GroupId groupId : groupIds) {
                List<PersonId> people = observableEnvironment.getPeopleForGroup(groupId);
                for (PersonId personId : people) {
                    ReportItem.Builder reportItemBuilder = ReportItem.builder();
                    reportItemBuilder.setReportType(getClass());
                    reportItemBuilder.setReportHeader(getReportHeader());
                    reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                    reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());

                    reportItemBuilder.addValue(groupId);
                    reportItemBuilder.addValue(contactGroupType);
                    reportItemBuilder.addValue(personId);

                    observableEnvironment.releaseOutputItem(reportItemBuilder.build());
                }
            }
        }

    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        return new HashSet<>();
    }

}
