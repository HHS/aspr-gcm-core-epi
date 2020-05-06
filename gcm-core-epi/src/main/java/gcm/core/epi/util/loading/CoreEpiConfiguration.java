package gcm.core.epi.util.loading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.plugin.behavior.BehaviorPluginType;
import gcm.core.epi.plugin.infection.InfectionPluginType;
import gcm.core.epi.plugin.seeding.SeedingPluginType;
import gcm.core.epi.plugin.transmission.TransmissionPluginType;
import gcm.core.epi.trigger.TriggerDescription;
import gcm.core.epi.util.loading.ImmutableCoreEpiConfiguration;
import gcm.core.epi.util.property.PropertyGroupSpecification;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(as = ImmutableCoreEpiConfiguration.class)
public abstract class CoreEpiConfiguration {

    public abstract String inputDirectory();

    public abstract String regions();

    public abstract String ageGroups();

    public abstract InfectionPluginType infectionPlugin();

    public abstract Optional<BehaviorPluginType> behaviorPlugin();

    public abstract Optional<TransmissionPluginType> transmissionPlugin();

    public abstract Optional<SeedingPluginType> seedingPlugin();

    public abstract List<TriggerDescription> triggers();

    public abstract Map<String, List<JsonNode>> scenarios();

    @Value.Default
    public int baseScenarioId() {
        return 0;
    }

    public abstract List<PropertyGroupSpecification> propertyGroups();

    public abstract List<ReportWrapperItem> reports();

    @Value.Default
    public boolean includeExperimentColumnReport() {
        return false;
    }

    @Value.Default
    public boolean displayExperimentColumns() {
        return true;
    }

    public abstract List<String> forcedExperimentColumnProperties();

    @Value.Default
    public boolean runProfilingReport() {
        return false;
    }

    public abstract String outputDirectory();

    public abstract int replications();

    public abstract int threads();

    public abstract long randomSeed();

    @Value.Default
    public boolean useProgressLog() {
        return false;
    }

    @Value.Default
    public boolean runMemoryReport() {
        return false;
    }

    @Value.Default
    public double memoryReportInterval() {
        return -1.0;
    }

    @Value.Default
    public boolean runPlanningQueueReport() {
        return false;
    }

    @Value.Default
    public int planningQueueReportThreshold() {
        return -1;
    }

}
