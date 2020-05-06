package gcm.core.epi.util.configsplit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import gcm.core.epi.util.loading.CoreEpiConfiguration;
import gcm.core.epi.util.loading.ImmutableCoreEpiConfiguration;
import gcm.core.epi.util.property.ImmutablePropertyGroupSpecification;
import gcm.core.epi.util.property.PropertyGroupSpecification;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A main method utility for generating multiple input config files from a
 * single config file that specifies a large experiment. The resultant config
 * files form a complete partition of the implied experiment space.
 *
 * @author Shawn Hatch
 */
public class ConfigurationSplitter {

    private final Path inputConfigPath;
    private final Path targetDirectory;
    private final Path inputDirectory;
    private final Path outputDirectory;
    private final List<String> nonSplittingVariables;

    private ConfigurationSplitter(Path inputConfigPath, Path targetDirectory, Path inputDirectory, Path outputDirectory, List<String> nonSplittingVariables) {
        this.inputConfigPath = inputConfigPath;
        this.targetDirectory = targetDirectory;
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.nonSplittingVariables = new ArrayList<>(nonSplittingVariables);
    }

    private static String getPaddedString(int maxValue, int value) {
        int maxLength = Integer.toString(maxValue).length();
        String result = Integer.toString(value);
        while (result.length() < maxLength) {
            result = "0" + result;
        }
        return result;
    }

    public static void main(String[] args) throws Exception {

        // The input config file to split
        Path inputConfigPath = Paths.get(args[0]);

        // The directory where the resultant config files will be written
        Path targetDirectory = Paths.get(args[1]);

        // The input directory path that will be listed in the config files
        Path inputDirectory = Paths.get(args[2]);

        // The output directory path that will be listed in the config files,
        // where each config file will specify a sub-directory for its output
        Path outputDirectory = Paths.get(args[3]);

        // The list of scenario fields that are to remain as-is in the resultant
        // config files, even when they have multiple values. Dropping a
        // variable from the split algorithm will drop all other variables that are
        // covariant as well.
        List<String> nonSplittingVariables = new ArrayList<>();
        for (int i = 4; i < args.length; i++) {
            nonSplittingVariables.add(args[i]);
        }

        new ConfigurationSplitter(inputConfigPath, targetDirectory, inputDirectory, outputDirectory, nonSplittingVariables).execute();

    }

    private void clearTargetDirectory() {
        File dir = targetDirectory.toFile();

        for (File file : dir.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }

    }

    private void execute() throws Exception {

        clearTargetDirectory();

        // get a CoreFluConfiguration instance from the input config path
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()).enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY).registerModule(new Jdk8Module());
        CoreEpiConfiguration configuration = objectMapper.readValue(inputConfigPath.toFile(), ImmutableCoreEpiConfiguration.class);
        configuration.propertyGroups().forEach(propertyGroup -> {
            propertyGroup.labels();
        });

        // determine the name of the input config path to use as the root name
        // of the resultant config files
        String rootConfigName = inputConfigPath.toFile().getName();
        rootConfigName = rootConfigName.substring(0, rootConfigName.length() - 5);

        Map<String, CovariantGroup> covariantMap = new LinkedHashMap<>();
        Map<PropertyGroupSpecification, CovariantGroup> propertyGroupSpecificationToCovariantGroupMap = new LinkedHashMap<>();
        for (PropertyGroupSpecification propertyGroupSpecification : configuration.propertyGroups()) {
            CovariantGroup covariantGroup = new CovariantGroup();
            propertyGroupSpecificationToCovariantGroupMap.put(propertyGroupSpecification, covariantGroup);
            covariantGroup.name = propertyGroupSpecification.name();
            covariantGroup.labels.addAll(propertyGroupSpecification.labels());
            List<String> list = propertyGroupSpecification.properties();
            for (String item : list) {
                covariantMap.put(item, covariantGroup);
            }
        }

        // Loop through the scenario entries, associating each entry with a
        // covariant group and adding it to that group

        Map<String, List<JsonNode>> entries = configuration.scenarios();
        for (String key : entries.keySet()) {
            List<JsonNode> list = entries.get(key);

            CovariantGroup covariantGroup = covariantMap.get(key);
            if (covariantGroup == null) {
                covariantGroup = new CovariantGroup();
                covariantGroup.name = key;
                covariantMap.put(key, covariantGroup);
            }
            if (covariantGroup.variantCount < 0) {
                covariantGroup.variantCount = list.size();
            } else {
                if (covariantGroup.variantCount != list.size()) {
                    throw new RuntimeException("variable " + key + " is not of the correct size for covariant group " + covariantGroup.name);
                }
            }
            covariantGroup.entries.put(key, list);
        }

        for (CovariantGroup covariantGroup : covariantMap.values()) {
            if (covariantGroup.labels.size() > 0) {
                if (covariantGroup.labels.size() != covariantGroup.variantCount) {
                    throw new RuntimeException("The number of group labels for covaiant group " + covariantGroup.name + " does not match the variant count of its constituent variables");
                }
            }
        }

        /*
         * We determine which fields need to be forced by adopting any that were
         * in the original config file and adding the ones implied by the tuples
         */
        Set<String> forcedExperimentColumnProperties = new LinkedHashSet<>(configuration.forcedExperimentColumnProperties());
        for (CovariantGroup covariantGroup : covariantMap.values()) {
            if (covariantGroup.variantCount > 1) {
                forcedExperimentColumnProperties.addAll(covariantGroup.entries.keySet());
            }
        }

        // Inactivate the covariantGroups that correspond to the non splitting
        // variables
        for (String nonSplittingVariable : nonSplittingVariables) {
            CovariantGroup covariantGroup = covariantMap.get(nonSplittingVariable);
            if (covariantGroup != null) {
                covariantGroup.active = false;
            }
        }

        Set<CovariantGroup> activeCovarinantGroups = covariantMap.values().stream().filter(covariant -> covariant.active).collect(Collectors.toSet());
        Set<CovariantGroup> inactiveCovarinantGroups = covariantMap.values().stream().filter(covariant -> !covariant.active).collect(Collectors.toSet());
        // establish the modulus for each covariant group that will aid in
        // determining which variant value to use for each generated input
        // yaml
        int fileCount = 1;
        for (CovariantGroup covariantGroup : activeCovarinantGroups) {
            covariantGroup.modulus = fileCount;
            fileCount *= covariantGroup.variantCount;
        }

        int scenariosPerFile = 1;
        for (CovariantGroup covariantGroup : inactiveCovarinantGroups) {
            scenariosPerFile *= covariantGroup.variantCount;
        }

        // create the split config files
        for (int fileCounter = 0; fileCounter < fileCount; fileCounter++) {
            Map<String, List<JsonNode>> newEntries = new LinkedHashMap<>(entries);

            /*
             * For each entry in the covariant group, use the JsonNode from the
             * original entries that corresponds to the current tuple value and
             * place that in the new entries
             */
            for (CovariantGroup covariantGroup : activeCovarinantGroups) {
                for (String key : covariantGroup.entries.keySet()) {
                    List<JsonNode> list = covariantGroup.entries.get(key);
                    List<JsonNode> newList = new ArrayList<>();
                    int valueIndex = (fileCounter / covariantGroup.modulus) % covariantGroup.variantCount;
                    newList.add(list.get(valueIndex));
                    newEntries.put(key, newList);
                }

            }

            List<PropertyGroupSpecification> newPropertyGroupSpecifications = new ArrayList<>();

            for (PropertyGroupSpecification propertyGroupSpecification : configuration.propertyGroups()) {
                CovariantGroup covariantGroup = propertyGroupSpecificationToCovariantGroupMap.get(propertyGroupSpecification);
                if (covariantGroup.active && covariantGroup.labels.size() > 0) {
                    int valueIndex = (fileCounter / covariantGroup.modulus) % covariantGroup.variantCount;
                    String label = covariantGroup.labels.get(valueIndex);
                    ImmutablePropertyGroupSpecification newPropertyGroupSpecification = ImmutablePropertyGroupSpecification.builder().name(propertyGroupSpecification.name())
                            .addAllProperties(propertyGroupSpecification.properties()).addLabels(label)
                            .build();
                    newPropertyGroupSpecifications.add(newPropertyGroupSpecification);
                } else {
                    newPropertyGroupSpecifications.add(propertyGroupSpecification);
                }
            }

            ImmutableCoreEpiConfiguration newCoreEpiConfiguration = //
                    ImmutableCoreEpiConfiguration.builder()//
                            .from(configuration)//
                            .baseScenarioId(fileCounter * scenariosPerFile)//
                            .forcedExperimentColumnProperties(forcedExperimentColumnProperties).inputDirectory(inputDirectory.toString())//
                            .outputDirectory(outputDirectory.resolve("output_" + getPaddedString(fileCount, fileCounter + 1)).toString())//
                            .scenarios(newEntries)//
                            .propertyGroups(newPropertyGroupSpecifications).build();//

            objectMapper = new ObjectMapper(new YAMLFactory()).registerModule(new Jdk8Module());

            File file = targetDirectory.resolve(rootConfigName + "_" + getPaddedString(fileCount, fileCounter + 1) + ".yaml").toFile();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                objectMapper //
                        .writerWithDefaultPrettyPrinter() //
                        .writeValue(fos, newCoreEpiConfiguration);
            }
        }
    }

    private static class CovariantGroup {
        String name;
        int modulus;
        int variantCount = -1;
        boolean active = true;
        Map<String, List<JsonNode>> entries = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
    }
}
