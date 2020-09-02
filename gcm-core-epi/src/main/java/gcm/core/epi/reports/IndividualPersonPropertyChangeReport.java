package gcm.core.epi.reports;

import gcm.output.reports.AbstractReport;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.scenario.PersonId;
import gcm.scenario.PersonPropertyId;
import gcm.simulation.ObservableEnvironment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class IndividualPersonPropertyChangeReport extends AbstractReport {

    private final Set<PersonPropertyId> personPropertyIds = new LinkedHashSet<>();
    private ReportHeader reportHeader;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.ReportHeaderBuilder reportHeaderBuilder = new ReportHeader.ReportHeaderBuilder();
            reportHeaderBuilder.add("Time");
            reportHeaderBuilder.add("PersonId");
            reportHeaderBuilder.add("Property");
            reportHeaderBuilder.add("Value");
            reportHeader = reportHeaderBuilder.build();
        }
        return reportHeader;
    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        return Stream.of(StateChange.PERSON_PROPERTY_VALUE_ASSIGNMENT).collect(Collectors.toSet());
    }

    @Override
    public void handlePersonPropertyValueAssignment(ObservableEnvironment observableEnvironment, PersonId personId, PersonPropertyId personPropertyId, Object oldPersonPropertyValue) {

        if (personPropertyIds.contains(personPropertyId)) {
            ReportItem.ReportItemBuilder reportItemBuilder = new ReportItem.ReportItemBuilder();
            reportItemBuilder.setReportType(getClass());
            reportItemBuilder.setReportHeader(getReportHeader());
            reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
            reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());

            reportItemBuilder.addValue(observableEnvironment.getTime());
            reportItemBuilder.addValue(personId);
            reportItemBuilder.addValue(personPropertyId);
            reportItemBuilder.addValue(observableEnvironment.getPersonPropertyValue(personId, personPropertyId));

            observableEnvironment.releaseOutputItem(reportItemBuilder.build());
        }

    }

    @Override
    public void init(ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        super.init(observableEnvironment, initialData);

        for (Object initialDatum : initialData) {
            if (initialDatum instanceof PersonPropertyId[]) {
                personPropertyIds.addAll(Arrays.asList((PersonPropertyId[]) initialDatum));
            } else {
                throw new RuntimeException("Invalid initial data passed to IndividualPersonPropertyChangeReport");
            }
        }

        if (initialData.size() == 0) {
            personPropertyIds.addAll(observableEnvironment.getPersonPropertyIds());
        }

        for (PersonId personId : observableEnvironment.getPeople()) {
            for (PersonPropertyId personPropertyId : personPropertyIds) {
                ReportItem.ReportItemBuilder reportItemBuilder = new ReportItem.ReportItemBuilder();
                reportItemBuilder.setReportType(getClass());
                reportItemBuilder.setReportHeader(getReportHeader());
                reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());

                reportItemBuilder.addValue(observableEnvironment.getTime());
                reportItemBuilder.addValue(personId);
                reportItemBuilder.addValue(personPropertyId);
                reportItemBuilder.addValue(observableEnvironment.getPersonPropertyValue(personId, personPropertyId));

                observableEnvironment.releaseOutputItem(reportItemBuilder.build());
            }
        }

    }

}
