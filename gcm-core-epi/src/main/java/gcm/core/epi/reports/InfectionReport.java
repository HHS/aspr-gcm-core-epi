package gcm.core.epi.reports;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.InfectionData;
import nucleus.ReportContext;
import plugins.globals.datacontainers.GlobalDataView;
import plugins.globals.events.GlobalPropertyChangeObservationEvent;
import plugins.people.support.PersonId;
import plugins.personproperties.datacontainers.PersonPropertyDataView;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;

import java.util.Optional;

public class InfectionReport {

    private final ReportHeader reportHeader;
    private final boolean showTransmissionAttempts;

    public InfectionReport(boolean showTransmissionAttempts) {
        this.showTransmissionAttempts = showTransmissionAttempts;
        ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder()
                .add("Time")
                .add("SourcePersonId")
                .add("SourcePersonAgeGroup")
                .add("TargetPersonId")
                .add("TargetPersonAgeGroup")
                .add("TransmissionSetting");
        if (showTransmissionAttempts) {
            reportHeaderBuilder.add("TransmissionOccurred");
        }
        reportHeader = reportHeaderBuilder.build();
    }

    GlobalDataView globalDataView;
    PersonPropertyDataView personPropertyDataView;

    public void init(ReportContext reportContext) {
        reportContext.subscribeToEvent(GlobalPropertyChangeObservationEvent.class,
                this::handleGlobalPropertyChangeObservationEvent);
        globalDataView = reportContext.getDataView(GlobalDataView.class).get();
        personPropertyDataView = reportContext.getDataView(PersonPropertyDataView.class).get();
    }

    private void handleGlobalPropertyChangeObservationEvent(ReportContext context, GlobalPropertyChangeObservationEvent globalPropertyChangeObservationEvent) {
        if (globalPropertyChangeObservationEvent.getGlobalPropertyId() == GlobalProperty.MOST_RECENT_INFECTION_DATA) {
            //noinspection unchecked
            handleMostRecentInfectionDataAssignment(context, (Optional<InfectionData>) globalPropertyChangeObservationEvent.getCurrentPropertyValue());
        }
    }

    private void handleMostRecentInfectionDataAssignment(ReportContext context, Optional<InfectionData> optionalInfectionData) {

        optionalInfectionData.ifPresent(infectionData -> {
            if (infectionData.transmissionOccurred() | showTransmissionAttempts) {
                final ReportItem.Builder reportItemBuilder = ReportItem.builder();
                reportItemBuilder.setReportId(context.getCurrentReportId());
                reportItemBuilder.setReportHeader(reportHeader);
                reportItemBuilder.addValue(context.getTime());
                PopulationDescription populationDescription = globalDataView.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);

                if (infectionData.sourcePersonId().isPresent()) {
                    PersonId sourcePersonId = infectionData.sourcePersonId().get();
                    reportItemBuilder.addValue(sourcePersonId);
                    Integer sourcePersonAgeGroupIndex = personPropertyDataView.getPersonPropertyValue(sourcePersonId, PersonProperty.AGE_GROUP_INDEX);
                    reportItemBuilder.addValue(populationDescription.ageGroupPartition().getAgeGroupFromIndex(sourcePersonAgeGroupIndex));
                } else {
                    reportItemBuilder.addValue("");
                    reportItemBuilder.addValue("");
                }

                if (infectionData.targetPersonId().isPresent()) {
                    PersonId targetPersonId = infectionData.targetPersonId().get();
                    reportItemBuilder.addValue(targetPersonId);
                    Integer targetPersonAgeGroupIndex = personPropertyDataView.getPersonPropertyValue(targetPersonId, PersonProperty.AGE_GROUP_INDEX);
                    reportItemBuilder.addValue(populationDescription.ageGroupPartition().getAgeGroupFromIndex(targetPersonAgeGroupIndex));
                } else {
                    reportItemBuilder.addValue("");
                    reportItemBuilder.addValue("");
                }

                reportItemBuilder.addValue(infectionData.transmissionSetting());
                if (showTransmissionAttempts) {
                    reportItemBuilder.addValue(infectionData.transmissionOccurred());
                }

                context.releaseOutput(reportItemBuilder.build());
            }
        });

    }

}
