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
import plugins.reports.support.AbstractReport;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;

import java.util.Optional;
import java.util.Set;

public class InfectionReport extends AbstractReport {

    private ReportHeader reportHeader;
    private boolean showTransmissionAttempts = false;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
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
        return reportHeader;
    }

    @Override
    public void setInitializingData(Set<Object> initialData) {
        for (Object initialDatum : initialData) {
            if (initialDatum instanceof Boolean) {
                showTransmissionAttempts = (Boolean) initialDatum;
            } else {
                throw new RuntimeException("Invalid initial data passed to InfectionReport");
            }
        }
    }

    GlobalDataView globalDataView;
    PersonPropertyDataView personPropertyDataView;

    @Override
    public void init(ReportContext reportContext) {
        super.init(reportContext);
        reportContext.subscribeToEvent(GlobalPropertyChangeObservationEvent.class);
        setConsumer(GlobalPropertyChangeObservationEvent.class, this::handleGlobalPropertyChangeObservationEvent);
        globalDataView = reportContext.getDataView(GlobalDataView.class).get();
        personPropertyDataView = reportContext.getDataView(PersonPropertyDataView.class).get();
    }

    private void handleGlobalPropertyChangeObservationEvent(GlobalPropertyChangeObservationEvent globalPropertyChangeObservationEvent) {
        if (globalPropertyChangeObservationEvent.getGlobalPropertyId() == GlobalProperty.MOST_RECENT_INFECTION_DATA) {
            handleMostRecentInfectionDataAssignment((Optional<InfectionData>) globalPropertyChangeObservationEvent.getCurrentPropertyValue());
        }
    }

    private void handleMostRecentInfectionDataAssignment(Optional<InfectionData> optionalInfectionData) {

        optionalInfectionData.ifPresent(infectionData -> {
            if (infectionData.transmissionOccurred() | showTransmissionAttempts) {
                final ReportItem.Builder reportItemBuilder = ReportItem.builder();
                reportItemBuilder.setReportType(getClass());
                reportItemBuilder.setReportHeader(getReportHeader());

                reportItemBuilder.addValue(getTime());
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

                releaseOutputItem(reportItemBuilder.build());
            }
        });

    }

}
