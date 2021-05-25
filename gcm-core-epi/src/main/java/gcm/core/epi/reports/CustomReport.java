package gcm.core.epi.reports;

import gcm.core.epi.identifiers.Resource;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import gcm.core.epi.util.loading.LoadableReport;
import gcm.core.epi.util.loading.ReportSupplier;
import gcm.core.epi.util.loading.ReportWrapperItem;
import nucleus.ReportContext;
import nucleus.ReportId;
import plugins.personproperties.support.PersonPropertyId;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public enum CustomReport implements LoadableReport, ReportId {

    GROUP_MEMBERSHIP_REPORT((reportWrapperItem, pluginList) -> () -> new GroupMembershipReport()::init),

    INDIVIDUAL_PERSON_PROPERTY_CHANGE_REPORT((reportWrapperItem, pluginList) -> {
        Set<PersonPropertyId> personPropertyIds = CoreEpiBootstrapUtil.getPersonPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList);
        return () -> new IndividualPersonPropertyChangeReport(personPropertyIds)::init;
    }),

    INCIDENCE_REPORT((reportWrapperItem, pluginList) ->
            () -> new IncidenceReport(reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel())::init),

    INCIDENCE_REPORT_BY_AGE((reportWrapperItem, pluginList) ->
        () -> new IncidenceReportByAge(reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel())::init),

    INFECTION_REPORT((reportWrapperItem, pluginList) ->
            () -> new InfectionReport(reportWrapperItem.showTransmissionAttempts())::init),

    POPULATION_REPORT((reportWrapperItem, pluginList) ->
            () -> new PopulationReport(reportWrapperItem.regionAggregationLevel())::init),

    COMPARTMENT_REGIONAL_POPULATION_REPORT((reportWrapperItem, pluginList) ->
            () -> new CompartmentRegionalPopulationReport(reportWrapperItem.period(),
                    reportWrapperItem.regionAggregationLevel())::init),

    COMPARTMENT_REGIONAL_TRANSFER_REPORT((reportWrapperItem, pluginList) ->
            () -> new CompartmentRegionalTransferReport(reportWrapperItem.period(),
                    reportWrapperItem.regionAggregationLevel())::init),

    PERSON_REGIONAL_PROPERTY_REPORT((reportWrapperItem, pluginList) -> {
        PersonPropertyId[] personPropertyIds = CoreEpiBootstrapUtil.getPersonPropertyIdsFromStringSet(
                reportWrapperItem.items(), pluginList).toArray(new PersonPropertyId[0]);
        return () -> new PersonRegionalPropertyReport(reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel(),
                personPropertyIds)::init;
    }),

    PERSON_REGIONAL_RESOURCE_REPORT((reportWrapperItem, pluginList) -> {
        Set<Resource> resourceIds = getParsedResourceIds(reportWrapperItem);
        return () -> new PersonRegionalResourceReport(reportWrapperItem.period(), reportWrapperItem.regionAggregationLevel(),
                reportWrapperItem.reportPeopleWithoutResources(), reportWrapperItem.reportZeroPopulations(),
                resourceIds.toArray(new Resource[0]))::init;
    }),

    AGGREGATED_REGION_TRANSFER_REPORT((reportWrapperItem, pluginList) ->
            () -> new AggregatedRegionTransferReport(reportWrapperItem.period(),
                    reportWrapperItem.regionAggregationLevel())::init
    ),

    // Trigger report has no body and thus nothing to init
    TRIGGER_REPORT((reportWrapperItem, pluginList) -> () -> (ReportContext reportContext) -> {}),

    DETAILED_RESOURCE_VACCINATION_REPORT((reportWrapperItem, pluginList) ->
            () -> new DetailedResourceVaccinationReport(reportWrapperItem.period(),
                    reportWrapperItem.regionAggregationLevel())::init);

    private final ReportSupplier reportSupplier;

    CustomReport(ReportSupplier reportSupplier) {
        this.reportSupplier = reportSupplier;
    }

    private static Set<Resource> getParsedResourceIds(ReportWrapperItem reportWrapperItem) {
        return CoreEpiBootstrapUtil.getSetOfEnumsFromStringSet(reportWrapperItem.items(), Resource.class);
    }

    @Override
    public Supplier<Consumer<ReportContext>> getSupplier(ReportWrapperItem reportWrapperItem, List<Plugin> pluginList) {
        return this.reportSupplier.getSupplier(reportWrapperItem, pluginList);
    }

}
