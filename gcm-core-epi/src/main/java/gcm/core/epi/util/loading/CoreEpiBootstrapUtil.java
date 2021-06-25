package gcm.core.epi.util.loading;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import gcm.core.epi.Runner;
import gcm.core.epi.identifiers.*;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.population.*;
import gcm.core.epi.propertytypes.CombinedDeserializerModifier;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsCodeValueDeserializerModifier;
import gcm.core.epi.propertytypes.WeightsDeserializerModifier;
import gcm.core.epi.reports.CustomReport;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedProperty;
import gcm.core.epi.util.property.PropertyGroup;
import gcm.core.epi.util.property.PropertyGroupSpecification;
import org.apache.commons.math3.util.Pair;
import org.simpleflatmapper.csv.CsvParser;
import org.simpleflatmapper.util.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.globals.support.GlobalPropertyId;
import plugins.groups.support.GroupTypeId;
import plugins.personproperties.support.PersonPropertyId;
import plugins.properties.support.PropertyDefinition;
import plugins.regions.support.RegionId;
import plugins.regions.support.RegionPropertyId;
import util.MultiKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CoreEpiBootstrapUtil {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);
    private final Map<MultiKey, PopulationDescription> populationDescriptionCache = new HashMap<>();

    public static PopulationDescription loadPopulationDescriptionWithoutCache(List<Path> inputFiles, String identifier,
                                                                              AgeGroupPartition ageGroupPartition) {
        // Builder to store population description
        final ImmutablePopulationDescription.Builder populationDescriptionBuilder = ImmutablePopulationDescription.builder();

        // Store the string identifier
        populationDescriptionBuilder.id(identifier);

        // Store the AgeGroupPartition
        populationDescriptionBuilder.ageGroupPartition(ageGroupPartition);

        // Each key is a pair of the contact group type and the string id in the file (assumed to be unique for type)
        final Map<Pair<GroupTypeId, String>, Integer> groupIndexMap = new LinkedHashMap<>();
        Counter groupCounter = new Counter();

        // This will be used to consolidate RegionId object references
        Map<String, StringRegionId> censusTractRegionIdMap = new HashMap<>();

        for (Path inputFile : inputFiles) {

            CloseableIterator<PopulationDescriptionFileRecord> populationDescriptionFileRecordIterator;
            try {
                populationDescriptionFileRecordIterator = CsvParser
                        .mapTo(PopulationDescriptionFileRecord.class)
                        .iterator(inputFile.toFile());
            } catch (IOException e) {
                throw new RuntimeException("Cannot read input file: " + inputFile.toFile());
            }

            while (populationDescriptionFileRecordIterator.hasNext()) {

                PopulationDescriptionFileRecord populationDescriptionFileRecord =
                        populationDescriptionFileRecordIterator.next();

                // Add new person to the population description
                String homeTractString = populationDescriptionFileRecord.homeId().substring(0, 11);
                RegionId homeRegionId = censusTractRegionIdMap.computeIfAbsent(homeTractString,
                        StringRegionId::of);
                // Add region
                populationDescriptionBuilder.addRegionByPersonId(homeRegionId);
                // Add age group
                populationDescriptionBuilder.addAgeGroupIndexByPersonId(ageGroupPartition.getAgeGroupIndexFromAge(populationDescriptionFileRecord.age()));

                // Add home
                Integer groupId = groupIndexMap.computeIfAbsent(
                        new Pair<>(ContactGroupType.HOME, populationDescriptionFileRecord.homeId()),
                        (fileGroupId) -> {
                            populationDescriptionBuilder.addRegionByGroupId(PopulationDescription.NO_REGION_ID);
                            return groupCounter.increment().getCount();
                        }
                );
                populationDescriptionBuilder.addHomeGroupIdByPersonId(groupId);

                // Add school if present
                if (populationDescriptionFileRecord.schoolId().isPresent()) {
                    groupId = groupIndexMap.computeIfAbsent(
                            new Pair<>(ContactGroupType.SCHOOL, populationDescriptionFileRecord.schoolId().get()),
                            (fileGroupId) -> {
                                populationDescriptionBuilder.addRegionByGroupId(PopulationDescription.NO_REGION_ID);
                                return groupCounter.increment().getCount();
                            }
                    );
                    populationDescriptionBuilder.addSchoolGroupIdByPersonId(groupId);
                } else {
                    populationDescriptionBuilder.addSchoolGroupIdByPersonId(PopulationDescription.NO_GROUP_ASSIGNED);
                }

                // Add workplace if present
                if (populationDescriptionFileRecord.workplaceId().isPresent()) {
                    String workplaceId = populationDescriptionFileRecord.workplaceId().get();
                    // Corresponding census tract is the first 11 characters of the workplace ID
                    String workplaceTractString = workplaceId.substring(1, 12);
                    RegionId workplaceRegionId = censusTractRegionIdMap.computeIfAbsent(workplaceTractString,
                            StringRegionId::of);
                    groupId = groupIndexMap.computeIfAbsent(
                            new Pair<>(ContactGroupType.WORK, workplaceId),
                            (fileGroupId) -> {
                                populationDescriptionBuilder.addRegionByGroupId(workplaceRegionId);
                                return groupCounter.increment().getCount();
                            }
                    );
                    populationDescriptionBuilder.addWorkGroupIdByPersonId(groupId);
                } else {
                    populationDescriptionBuilder.addWorkGroupIdByPersonId(PopulationDescription.NO_GROUP_ASSIGNED);
                }
            }
        }

        return populationDescriptionBuilder.build();
    }

    public static AgeGroupPartition loadAgeGroupsFromFile(Path file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .registerModule(new Jdk8Module());
        return ImmutableAgeGroupPartition.of(objectMapper.readValue(file.toFile(),
                new TypeReference<List<AgeGroup>>() {
                }));
    }

    /*
        Return the Path that corresponds to the given string after substituting for home directory if applicable
     */
    public static Path getPathFromRelativeString(String stringPath) {
        if (stringPath.startsWith("~")) {
            return Paths.get(System.getProperty("user.home"), stringPath.substring(1));
        } else {
            return Paths.get(stringPath);
        }
    }

    /**
     * Converts a Set of Strings to a Set of Enums of a given getType
     *
     * @param strings Set of Strings
     * @param type    Class to convert Strings to. Simply E.class
     * @param <E>     Class name of Enum
     * @return Set of Enums
     */
    public static <E extends Enum<E>> Set<E> getSetOfEnumsFromStringSet(final Set<String> strings, final Class<E> type) {
        Set<E> propertyItems;
        if (strings.isEmpty())
            propertyItems = EnumSet.allOf(type);
        else
            propertyItems = strings.stream() //
                    .map(item -> E.valueOf(type, item)) //
                    .collect(Collectors.toSet());
        return propertyItems;
    }

    /**
     * Converts a Set of Strings to a Set of PersonPropertyIds taking into account any potential plugins
     *
     * @param strings    The Set of Strings to be converted
     * @param pluginList The List of Plugins used in this experiment
     * @return Set of PersonPropertyIds
     */
    public static Set<PersonPropertyId> getPersonPropertyIdsFromStringSet(final Set<String> strings, List<Plugin> pluginList) {
        Set<PersonPropertyId> personPropertyIds = new HashSet<>();
        // Base person properties
        for (PersonProperty personProperty : PersonProperty.values()) {
            if (strings.contains(personProperty.toString())) {
                personPropertyIds.add(personProperty);
            }
        }
        // Person properties added by plugins
        // TODO: Deal with potential collisions among property ids in plugins (for now adds all of them)
        for (Plugin plugin : pluginList) {
            for (PersonPropertyId personPropertyId : plugin.getPersonProperties()) {
                if (strings.contains(personPropertyId.toString())) {
                    personPropertyIds.add(personPropertyId);
                }
            }
        }
        return personPropertyIds;
    }

    /**
     * Converts a Set of Strings to a Set of RegionPropertyIds taking into account any potential plugins
     *
     * @param strings    The Set of Strings to be converted
     * @param pluginList The List of Plugins used in this experiment
     * @return Set of RegionPropertyIds
     */
    public static Set<RegionPropertyId> getRegionPropertyIdsFromStringSet(final Set<String> strings, List<Plugin> pluginList) {
        Set<RegionPropertyId> regionPropertyIds = new HashSet<>();
        // Base person properties
        for (RegionProperty regionProperty : RegionProperty.values()) {
            if (strings.contains(regionProperty.toString())) {
                regionPropertyIds.add(regionProperty);
            }
        }
        // Person properties added by plugins
        // TODO: Deal with potential collisions among property ids in plugins (for now adds all of them)
        for (Plugin plugin : pluginList) {
            for (RegionPropertyId regionPropertyId : plugin.getRegionProperties()) {
                if (strings.contains(regionPropertyId.toString())) {
                    regionPropertyIds.add(regionPropertyId);
                }
            }
        }
        return regionPropertyIds;
    }

    /**
     * Converts a Set of Strings to a Set of GlobalPropertyIds taking into account any potential plugins
     *
     * @param strings    The Set of Strings to be converted
     * @param pluginList The List of Plugins used in this experiment
     * @return Set of PersonPropertyIds
     */
    public static Set<GlobalPropertyId> getGlobalPropertyIdsFromStringSet(final Set<String> strings, List<Plugin> pluginList) {
        Set<GlobalPropertyId> globalPropertyIds = new HashSet<>();
        // Base person properties

        for (GlobalPropertyId globalProperty : GlobalProperty.values()) {
            if (strings.contains(globalProperty.toString())) {
                globalPropertyIds.add(globalProperty);
            }
        }
        // Person properties added by plugins
        // TODO: Deal with potential collisions among property ids in plugins (for now adds all of them)
        for (Plugin plugin : pluginList) {
            for (GlobalPropertyId globalPropertyId : plugin.getGlobalProperties()) {
                if (strings.contains(globalPropertyId.toString())) {
                    globalPropertyIds.add(globalPropertyId);
                }
            }
        }
        return globalPropertyIds;
    }

    private static Object getPropertyValueFromJson(ObjectMapper objectMapper, JsonNode jsonNode,
                                                   JavaType javaType) throws IOException {
        return objectMapper.readerFor(javaType).readValue(jsonNode);
    }

    public static Object getPropertyValueFromJson(JsonNode jsonNode, JavaType javaType,
                                                  AgeGroupPartition ageGroupPartition) throws IOException {
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
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .registerModule(new Jdk8Module())
                .registerModule(deserializationModule);
        return getPropertyValueFromJson(objectMapper, jsonNode, javaType);
    }

    public static List<RegionFileRecord> loadRegionsFromFile(Path file) {

        ObjectMapper csvMapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        try (
                MappingIterator<RegionFileRecord> censusTractFileRecordMappingIterator = csvMapper
                        .readerFor(RegionFileRecord.class)
                        .with(schema)
                        .readValues(file.toFile())) {
            return censusTractFileRecordMappingIterator.readAll();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

    }

    /**
     * Gets the LoadableReport from the corresponding enum string
     *
     * @param report The string name of the report
     */
    public static LoadableReport getLoadableReportFromString(String report) {
        try {
            return CommonReport.valueOf(report);
        } catch (IllegalArgumentException notCommonReport) {
            try {
                return CustomReport.valueOf(report);
            } catch (IllegalArgumentException notCustomReport) {
                throw new IllegalArgumentException("Unknown report type in configuration: " + report);
            }
        }
    }

    /**
     * This will load the population description by concatenating the data in the specified input files
     * Results are cached. Work is performed by the loadPopulationDescriptionWithoutCache method.
     *
     * @param inputFiles        The list of files that contain the data for the population description
     * @param identifier        The string id that will be used in output reporting to name this population description
     * @param ageGroupPartition The AgeGroupPartition that will be used for all simulations in this experiment
     * @return The population description that contains the data from the listed files
     */
    public PopulationDescription loadPopulationDescription(List<Path> inputFiles, String identifier,
                                                           AgeGroupPartition ageGroupPartition) {

        MultiKey callKey = new MultiKey(inputFiles, identifier, ageGroupPartition);
        return populationDescriptionCache.computeIfAbsent(callKey,
                (x) -> loadPopulationDescriptionWithoutCache(inputFiles, identifier, ageGroupPartition));
    }

    public PopulationDescription loadPopulationDescriptionFromFile(Path inputFile,
                                                                   AgeGroupPartition ageGroupPartition) {
        return loadPopulationDescription(Collections.singletonList(inputFile),
                inputFile.toString(),
                ageGroupPartition);
    }

    public PopulationDescription loadPopulationDescriptionFromDirectory(Path inputDirectory,
                                                                        AgeGroupPartition ageGroupPartition) {
        // Select only CSV files from the directory
        try {
            return loadPopulationDescription(Files.list(inputDirectory)
                            .filter(file -> file.toString().endsWith(".csv"))
                            .sorted()
                            .collect(Collectors.toList()),
                    inputDirectory.toString(),
                    ageGroupPartition);
        } catch (IOException e) {
            e.printStackTrace(); // TODO: Handle exceptions appropriately
            return ImmutablePopulationDescription.builder().id("Empty Population").build();
        }
    }

    public PopulationDescription loadPopulationDescriptionFromDirectory(Path inputDirectory, String identifier,
                                                                        AgeGroupPartition ageGroupPartition) {
        // Select only CSV files from the directory
        try {
            return loadPopulationDescription(Files.list(inputDirectory)
                            .filter(file -> file.toString().endsWith(".csv"))
                            .sorted()
                            .collect(Collectors.toList()),
                    identifier,
                    ageGroupPartition);
        } catch (IOException e) {
            e.printStackTrace(); // TODO: Handle exceptions appropriately
            return ImmutablePopulationDescription.builder().id("Empty Population").build();
        }
    }

    /**
     * Loads all of the person properties from the model, selected plugins, and scenarios into the experiment builder
     *
     * @param experimentBuilder The ExperimentBuilder for the experiment
     * @param pluginList        The List of Plugins used in this experiment
     * @param configuration     CoreFluConfiguration configuration
     * @param objectMapper      The ObjectMapper to use to parse YAML/JSON
     * @param inputPath         The path used for resolving file locations when given as parameter values in the input
     * @throws IOException When there is an exception reading from any input file
     */
    public void loadGlobalProperties(ExperimentBuilder experimentBuilder,
                                     List<Plugin> pluginList,
                                     CoreEpiConfiguration configuration,
                                     ObjectMapper objectMapper,
                                     Path inputPath,
                                     AgeGroupPartition ageGroupPartition) throws
            IOException {
        // First get string mappings for all external global properties
        Map<String, DefinedGlobalProperty> externalGlobalProperties = new HashMap<>();

        // Main global properties
        for (GlobalProperty globalProperty : GlobalProperty.values()) {
            if (globalProperty.isExternalProperty()) {
                externalGlobalProperties.put(globalProperty.toString(), globalProperty);
            }
            experimentBuilder.defineGlobalProperty(globalProperty,
                    globalProperty.getPropertyDefinition().definition());
        }

        // Plugin global properties
        for (Plugin plugin : pluginList) {
            for (DefinedGlobalProperty definedGlobalProperty : plugin.getGlobalProperties()) {
                if (definedGlobalProperty.isExternalProperty()) {
                    externalGlobalProperties.put(definedGlobalProperty.toString(), definedGlobalProperty);
                }
            }
        }

        // Load property values for scenarios into experiment
        for (Map.Entry<String, DefinedGlobalProperty> entry : externalGlobalProperties.entrySet()) {
            String propertyName = entry.getKey();
            DefinedGlobalProperty definedGlobalProperty = entry.getValue();
            PropertyValueJsonList propertyValueJsonList = configuration.scenarios().get(propertyName);
            if (propertyValueJsonList != null) {
                for (JsonNode jsonNode : propertyValueJsonList.jsonNodeList()) {
                    final Object result = parseJsonInput(objectMapper, jsonNode, inputPath, definedGlobalProperty,
                            ageGroupPartition);
                    experimentBuilder.addGlobalPropertyValue(definedGlobalProperty, result);
                }
            } else {
                logger.warn("Warning: External property " + propertyName + " is not defined in configuration file");
            }
        }

        // Handle external property covariation
        for (PropertyGroupSpecification propertyGroupSpecification : configuration.propertyGroups()) {
            PropertyGroup covariationGroup = PropertyGroup.of(propertyGroupSpecification.name());
            for (String propertyName : propertyGroupSpecification.properties()) {
                DefinedGlobalProperty property = externalGlobalProperties.get(propertyName);
                if (property != null) {
                    experimentBuilder.covaryGlobalProperty(property, covariationGroup);
                } else {
                    throw new IllegalArgumentException("Configuration file includes an invalid property name: " +
                            propertyName);
                }
            }
            // Add labels if provided as a group property for reporting
            if (propertyGroupSpecification.labels().size() > 0) {
                experimentBuilder.defineGlobalProperty(covariationGroup, PropertyDefinition.builder()
                        .setType(String.class).setPropertyValueMutability(false).build());
                experimentBuilder.covaryGlobalProperty(covariationGroup, covariationGroup);
                for (String label : propertyGroupSpecification.labels()) {
                    experimentBuilder.addGlobalPropertyValue(covariationGroup, label);
                }
                // Force inclusion in experiment columns
                experimentBuilder.forceGlobalPropertyExperimentColumn(covariationGroup);
            }
        }

        // Force inclusion of certain properties in experiment columns
        for (String propertyName : configuration.forcedExperimentColumnProperties()) {
            DefinedGlobalProperty property = externalGlobalProperties.get(propertyName);
            if (property != null) {
                experimentBuilder.forceGlobalPropertyExperimentColumn(property);
            } else {
                logger.warn("Warning: forcedExperimentColumnProperties references an undefined global property: " +
                        propertyName);
            }
        }

    }

    /**
     * Parses the given JSON node extracted from the input file that corresponds to a scenario parameter value
     *
     * @param objectMapper      The ObjectMapper used to perform the parsing
     * @param jsonNode          The JSON node of the input file representing the input parameter value
     * @param basePath          The path used for resolving file locations in input parameter strings
     * @param property          The DefinedProperty that is to be parsed from the input string
     * @param ageGroupPartition The AgeGroupPartition that is being used for all simulations in this experiment
     * @return The parsed value of the parameter
     * @throws IOException When there is an exception reading from any input file
     */
    private Object parseJsonInput(ObjectMapper objectMapper, JsonNode jsonNode,
                                  Path basePath, DefinedProperty property,
                                  AgeGroupPartition ageGroupPartition) throws IOException {
        /*
            First try to convert the jsonNode to the parameter value in question.
            Next, see if it can be interpreted as a string YAML file, and then load from Immutables
            If that fails, try a specialty loader (generally presuming the input is a file/directory)
            Otherwise, throw an exception
         */
        List<CheckedSupplier<Object>> parsingMethods = new ArrayList<>();

        parsingMethods.add(() -> getPropertyValueFromJson(objectMapper, jsonNode, property.getPropertyDefinition().javaType()));

        final Object basePropertyValue = objectMapper.treeToValue(jsonNode, Object.class);
        if (String.class.isAssignableFrom(basePropertyValue.getClass())) {
            String stringPathForLoading = (String) basePropertyValue;
            if (stringPathForLoading.endsWith(".yaml")) {
                // Load from YAML file via Immutables
                logger.info(property + ": loading from file " + stringPathForLoading);
                parsingMethods.add(() ->
                        objectMapper.readValue(basePath.resolve(stringPathForLoading).toFile(),
                                property.getPropertyDefinition().javaType()));
            } else {
                // Look for specialty loader
                if (property.equals(GlobalProperty.POPULATION_DESCRIPTION)) {
                    logger.info(property + ": loading from file " + stringPathForLoading);
                    Path pathForLoading = basePath.resolve(stringPathForLoading);
                    if (pathForLoading.toFile().isFile()) {
                        parsingMethods.add(() -> loadPopulationDescriptionFromFile(pathForLoading, ageGroupPartition));
                    } else {
                        parsingMethods.add(() -> loadPopulationDescriptionFromDirectory(pathForLoading, ageGroupPartition));
                    }
                }
            }
        }

        for (CheckedSupplier<Object> checkedSupplier : parsingMethods) {
            try {
                return checkedSupplier.get();
            } catch (IOException e) {
                // Try next parsing method
            }
        }
        throw new RuntimeException("Cannot parse " + property + " from input " + basePropertyValue);
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws IOException;
    }

    private static class Counter {
        private int count = -1;

        public int getCount() {
            return count;
        }

        public Counter increment() {
            count++;
            return this;
        }
    }

}
