package gcm.core.epi.util.loading;

import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.plugin.Plugin;
import nucleus.ReportContext;
import plugins.compartments.reports.CompartmentPropertyReport;
import plugins.gcm.reports.*;
import plugins.globals.reports.GlobalPropertyReport;
import plugins.globals.support.GlobalPropertyId;
import plugins.groups.reports.GroupPopulationReport;
import plugins.groups.reports.GroupPropertyReport;
import plugins.materials.reports.BatchStatusReport;
import plugins.materials.reports.MaterialsProducerPropertyReport;
import plugins.materials.reports.MaterialsProducerResourceReport;
import plugins.materials.reports.StageReport;
import plugins.personproperties.support.PersonPropertyId;
import plugins.regions.reports.RegionPropertyReport;
import plugins.regions.support.RegionPropertyId;
import plugins.resources.reports.ResourcePropertyReport;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public enum CommonReport implements LoadableReport {

    BATCH_STATUS_REPORT((reportWrapperItem, pluginList) -> () -> new BatchStatusReport()::init),

    COMPARTMENT_POPULATION_REPORT((reportWrapperItem, pluginList) ->
            () -> new CompartmentPopulationReport(reportWrapperItem.period())::init),

    COMPARTMENT_PROPERTY_REPORT((reportWrapperItem, pluginList) -> () -> new CompartmentPropertyReport()::init),

    COMPARTMENT_TRANSFER_REPORT((reportWrapperItem, pluginList) ->
            () -> new CompartmentTransferReport(reportWrapperItem.period())::init),

    GLOBAL_PROPERTY_REPORT((reportWrapperItem, pluginList) -> {
        GlobalPropertyId[] globalPropertyIds = CoreEpiBootstrapUtil.getGlobalPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new GlobalPropertyId[0]);
        return () -> new GlobalPropertyReport(globalPropertyIds)::init;
    }),

    GROUP_POPULATION_REPORT((reportWrapperItem, pluginList) ->
            () -> new GroupPopulationReport(reportWrapperItem.period())::init),

    GROUP_PROPERTY_REPORT((reportWrapperItem, pluginList) -> () -> {
        // TODO: For now no group properties
        GroupPropertyReport.GroupPropertyReportSettingsBuilder groupPropertyReportSettingsBuilder =
                GroupPropertyReport.settingsBuilder();
        return new GroupPropertyReport(reportWrapperItem.period(), groupPropertyReportSettingsBuilder.build())::init;
    }),

    MATERIALS_PRODUCER_PROPERTY_REPORT((reportWrapperItem, pluginList) -> () -> new MaterialsProducerPropertyReport()::init),

    MATERIALS_PRODUCER_RESOURCE_REPORT((reportWrapperItem, pluginList) -> () -> new MaterialsProducerResourceReport()::init),

    PERSON_PROPERTY_INTERACTION_REPORT((reportWrapperItem, pluginList) -> {
        PersonPropertyId[] personPropertyIds = CoreEpiBootstrapUtil.getPersonPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new PersonPropertyId[0]);
        return () -> new PersonPropertyInteractionReport(reportWrapperItem.period(), personPropertyIds)::init;
    }),

    PERSON_PROPERTY_REPORT((reportWrapperItem, pluginList) -> {
        PersonPropertyId[] personPropertyIds = CoreEpiBootstrapUtil.getPersonPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new PersonPropertyId[0]);
        return () -> new PersonPropertyReport(reportWrapperItem.period(), personPropertyIds)::init;
    }),

    PERSON_RESOURCE_REPORT((reportWrapperItem, pluginList) -> {
        Set<Resource> resourceIds = getParsedResourceIds(reportWrapperItem);
        return () -> new PersonResourceReport(reportWrapperItem.period(), reportWrapperItem.reportPeopleWithoutResources(),
                reportWrapperItem.reportZeroPopulations(), resourceIds.toArray(new Resource[0]))::init;
    }),

    REGION_PROPERTY_REPORT((reportWrapperItem, pluginList) -> {
        RegionPropertyId[] regionPropertyIds = CoreEpiBootstrapUtil.getRegionPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new RegionPropertyId[0]);
        return () -> new RegionPropertyReport(regionPropertyIds)::init;
    }),

    REGION_TRANSFER_REPORT((reportWrapperItem, pluginList) ->
            () -> new RegionTransferReport(reportWrapperItem.period())::init),

    RESOURCE_PROPERTY_REPORT((reportWrapperItem, pluginList) -> () -> new ResourcePropertyReport()::init),

    RESOURCE_REPORT((reportWrapperItem, pluginList) -> {
        Set<Resource> resourceIds = getParsedResourceIds(reportWrapperItem);
        return () -> new ResourceReport(reportWrapperItem.period(), resourceIds.toArray(new Resource[0]))::init;
    }),

    STAGE_REPORT((reportWrapperItem, pluginList) -> () -> new StageReport()::init);

    private final ReportSupplier reportSupplier;

    CommonReport(ReportSupplier reportSupplier) {
        this.reportSupplier = reportSupplier;
    }

    private static Set<Resource> getParsedResourceIds(ReportWrapperItem reportWrapperItem) {
        return CoreEpiBootstrapUtil.getSetOfEnumsFromStringSet(reportWrapperItem.items(), Resource.class);
    }

    public Supplier<Consumer<ReportContext>> getSupplier(ReportWrapperItem reportWrapperItem, List<Plugin> pluginList) {
        return this.reportSupplier.getSupplier(reportWrapperItem, pluginList);
    }

}

