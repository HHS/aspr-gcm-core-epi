package gcm.core.epi.reports;

import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import gcm.core.epi.util.loading.LoadableReport;
import gcm.core.epi.util.loading.ReportLoader;
import gcm.core.epi.util.loading.ReportWrapperItem;
import gcm.experiment.ExperimentExecutor;
import gcm.scenario.PersonPropertyId;

import java.nio.file.Path;
import java.util.List;

public enum CustomReport implements LoadableReport {

    GROUP_MEMBERSHIP_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, GroupMembershipReport.class);
    }),

    INDIVIDUAL_PERSON_PROPERTY_CHANGE_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        PersonPropertyId[] personPropertyIds = CoreEpiBootstrapUtil.getPersonPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new PersonPropertyId[0]);
        // Report expects a single entry for initializationData that is a PersonPropertyId[], so cast to Object
        experimentExecutor.addCustomReport(path, IndividualPersonPropertyChangeReport.class, (Object) personPropertyIds);
    }),

    INCIDENCE_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, IncidenceReport.class, reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel());
    }),

    INCIDENCE_REPORT_BY_AGE((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, IncidenceReportByAge.class, reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel());
    }),

    INFECTION_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, InfectionReport.class, reportWrapperItem.showTransmissionAttempts());
    }),

    POPULATION_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, PopulationReport.class, reportWrapperItem.regionAggregationLevel());
    }),

    COMPARTMENT_REGIONAL_POPULATION_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, CompartmentRegionalPopulationReport.class, reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel());
    }),

    COMPARTMENT_REGIONAL_TRANSFER_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, CompartmentRegionalTransferReport.class, reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel());
    }),

    PERSON_REGIONAL_PROPERTY_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        PersonPropertyId[] personPropertyIds = CoreEpiBootstrapUtil.getPersonPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new PersonPropertyId[0]);
        experimentExecutor.addCustomReport(path, PersonRegionalPropertyReport.class, reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel(), personPropertyIds);
    }),

    PERSON_REGION_RESOURCE_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, PersonRegionResourceReport.class, reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel());
    }),

    AGGREGATED_REGION_TRANSFER_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, AggregatedRegionTransferReport.class, reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel());
    }),

    TRIGGER_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, TriggerReport.class);
    }),

    DETAILED_RESOURCE_VACCINATION_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCustomReport(path, DetailedResourceVaccinationReport.class, reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel());
    });

    private final ReportLoader reportLoader;

    CustomReport(ReportLoader reportLoader) {
        this.reportLoader = reportLoader;
    }

    @Override
    public void load(ExperimentExecutor experimentExecutor, Path path, ReportWrapperItem reportWrapperItem, List<Plugin> pluginList) {
        this.reportLoader.load(experimentExecutor, path, reportWrapperItem, pluginList);
    }

}
