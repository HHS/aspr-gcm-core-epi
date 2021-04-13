package gcm.core.epi.reports;

import nucleus.ReportContext;
import plugins.people.datacontainers.PersonDataView;
import plugins.people.support.PersonId;
import plugins.personproperties.datacontainers.PersonPropertyDataView;
import plugins.personproperties.events.observation.PersonPropertyChangeObservationEvent;
import plugins.personproperties.support.PersonPropertyId;
import plugins.reports.support.AbstractReport;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;


import java.util.LinkedHashSet;
import java.util.Set;
public final class IndividualPersonPropertyChangeReport extends AbstractReport {

    private final Set<PersonPropertyId> personPropertyIds = new LinkedHashSet<>();
    private ReportHeader reportHeader;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            reportHeader = ReportHeader.builder()
                    .add("Time")
                    .add("PersonId")
                    .add("Property")
                    .add("Value")
                    .build();
        }
        return reportHeader;
    }

    private void handlePersonPropertyChangeObservationEvent(PersonPropertyChangeObservationEvent personPropertyChangeObservationEvent) {
        PersonPropertyId personPropertyId = personPropertyChangeObservationEvent.getPersonPropertyId();
        if (personPropertyIds.contains(personPropertyId)) {
            ReportItem.Builder reportItemBuilder = ReportItem.builder();
            reportItemBuilder.setReportType(getClass());
            reportItemBuilder.setReportHeader(getReportHeader());

            reportItemBuilder.addValue(getTime());
            reportItemBuilder.addValue(personPropertyChangeObservationEvent.getPersonId());
            reportItemBuilder.addValue(personPropertyId);
            reportItemBuilder.addValue(personPropertyChangeObservationEvent.getCurrentPropertyValue());

            releaseOutputItem(reportItemBuilder.build());
        }
    }

    private PersonDataView personDataView;
    private PersonPropertyDataView personPropertyDataView;

    @Override
    public void setInitializingData(Set<Object> initialData) {
        super.setInitializingData(initialData);
        for (Object initialDatum : initialData) {
            if (initialDatum instanceof PersonPropertyId) {
                PersonPropertyId personPropertyId = (PersonPropertyId) initialDatum;
                personPropertyIds.add(personPropertyId);
            }
        }
    }

    @Override
    public void init(ReportContext reportContext) {
        super.init(reportContext);

        reportContext.subscribeToEvent(PersonPropertyChangeObservationEvent.class);
        setConsumer(PersonPropertyChangeObservationEvent.class, this::handlePersonPropertyChangeObservationEvent);

        personDataView = reportContext.getDataView(PersonDataView.class).get();
        personPropertyDataView = reportContext.getDataView(PersonPropertyDataView.class).get();

        /*
         * If no person properties were specified, then assume all are wanted
         */
        if (personPropertyIds.size() == 0) {
            personPropertyIds.addAll(personPropertyDataView.getPersonPropertyIds());
        }

        for (PersonId personId : personDataView.getPeople()) {
            for (PersonPropertyId personPropertyId : personPropertyIds) {
                ReportItem.Builder reportItemBuilder = ReportItem.builder();
                reportItemBuilder.setReportType(getClass());
                reportItemBuilder.setReportHeader(getReportHeader());

                reportItemBuilder.addValue(getTime());
                reportItemBuilder.addValue(personId);
                reportItemBuilder.addValue(personPropertyId);
                reportItemBuilder.addValue(personPropertyDataView.getPersonPropertyValue(personId, personPropertyId));

                releaseOutputItem(reportItemBuilder.build());
            }
        }

    }

}
