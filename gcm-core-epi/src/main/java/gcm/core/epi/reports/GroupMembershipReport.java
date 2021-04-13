package gcm.core.epi.reports;

import gcm.core.epi.identifiers.ContactGroupType;
import nucleus.ReportContext;
import plugins.groups.datacontainers.PersonGroupDataView;
import plugins.groups.support.GroupId;
import plugins.people.support.PersonId;
import plugins.reports.support.AbstractReport;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;

import java.util.List;

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

    PersonGroupDataView personGroupDataView;

    @Override
    public void init(ReportContext reportContext) {
        super.init(reportContext);

        personGroupDataView = reportContext.getDataView(PersonGroupDataView.class).get();
    }

    @Override
    public void close() {
        for (ContactGroupType contactGroupType : ContactGroupType.values()) {
            List<GroupId> groupIds = personGroupDataView.getGroupsForGroupType(contactGroupType);
            for (GroupId groupId : groupIds) {
                List<PersonId> people = personGroupDataView.getPeopleForGroup(groupId);
                for (PersonId personId : people) {
                    ReportItem.Builder reportItemBuilder = ReportItem.builder();
                    reportItemBuilder.setReportType(getClass());
                    reportItemBuilder.setReportHeader(getReportHeader());

                    reportItemBuilder.addValue(groupId);
                    reportItemBuilder.addValue(contactGroupType);
                    reportItemBuilder.addValue(personId);

                    releaseOutputItem(reportItemBuilder.build());
                }
            }
        }

    }

}
