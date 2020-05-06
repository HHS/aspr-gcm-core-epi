package gcm.core.epi.reports;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.InfectionData;
import gcm.output.reports.AbstractReport;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.scenario.GlobalPropertyId;
import gcm.scenario.PersonId;
import gcm.simulation.ObservableEnvironment;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InfectionReport extends AbstractReport {

    private ReportHeader reportHeader;
    private boolean showTransmissionAttempts = false;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.ReportHeaderBuilder reportHeaderBuilder = new ReportHeader.ReportHeaderBuilder();
            reportHeaderBuilder.add("Time");
            reportHeaderBuilder.add("SourcePersonId");
            reportHeaderBuilder.add("SourcePersonAgeGroup");
            reportHeaderBuilder.add("TargetPersonId");
            reportHeaderBuilder.add("TargetPersonAgeGroup");
            reportHeaderBuilder.add("TransmissionSetting");
            if (showTransmissionAttempts) {
                reportHeaderBuilder.add("TransmissionOccurred");
            }
            reportHeader = reportHeaderBuilder.build();
        }
        return reportHeader;
    }

    @Override
    public void init(ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        super.init(observableEnvironment, initialData);
        for (Object initialDatum : initialData) {
            if (initialDatum instanceof Boolean) {
                showTransmissionAttempts = (Boolean) initialDatum;
            } else {
                throw new RuntimeException("Invalid initial data passed to InfectionReport");
            }
        }

        handleMostRecentInfectionDataAssignment(observableEnvironment);

    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        return Stream.of(StateChange.GLOBAL_PROPERTY_VALUE_ASSIGNMENT).collect(Collectors.toSet());
    }

    @Override
    public void handleGlobalPropertyValueAssignment(ObservableEnvironment observableEnvironment, GlobalPropertyId propertyId) {

        if (propertyId == GlobalProperty.MOST_RECENT_INFECTION_DATA) {
            handleMostRecentInfectionDataAssignment(observableEnvironment);
        }

    }

    private void handleMostRecentInfectionDataAssignment(ObservableEnvironment observableEnvironment) {

        Optional<InfectionData> optionalInfectionData = observableEnvironment.getGlobalPropertyValue(GlobalProperty.MOST_RECENT_INFECTION_DATA);

        optionalInfectionData.ifPresent(infectionData -> {
            if (infectionData.transmissionOccurred() | showTransmissionAttempts) {
                final ReportItem.ReportItemBuilder reportItemBuilder = new ReportItem.ReportItemBuilder();
                reportItemBuilder.setReportType(getClass());
                reportItemBuilder.setReportHeader(getReportHeader());
                reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());

                reportItemBuilder.addValue(observableEnvironment.getTime());
                PopulationDescription populationDescription = observableEnvironment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);

                if (infectionData.sourcePersonId().isPresent()) {
                    PersonId sourcePersonId = infectionData.sourcePersonId().get();
                    reportItemBuilder.addValue(sourcePersonId);
                    Integer sourcePersonAgeGroupIndex = observableEnvironment.getPersonPropertyValue(sourcePersonId, PersonProperty.AGE_GROUP_INDEX);
                    reportItemBuilder.addValue(populationDescription.ageGroupPartition().getAgeGroupFromIndex(sourcePersonAgeGroupIndex));
                } else {
                    reportItemBuilder.addValue("");
                    reportItemBuilder.addValue("");
                }

                if (infectionData.targetPersonId().isPresent()) {
                    PersonId targetPersonId = infectionData.targetPersonId().get();
                    reportItemBuilder.addValue(targetPersonId);
                    Integer targetPersonAgeGroupIndex = observableEnvironment.getPersonPropertyValue(targetPersonId, PersonProperty.AGE_GROUP_INDEX);
                    reportItemBuilder.addValue(populationDescription.ageGroupPartition().getAgeGroupFromIndex(targetPersonAgeGroupIndex));
                } else {
                    reportItemBuilder.addValue("");
                    reportItemBuilder.addValue("");
                }

                reportItemBuilder.addValue(infectionData.transmissionSetting());
                if (showTransmissionAttempts) {
                    reportItemBuilder.addValue(infectionData.transmissionOccurred());
                }

                observableEnvironment.releaseOutputItem(reportItemBuilder.build());
            }
        });

    }

}
