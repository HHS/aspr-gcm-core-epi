package gcm.core.epi.reports;

import gcm.core.epi.plugin.vaccine.resourcebased.DetailedResourceBasedVaccinePlugin;
import gcm.core.epi.plugin.vaccine.resourcebased.DetailedResourceVaccinationData;
import gcm.core.epi.plugin.vaccine.resourcebased.VaccineAdministratorId;
import gcm.core.epi.plugin.vaccine.resourcebased.VaccineId;
import gcm.output.reports.ReportHeader;
import gcm.output.reports.ReportItem;
import gcm.output.reports.StateChange;
import gcm.scenario.GlobalPropertyId;
import gcm.scenario.RegionId;
import gcm.simulation.ObservableEnvironment;

import java.util.*;

public class DetailedResourceVaccinationReport extends RegionAggregationPeriodicReport {

    private final Map<String, Map<AdministrationData,
            Map<DetailedResourceVaccinationData.DoseType, Counter>>> regionCounterMap = new LinkedHashMap<>();
    private ReportHeader reportHeader;

    private ReportHeader getReportHeader() {
        if (reportHeader == null) {
            ReportHeader.Builder reportHeaderBuilder = ReportHeader.builder();
            addTimeFieldHeaders(reportHeaderBuilder);
            reportHeader = reportHeaderBuilder
                    .add("Region")
                    .add("VaccineAdministrator")
                    .add("Vaccine")
                    .add("FirstDoses")
                    .add("SecondDoses")
                    .build();
        }
        return reportHeader;
    }

    @Override
    public void init(ObservableEnvironment observableEnvironment, Set<Object> initialData) {
        if (!observableEnvironment.getGlobalPropertyIds().contains(
                DetailedResourceBasedVaccinePlugin.VaccineGlobalProperty.MOST_RECENT_VACCINATION_DATA)) {
            throw new RuntimeException("Detailed Resource Vaccination Report requires the corresponding plugin");
        }
        super.init(observableEnvironment, initialData);

        // Initialize regionCounterMap
        for (RegionId regionId : observableEnvironment.getRegionIds()) {
            regionCounterMap.put(getFipsString(regionId), new HashMap<>());
        }
    }

    @Override
    public void handleGlobalPropertyValueAssignment(ObservableEnvironment observableEnvironment, GlobalPropertyId globalPropertyId) {
        if (globalPropertyId.equals(DetailedResourceBasedVaccinePlugin
                .VaccineGlobalProperty.MOST_RECENT_VACCINATION_DATA)) {
            setCurrentReportingPeriod(observableEnvironment);
            Optional<DetailedResourceVaccinationData> vaccinationDataOptional = observableEnvironment.getGlobalPropertyValue(
                    DetailedResourceBasedVaccinePlugin.VaccineGlobalProperty.MOST_RECENT_VACCINATION_DATA);
            if (vaccinationDataOptional.isPresent()) {
                DetailedResourceVaccinationData vaccinationData = vaccinationDataOptional.get();
                Map<AdministrationData, Map<DetailedResourceVaccinationData.DoseType, Counter>> administrationCounterMap =
                        regionCounterMap.get(getFipsString(vaccinationData.regionId()));
                Map<DetailedResourceVaccinationData.DoseType, Counter> counterMap = administrationCounterMap.computeIfAbsent(
                        new AdministrationData(vaccinationData.vaccineAdministratorId(), vaccinationData.vaccineId()),
                        x -> new EnumMap<>(DetailedResourceVaccinationData.DoseType.class));
                Counter counter = counterMap.computeIfAbsent(vaccinationData.doseType(), x -> new Counter());
                counter.count++;
            }
        }
    }

    @Override
    protected void flush(ObservableEnvironment observableEnvironment) {
        final ReportItem.Builder reportItemBuilder = ReportItem.builder();

        regionCounterMap.forEach(
                (regionId, counterMap) -> {
                    counterMap.forEach(
                            (administrationData, doseTypeCounterMap) -> {
                                reportItemBuilder.setReportHeader(getReportHeader());
                                reportItemBuilder.setReportType(getClass());
                                reportItemBuilder.setScenarioId(observableEnvironment.getScenarioId());
                                reportItemBuilder.setReplicationId(observableEnvironment.getReplicationId());

                                buildTimeFields(reportItemBuilder);
                                reportItemBuilder.addValue(regionId);
                                reportItemBuilder.addValue(administrationData.vaccineAdministratorId.id());
                                reportItemBuilder.addValue(administrationData.vaccineId.id());
                                reportItemBuilder.addValue(doseTypeCounterMap
                                        .computeIfAbsent(DetailedResourceVaccinationData.DoseType.FIRST_DOSE, x -> new Counter()).count);
                                reportItemBuilder.addValue(doseTypeCounterMap
                                        .computeIfAbsent(DetailedResourceVaccinationData.DoseType.SECOND_DOSE, x -> new Counter()).count);

                                observableEnvironment.releaseOutputItem(reportItemBuilder.build());

                                // Reset counters
                                for (DetailedResourceVaccinationData.DoseType doseType :
                                        DetailedResourceVaccinationData.DoseType.values()) {
                                    doseTypeCounterMap.get(doseType).count = 0;
                                }
                            }
                    );
                }
        );
    }

    @Override
    public Set<StateChange> getListenedStateChanges() {
        final Set<StateChange> result = new LinkedHashSet<>();
        result.add(StateChange.GLOBAL_PROPERTY_VALUE_ASSIGNMENT);
        return result;
    }

    /*
        Will count by (AdministrationData, DoseType)
     */
    private static class AdministrationData {
        final VaccineAdministratorId vaccineAdministratorId;
        final VaccineId vaccineId;

        private AdministrationData(VaccineAdministratorId vaccineAdministratorId, VaccineId vaccineId) {
            this.vaccineAdministratorId = vaccineAdministratorId;
            this.vaccineId = vaccineId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AdministrationData that = (AdministrationData) o;
            return vaccineAdministratorId.equals(that.vaccineAdministratorId) && vaccineId.equals(that.vaccineId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vaccineAdministratorId, vaccineId);
        }
    }

    /*
        A counter for people in a region that met a given criterion
     */
    private final static class Counter {
        int count;
    }

}
