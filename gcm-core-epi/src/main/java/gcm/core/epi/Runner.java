package gcm.core.epi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import gcm.core.epi.components.Region;
import gcm.core.epi.identifiers.*;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.propertytypes.CombinedDeserializerModifier;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsCodeValueDeserializerModifier;
import gcm.core.epi.propertytypes.WeightsDeserializerModifier;
import gcm.core.epi.trigger.ImmutableTriggerId;
import gcm.core.epi.trigger.Trigger;
import gcm.core.epi.trigger.TriggerContainer;
import gcm.core.epi.trigger.TriggerDescription;
import gcm.core.epi.util.loading.*;
import gcm.core.epi.util.logging.LogItemHandler;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.experiment.Experiment;
import gcm.experiment.ExperimentExecutor;
import gcm.output.simstate.NIOProfileItemHandler;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.RegionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        /*
         * CoreFlu Configuration
         *
         * Note: args[0] should be the path to the YAML configuration file or directory
         */
        if (args.length == 0) {
            throw new IllegalArgumentException("First argument must be path to YAML configuration file or directory.");
        }
        final Path inputConfigPath = CoreEpiBootstrapUtil.getPathFromRelativeString(args[0]);
        if (Files.isDirectory(inputConfigPath)) {
            // Iterate over files in directory and execute each of them
            List<Path> inputConfigFiles = Files.list(inputConfigPath)
                    .filter(file -> file.toString().endsWith(".yaml"))
                    .sorted()
                    .collect(Collectors.toList());
            int counter = 1;
            for (Path inputConfigFilePath : inputConfigFiles) {
                logger.info("Running config " + counter + " of " + inputConfigFiles.size() + ": " + inputConfigFilePath);
                runFromYaml(inputConfigFilePath);
                counter++;
            }
        } else {
            // Run the single file
            runFromYaml(inputConfigPath);
        }

    }

    private static void runFromYaml(Path inputConfigPath) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .registerModule(new Jdk8Module());

        // Enable modified deserialization for property value lists to handle potential empty strings
        Module propertyDeserializationModule = new SimpleModule()
                .addDeserializer(ImmutablePropertyValueJsonList.class, new PropertyValueJsonListDeserializer());
        objectMapper.registerModule(propertyDeserializationModule);

        // Open configuration file and parse
        CoreEpiConfiguration configuration = objectMapper.readValue(inputConfigPath.toFile(), ImmutableCoreEpiConfiguration.class);

        // Get input path from configuration, translating home directory if needed
        final Path inputPath = CoreEpiBootstrapUtil.getPathFromRelativeString(configuration.inputDirectory());
        System.setProperty("user.dir", inputPath.toString());

        // Get output path from configuration, translating home directory if needed, and create if needed
        final Path outputPath = CoreEpiBootstrapUtil.getPathFromRelativeString(configuration.outputDirectory());
        Files.createDirectories(outputPath);

        // Scenario
        ExperimentBuilder experimentBuilder = new ExperimentBuilder();
        experimentBuilder.setBaseScenarioId(configuration.baseScenarioId());

        // Add global components
        for (final GlobalComponent globalComponent : GlobalComponent.values()) {
            experimentBuilder.addGlobalComponentId(globalComponent, globalComponent.getComponentClass());
        }

        // Add regions
        List<RegionFileRecord> regionFileRecords = CoreEpiBootstrapUtil.loadRegionsFromFile(inputPath.resolve(configuration.regions()));
        for (final RegionFileRecord regionFileRecord : regionFileRecords) {
            experimentBuilder.addRegionId(StringRegionId.of(regionFileRecord.id()), Region.class);
        }

        // Determine age groups
        AgeGroupPartition ageGroupPartition = CoreEpiBootstrapUtil.loadAgeGroupsFromFile(inputPath.resolve(configuration.ageGroups()));

        // Create module for deserializing special objects from Strings for use in map keys during property loading
        SimpleModule deserializationModule = new SimpleModule();
        deserializationModule.addKeyDeserializer(AgeGroup.class, new AgeGroupStringMapDeserializer(ageGroupPartition));
        deserializationModule.addKeyDeserializer(FipsCode.class, new FipsCodeStringMapDeserializer());
        List<BeanDeserializerModifier> modifiers = new ArrayList<>();
        // Add dynamic mapping for FipsCodeValue<T> and FipsCodeDouble objects
        modifiers.add(new FipsCodeValueDeserializerModifier());
        // Add dynamic mapping for Weights<T> and AgeWeights objects
        modifiers.add(new WeightsDeserializerModifier());
        deserializationModule.setDeserializerModifier(new CombinedDeserializerModifier(modifiers));
        // Register module in objectMapper
        objectMapper.registerModule(deserializationModule);

        // Add compartments
        for (final Compartment compartment : Compartment.values()) {
            experimentBuilder.addCompartmentId(compartment, compartment.getComponentClass());
        }

        // Add group type ids
        for (final ContactGroupType contactGroupType : ContactGroupType.values()) {
            experimentBuilder.addGroupTypeId(contactGroupType);
        }
        for (final HospitalGroupType hospitalGroupType : HospitalGroupType.values()) {
            experimentBuilder.addGroupTypeId(hospitalGroupType);
        }

        // Add plugin components
        List<Plugin> pluginList = new ArrayList<>();
        pluginList.add(configuration.infectionPlugin().getPluginClass().getDeclaredConstructor().newInstance());
        // TODO: Antiviral and vaccine plugins
        if (configuration.behaviorPlugin().isPresent()) {
            pluginList.add(configuration.behaviorPlugin().get().getBehaviorPluginClass().getDeclaredConstructor().newInstance());
        }
        if (configuration.transmissionPlugin().isPresent()) {
            pluginList.add(configuration.transmissionPlugin().get().getTransmissionPluginClass().getDeclaredConstructor().newInstance());
        }
        if (configuration.seedingPlugin().isPresent()) {
            pluginList.add(configuration.seedingPlugin().get().getPluginClass().getDeclaredConstructor().newInstance());
        }
        if (configuration.vaccinePlugin().isPresent()) {
            pluginList.add(configuration.vaccinePlugin().get().getPluginClass().getDeclaredConstructor().newInstance());
        }

        for (Plugin plugin : pluginList) {
            plugin.load(experimentBuilder);
        }

        // Global property loading
        CoreEpiBootstrapUtil loader = new CoreEpiBootstrapUtil();
        loader.loadGlobalProperties(experimentBuilder, pluginList, configuration, objectMapper, inputPath, ageGroupPartition);

        // Region property loading
        for (RegionProperty regionProperty : RegionProperty.values()) {
            experimentBuilder.defineRegionProperty(regionProperty,
                    regionProperty.getPropertyDefinition().definition());
        }
        for (RegionFileRecord regionFileRecord : regionFileRecords) {
            RegionId regionId = StringRegionId.of(regionFileRecord.id());
            experimentBuilder.addRegionPropertyValue(regionId, RegionProperty.LAT, regionFileRecord.lat());
            experimentBuilder.addRegionPropertyValue(regionId, RegionProperty.LON, regionFileRecord.lon());
        }

        // Person property loading
        for (PersonProperty personProperty : PersonProperty.values()) {
            experimentBuilder.definePersonProperty(personProperty,
                    personProperty.getPropertyDefinition().definition());
        }

        // Group property loading
        for (WorkplaceProperty workplaceProperty : WorkplaceProperty.values()) {
            experimentBuilder.defineGroupProperty(ContactGroupType.WORK, workplaceProperty,
                    workplaceProperty.getPropertyDefinition().definition());
        }

        // Add resources
        for (Resource resource : Resource.values()) {
            experimentBuilder.addResource(resource);
        }

        // Add random number generator IDs
        for (RandomId randomId : RandomId.values()) {
            experimentBuilder.addRandomNumberGeneratorId(randomId);
        }

        // Load triggers
        TriggerContainer.Builder triggerContainerBuilder = TriggerContainer.builder();
        for (TriggerDescription triggerDescription : configuration.triggers()) {
            Class<? extends Trigger> triggerClass = triggerDescription.type().getTriggerClass();
            Trigger trigger = objectMapper.treeToValue(triggerDescription.data(), triggerClass);
            triggerContainerBuilder.addTrigger(ImmutableTriggerId.of(triggerDescription.id(), trigger), trigger);
            // Define region properties for triggering if needed
            if (trigger.triggeringRegionProperty().isPresent()) {
                DefinedRegionProperty triggeringRegionProperty = trigger.triggeringRegionProperty().get();
                experimentBuilder.defineRegionProperty(triggeringRegionProperty,
                        triggeringRegionProperty.getPropertyDefinition().definition());
            }
            //triggerDescription.type().load(triggerContainerBuilder, triggerDescription.name(), triggerDescription.data());
        }
        experimentBuilder.addGlobalPropertyValue(GlobalProperty.TRIGGER_CONTAINER, triggerContainerBuilder.build());

        // Build experiment
        Experiment experiment = experimentBuilder.build();

        // Run
        ExperimentExecutor experimentExecutor = new ExperimentExecutor();
        experimentExecutor.setExperiment(experiment);
        experimentExecutor.setSeed(configuration.randomSeed());
        experimentExecutor.setThreadCount(configuration.threads());
        experimentExecutor.setReplicationCount(configuration.replications());
        experimentExecutor.setProduceSimulationStatusOutput(true);
        experimentExecutor.setLogItemHandler(new LogItemHandler());
        CoreEpiBootstrapUtil.loadReports(experimentExecutor, configuration.reports(), pluginList, outputPath);
        experimentExecutor.setDisplayExperimentColumnsInReports(configuration.displayExperimentColumns());
        if (configuration.includeExperimentColumnReport()) {
            experimentExecutor.addExperimentColumnReport(outputPath.resolve("experiment_column_report.tsv"));
        }
        if (configuration.runProfilingReport()) {
            experimentExecutor.addOutputItemHandler(new NIOProfileItemHandler(outputPath.resolve("profiling_report.tsv")));
        }
        if (configuration.useProgressLog()) {
            experimentExecutor.setExperimentProgressLog(outputPath.resolve("progress_log.tsv"));
        }
        if (configuration.runMemoryReport()) {
            experimentExecutor.setMemoryReport(outputPath.resolve("memory_report.tsv"), configuration.memoryReportInterval());
        }
        if (configuration.runPlanningQueueReport()) {
            experimentExecutor.setPlanningQueueReport(outputPath.resolve("planning_queue_report.tsv"), configuration.planningQueueReportThreshold());
        }

        experimentExecutor.execute();
    }

}
