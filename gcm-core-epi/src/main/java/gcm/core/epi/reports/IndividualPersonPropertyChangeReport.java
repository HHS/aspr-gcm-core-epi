package gcm.core.epi.reports;

import nucleus.EventLabel;
import nucleus.ReportContext;
import plugins.people.datacontainers.PersonDataView;
import plugins.people.support.PersonId;
import plugins.personproperties.datacontainers.PersonPropertyDataView;
import plugins.personproperties.events.observation.PersonPropertyChangeObservationEvent;
import plugins.personproperties.support.PersonPropertyId;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;


import java.util.Set;
public final class IndividualPersonPropertyChangeReport {

    private final Set<PersonPropertyId> personPropertyIds;
    private final ReportHeader reportHeader;

    public IndividualPersonPropertyChangeReport(Set<PersonPropertyId> personPropertyIds) {
        this.personPropertyIds = personPropertyIds;
        reportHeader = ReportHeader.builder()
                .add("Time")
                .add("PersonId")
                .add("Property")
                .add("Value")
                .build();
    }

    private void handlePersonPropertyChangeObservationEvent(ReportContext context, PersonPropertyChangeObservationEvent personPropertyChangeObservationEvent) {
        PersonPropertyId personPropertyId = personPropertyChangeObservationEvent.getPersonPropertyId();
        if (personPropertyIds.contains(personPropertyId)) {
            ReportItem.Builder reportItemBuilder = ReportItem.builder();
            reportItemBuilder.setReportId(context.getCurrentReportId());
            reportItemBuilder.setReportHeader(reportHeader);

            reportItemBuilder.addValue(context.getTime());
            reportItemBuilder.addValue(personPropertyChangeObservationEvent.getPersonId());
            reportItemBuilder.addValue(personPropertyId);
            reportItemBuilder.addValue(personPropertyChangeObservationEvent.getCurrentPropertyValue());

            context.releaseOutput(reportItemBuilder.build());
        }
    }

    public void init(ReportContext reportContext) {

        PersonDataView personDataView = reportContext.getDataView(PersonDataView.class).get();
        PersonPropertyDataView personPropertyDataView = reportContext.getDataView(PersonPropertyDataView.class).get();

        /*
         * If no person properties were specified, then assume all are wanted
         */
        if (personPropertyIds.size() == 0) {
            personPropertyIds.addAll(personPropertyDataView.getPersonPropertyIds());
        }

        // If all person properties are included, then subscribe to the event
        // class, otherwise subscribe to the individual property values
        if (personPropertyIds.equals(personPropertyDataView.getPersonPropertyIds())) {
            reportContext.subscribe(PersonPropertyChangeObservationEvent.class, this::handlePersonPropertyChangeObservationEvent);
        } else {
            for (PersonPropertyId personPropertyId : personPropertyIds) {
                EventLabel<PersonPropertyChangeObservationEvent> eventLabelByProperty = PersonPropertyChangeObservationEvent.getEventLabelByProperty(reportContext, personPropertyId);
                reportContext.subscribe(eventLabelByProperty, this::handlePersonPropertyChangeObservationEvent);
            }
        }

        for (PersonId personId : personDataView.getPeople()) {
            for (PersonPropertyId personPropertyId : personPropertyIds) {
                ReportItem.Builder reportItemBuilder = ReportItem.builder();
                reportItemBuilder.setReportId(reportContext.getCurrentReportId());
                reportItemBuilder.setReportHeader(reportHeader);

                reportItemBuilder.addValue(reportContext.getTime());
                reportItemBuilder.addValue(personId);
                reportItemBuilder.addValue(personPropertyId);
                reportItemBuilder.addValue(personPropertyDataView.getPersonPropertyValue(personId, personPropertyId));

                reportContext.releaseOutput(reportItemBuilder.build());
            }
        }

    }

}
