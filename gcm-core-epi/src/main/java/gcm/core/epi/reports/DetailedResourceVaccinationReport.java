package gcm.core.epi.reports;

import gcm.core.epi.plugin.vaccine.resourcebased.DetailedResourceBasedVaccinePlugin;
import gcm.core.epi.plugin.vaccine.resourcebased.DetailedResourceVaccinationData;
import gcm.core.epi.plugin.vaccine.resourcebased.VaccineAdministratorId;
import gcm.core.epi.plugin.vaccine.resourcebased.VaccineId;
import gcm.core.epi.population.AgeGroup;
import nucleus.ReportContext;
import plugins.globals.datacontainers.GlobalDataView;
import plugins.globals.events.GlobalPropertyChangeObservationEvent;
import plugins.globals.events.GlobalPropertyValueAssignmentEvent;
import plugins.regions.datacontainers.RegionDataView;
import plugins.regions.support.RegionId;
import plugins.reports.support.ReportHeader;
import plugins.reports.support.ReportItem;

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
                    .add("AgeGroup")
                    .add("FirstDoses")
                    .add("SecondDoses")
                    .build();
        }
        return reportHeader;
    }

    private GlobalDataView globalDataView;
    private RegionDataView regionDataView;

    @Override
    public void init(final ReportContext reportContext) {
        super.init(reportContext);

        globalDataView = reportContext.getDataView(GlobalDataView.class).get();
        if (!globalDataView.getGlobalPropertyIds().contains(
                DetailedResourceBasedVaccinePlugin.VaccineGlobalProperty.MOST_RECENT_VACCINATION_DATA)) {
            throw new RuntimeException("Detailed Resource Vaccination Report requires the corresponding plugin");
        }
        regionDataView = reportContext.getDataView(RegionDataView.class).get();

        reportContext.subscribeToEvent(GlobalPropertyChangeObservationEvent.class);
        setConsumer(GlobalPropertyChangeObservationEvent.class, this::handleGlobalPropertyChangeObservationEvent);

        // Initialize regionCounterMap
        for (RegionId regionId : regionDataView.getRegionIds()) {
            regionCounterMap.put(getFipsString(regionId), new HashMap<>());
        }
    }


    public void handleGlobalPropertyChangeObservationEvent(GlobalPropertyChangeObservationEvent globalPropertyChangeObservationEvent) {
        if (globalPropertyChangeObservationEvent.getGlobalPropertyId().equals(DetailedResourceBasedVaccinePlugin
                .VaccineGlobalProperty.MOST_RECENT_VACCINATION_DATA)) {
            Optional<DetailedResourceVaccinationData> vaccinationDataOptional = globalDataView.getGlobalPropertyValue(
                    DetailedResourceBasedVaccinePlugin.VaccineGlobalProperty.MOST_RECENT_VACCINATION_DATA);
            if (vaccinationDataOptional.isPresent()) {
                DetailedResourceVaccinationData vaccinationData = vaccinationDataOptional.get();
                Map<AdministrationData, Map<DetailedResourceVaccinationData.DoseType, Counter>> administrationCounterMap =
                        regionCounterMap.get(getFipsString(vaccinationData.regionId()));
                Map<DetailedResourceVaccinationData.DoseType, Counter> counterMap = administrationCounterMap.computeIfAbsent(
                        new AdministrationData(vaccinationData.vaccineAdministratorId(), vaccinationData.vaccineId(),
                                vaccinationData.ageGroup()),
                        x -> new EnumMap<>(DetailedResourceVaccinationData.DoseType.class));
                Counter counter = counterMap.computeIfAbsent(vaccinationData.doseType(), x -> new Counter());
                counter.count++;
            }
        }
    }

    @Override
    protected void flush() {
        final ReportItem.Builder reportItemBuilder = ReportItem.builder();

        regionCounterMap.forEach(
                (regionId, counterMap) -> counterMap.forEach(
                        (administrationData, doseTypeCounterMap) -> {
                            reportItemBuilder.setReportHeader(getReportHeader());
                            reportItemBuilder.setReportType(getClass());

                            buildTimeFields(reportItemBuilder);
                            reportItemBuilder.addValue(regionId);
                            reportItemBuilder.addValue(administrationData.vaccineAdministratorId.id());
                            reportItemBuilder.addValue(administrationData.vaccineId.id());
                            reportItemBuilder.addValue(administrationData.ageGroup.toString());
                            reportItemBuilder.addValue(doseTypeCounterMap
                                    .computeIfAbsent(DetailedResourceVaccinationData.DoseType.FIRST_DOSE, x -> new Counter()).count);
                            reportItemBuilder.addValue(doseTypeCounterMap
                                    .computeIfAbsent(DetailedResourceVaccinationData.DoseType.SECOND_DOSE, x -> new Counter()).count);

                            releaseOutputItem(reportItemBuilder.build());

                            // Reset counters
                            for (DetailedResourceVaccinationData.DoseType doseType :
                                    DetailedResourceVaccinationData.DoseType.values()) {
                                doseTypeCounterMap.get(doseType).count = 0;
                            }
                        }
                )
        );
    }

    /*
        Will count by (AdministrationData, DoseType)
     */
    private static class AdministrationData {
        final VaccineAdministratorId vaccineAdministratorId;
        final VaccineId vaccineId;
        final AgeGroup ageGroup;

        private AdministrationData(VaccineAdministratorId vaccineAdministratorId, VaccineId vaccineId, AgeGroup ageGroup) {
            this.vaccineAdministratorId = vaccineAdministratorId;
            this.vaccineId = vaccineId;
            this.ageGroup = ageGroup;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AdministrationData that = (AdministrationData) o;
            return vaccineAdministratorId.equals(that.vaccineAdministratorId) && vaccineId.equals(that.vaccineId) &&
                    ageGroup.equals(that.ageGroup);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vaccineAdministratorId, vaccineId, ageGroup);
        }
    }

    /*
        A counter for people in a region that met a given criterion
     */
    private final static class Counter {
        int count;
    }

}
