package gcm.core.epi.util.loading;

import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.plugin.Plugin;
import gcm.experiment.ExperimentExecutor;
import gcm.output.reports.commonreports.GroupPropertyReport;
import gcm.scenario.GlobalPropertyId;
import gcm.scenario.PersonPropertyId;
import gcm.scenario.RegionPropertyId;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public enum CommonReport implements LoadableReport {

    BATCH_STATUS_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addBatchStatusReport(path);
    }),
    COMPARTMENT_POPULATION_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCompartmentPopulationReport(path, reportWrapperItem.period());
    }),
    COMPARTMENT_PROPERTY_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCompartmentPropertyReport(path);
    }),
    COMPARTMENT_TRANSFER_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addCompartmentTransferReport(path, reportWrapperItem.period());
    }),
    GLOBAL_PROPERTY_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        GlobalPropertyId[] globalPropertyIds = CoreEpiBootstrapUtil.getGlobalPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new GlobalPropertyId[0]);
        experimentExecutor.addGlobalPropertyReport(path, globalPropertyIds);
    }),
    GROUP_POPULATION_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addGroupPopulationReport(path, reportWrapperItem.period());
    }),
    GROUP_PROPERTY_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        // TODO: For now no group properties
        GroupPropertyReport.GroupPropertyReportSettingsBuilder groupPropertyReportSettingsBuilder =
                GroupPropertyReport.settingsBuilder();
        experimentExecutor.addGroupPropertyReport(path, reportWrapperItem.period(), groupPropertyReportSettingsBuilder.build());
    }),
    MATERIALS_PRODUCER_PROPERTY_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addMaterialsProducerPropertyReport(path);
    }),
    MATERIALS_PRODUCER_RESOURCE_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addMaterialsProducerResourceReport(path);
    }),
    PERSON_PROPERTY_INTERACTION_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        PersonPropertyId[] personPropertyIds = CoreEpiBootstrapUtil.getPersonPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new PersonPropertyId[0]);
        experimentExecutor.addPersonPropertyInteractionReport(path, reportWrapperItem.period(), personPropertyIds);
    }),
    PERSON_PROPERTY_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        PersonPropertyId[] personPropertyIds = CoreEpiBootstrapUtil.getPersonPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new PersonPropertyId[0]);
        experimentExecutor.addPersonPropertyReport(path, reportWrapperItem.period(), personPropertyIds);
    }),
    PERSON_RESOURCE_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        Set<Resource> resourceIds = getParsedResourceIds(reportWrapperItem);
        experimentExecutor.addPersonResourceReport(path, reportWrapperItem.period(),
                reportWrapperItem.reportPeopleWithoutResources(), reportWrapperItem.reportZeroPopulations(),
                resourceIds.toArray(new Resource[0]));
    }),
    REGION_PROPERTY_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        RegionPropertyId[] regionPropertyIds = CoreEpiBootstrapUtil.getRegionPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new RegionPropertyId[0]);
        experimentExecutor.addRegionPropertyReport(path, regionPropertyIds);
    }),
    REGION_TRANSFER_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addMaterialsProducerResourceReport(path);
    }),
    RESOURCE_PROPERTY_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addResourcePropertyReport(path);
    }),
    RESOURCE_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        Set<Resource> resourceIds = getParsedResourceIds(reportWrapperItem);
        experimentExecutor.addResourceReport(path, reportWrapperItem.period(), resourceIds.toArray(new Resource[0]));
    }),
    STAGE_REPORT((experimentExecutor, path, reportWrapperItem, pluginList) -> {
        experimentExecutor.addStageReport(path);
    });

    private final ReportLoader reportLoader;

    CommonReport(ReportLoader reportLoader) {
        this.reportLoader = reportLoader;
    }

    private static Set<Resource> getParsedResourceIds(ReportWrapperItem reportWrapperItem) {
        return CoreEpiBootstrapUtil.getSetOfEnumsFromStringSet(reportWrapperItem.items(), Resource.class);
    }

    public void load(ExperimentExecutor experimentExecutor, Path path, ReportWrapperItem reportWrapperItem, List<Plugin> pluginList) {
        this.reportLoader.load(experimentExecutor, path, reportWrapperItem, pluginList);
    }

}

