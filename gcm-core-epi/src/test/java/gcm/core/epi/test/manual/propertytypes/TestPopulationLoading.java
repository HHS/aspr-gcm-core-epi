package gcm.core.epi.test.manual.propertytypes;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.StringRegionId;
import gcm.core.epi.population.*;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import gcm.core.epi.util.loading.PopulationDescriptionFileRecord;
import gcm.scenario.GroupTypeId;
import gcm.scenario.RegionId;
import org.apache.commons.math3.util.Pair;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;
import org.simpleflatmapper.csv.CsvParser;
import org.simpleflatmapper.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TestPopulationLoading {

    private static final String AGE_GROUP_FILE = "~/Desktop/Coreflu/input/transmission/default-age-groups-5-classes.yaml";
    private static final String POPULATION_FILE = "~/Desktop/Coreflu/input/population/all_states/va.csv";

    @Test
    public void test() throws IOException {

        final Path ageGroupFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(AGE_GROUP_FILE);
        AgeGroupPartition ageGroupPartition = CoreEpiBootstrapUtil.loadAgeGroupsFromFile(ageGroupFilePath);

        final Path populationFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(POPULATION_FILE);

        CoreEpiBootstrapUtil coreEpiBootstrapUtil = new CoreEpiBootstrapUtil();
        long startTime = java.lang.System.currentTimeMillis();
        PopulationDescription populationDescription = coreEpiBootstrapUtil.loadPopulationDescriptionFromFile(populationFilePath, ageGroupPartition);
        long endTime = java.lang.System.currentTimeMillis();

        System.out.println(populationDescription.toString() + " loaded in " + (endTime-startTime)/1000.0 + "s");

    }

    @Test
    public void testParts() throws IOException {

        final Path ageGroupFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(AGE_GROUP_FILE);
        AgeGroupPartition ageGroupPartition = CoreEpiBootstrapUtil.loadAgeGroupsFromFile(ageGroupFilePath);

        final Path populationFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(POPULATION_FILE);

        // Read synthetic population from csv files
        ObjectMapper csvMapper = new CsvMapper();
        Jdk8Module jdk8Module = new Jdk8Module();
        //jdk8Module.configureAbsentsAsNulls(true);
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        long startTime = java.lang.System.currentTimeMillis();
        //byte[] bytes = Files.readAllBytes(populationFilePath);
        MappingIterator<PopulationDescriptionFileRecord> peopleIterator = csvMapper
                .registerModule(jdk8Module)
                //.registerModule(new AfterburnerModule())
                .readerFor(PopulationDescriptionFileRecord.class)
                .with(schema)
                .readValues(populationFilePath.toFile());
        List<PopulationDescriptionFileRecord> peopleList = peopleIterator.readAll();
        long endTime = java.lang.System.currentTimeMillis();

        System.out.println(peopleList.get(0) + populationFilePath.toString() + " loaded in " + (endTime-startTime)/1000.0 + "s");

    }

    @Test
    public void testPartsSimpleFlatMapper() throws IOException {

        final Path ageGroupFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(AGE_GROUP_FILE);
        AgeGroupPartition ageGroupPartition = CoreEpiBootstrapUtil.loadAgeGroupsFromFile(ageGroupFilePath);

        final Path populationFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(POPULATION_FILE);

        // Read synthetic population from csv files

        long startTime = java.lang.System.currentTimeMillis();
        //byte[] bytes = Files.readAllBytes(populationFilePath);
        List<PopulationDescriptionFileRecord> peopleList = CsvParser
                .mapTo(PopulationDescriptionFileRecord.class)
                .stream(populationFilePath.toFile(), stream -> stream.collect(Collectors.toList()));
        long endTime = java.lang.System.currentTimeMillis();

        System.out.println(peopleList.get(0) + populationFilePath.toString() + " loaded in " + (endTime-startTime)/1000.0 + "s");

        System.out.println(GraphLayout.parseInstance(peopleList).toFootprint());

    }

    @Test
    public void testLoadingSimpleFlatMapper() throws IOException {

        final Path ageGroupFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(AGE_GROUP_FILE);
        AgeGroupPartition ageGroupPartition = CoreEpiBootstrapUtil.loadAgeGroupsFromFile(ageGroupFilePath);

        final Path populationFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(POPULATION_FILE);
        String identifier = populationFilePath.toString();

        // Read synthetic population from csv files

        long startTime = java.lang.System.currentTimeMillis();

        // Builder to store population description
        ImmutablePopulationDescription.Builder populationDescriptionBuilder = ImmutablePopulationDescription.builder();

        // Store the string identifier
        populationDescriptionBuilder.id(identifier);

        // Store the AgeGroupPartition
        populationDescriptionBuilder.ageGroupPartition(ageGroupPartition);

        // Each key is a pair of the contact group type and the string id in the file (assumed to be unique for type)
        final Map<Pair<GroupTypeId, String>, ImmutableGroupSpecification.Builder> groupSpecificationBuilderMap = new LinkedHashMap<>();

        // Id of the next person to be added to the simulation.
        int personCounter = 0;

        // This will be used to consolidate RegionId object references
        Map<String, StringRegionId> censusTractRegionIdMap = new HashMap<>();

        CloseableIterator<PopulationDescriptionFileRecord> populationDescriptionFileRecordIterator = CsvParser
                .mapTo(PopulationDescriptionFileRecord.class)
                .iterator(populationFilePath.toFile());

        System.out.println("Setup: " + (System.currentTimeMillis() - startTime)/1000.0);

        // Iterate over the file and add data
        while (populationDescriptionFileRecordIterator.hasNext()) {

            PopulationDescriptionFileRecord populationDescriptionFileRecord =
                    populationDescriptionFileRecordIterator.next();

            // Add new person to the population description
            String homeTractString = populationDescriptionFileRecord.homeId().substring(0, 11);
            RegionId homeRegionId = censusTractRegionIdMap.computeIfAbsent(homeTractString,
                    StringRegionId::of);
            PersonData personData = ImmutablePersonData.builder()
                    .regionId(homeRegionId)
//                            .putPersonPropertyValues(PersonProperty.AGE_GROUP_INDEX, 0)
                    .putPersonPropertyValues(PersonProperty.AGE_GROUP_INDEX,
                            ageGroupPartition.getAgeGroupIndexFromAge(populationDescriptionFileRecord.age()))
                    .build();
            populationDescriptionBuilder.addDataByPersonId(personData);

            // Add home
            ImmutableGroupSpecification.Builder groupSpecificationBuilder = groupSpecificationBuilderMap.computeIfAbsent(
                    new Pair<>(ContactGroupType.HOME, populationDescriptionFileRecord.homeId()),
                    (fileGroupId) -> ImmutableGroupSpecification.builder().groupType(ContactGroupType.HOME)
            );
            groupSpecificationBuilder.addGroupMembers(personCounter);

            // Add school if present
            if (populationDescriptionFileRecord.schoolId().isPresent()) {
                groupSpecificationBuilder = groupSpecificationBuilderMap.computeIfAbsent(
                        new Pair<>(ContactGroupType.SCHOOL, populationDescriptionFileRecord.schoolId().get()),
                        (fileGroupId) -> ImmutableGroupSpecification.builder().groupType(ContactGroupType.SCHOOL)
                );
                groupSpecificationBuilder.addGroupMembers(personCounter);
            }

            // Add workplace if present
            if (populationDescriptionFileRecord.workplaceId().isPresent()) {
                String workplaceId = populationDescriptionFileRecord.workplaceId().get();
                // Corresponding census tract is the first 11 characters of the workplace ID
                String workplaceTractString = workplaceId.substring(1, 12);
                RegionId workplaceRegionId = censusTractRegionIdMap.computeIfAbsent(workplaceTractString,
                        StringRegionId::of);
                groupSpecificationBuilder = groupSpecificationBuilderMap.computeIfAbsent(
                        new Pair<>(ContactGroupType.WORK, workplaceId),
                        (fileGroupId) -> ImmutableGroupSpecification.builder().groupType(ContactGroupType.WORK)
                );
                groupSpecificationBuilder.regionId(workplaceRegionId);
                groupSpecificationBuilder.addGroupMembers(personCounter);
            }

            // Increment counter
            personCounter++;

        }
        System.out.println("Finished Parsing: " + (System.currentTimeMillis() - startTime)/1000.0);

        // Build the group specifications
        for (Map.Entry<Pair<GroupTypeId, String>, ImmutableGroupSpecification.Builder> entry :
                groupSpecificationBuilderMap.entrySet()) {
            populationDescriptionBuilder.addGroupSpecificationByGroupId(entry.getValue().build());
        }
        PopulationDescription populationDescription = populationDescriptionBuilder.build();
        long endTime = java.lang.System.currentTimeMillis();

        System.out.println(populationDescription + " loaded in " + (endTime-startTime)/1000.0 + "s");
        System.out.println(populationDescription.dataByPersonId().size());

        System.out.println(GraphLayout.parseInstance(populationDescription).toFootprint());
    }

    @Test
    public void testLoadingSimpleFlatMapperCompact() throws IOException {

        final Path ageGroupFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(AGE_GROUP_FILE);
        AgeGroupPartition ageGroupPartition = CoreEpiBootstrapUtil.loadAgeGroupsFromFile(ageGroupFilePath);

        final Path populationFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(POPULATION_FILE);
        String identifier = populationFilePath.toString();

        // Read synthetic population from csv files

        long startTime = java.lang.System.currentTimeMillis();

        // Builder to store population description
        final ImmutableCompactPopulationDescription.Builder populationDescriptionBuilder = ImmutableCompactPopulationDescription.builder();

        // Store the string identifier
        populationDescriptionBuilder.id(identifier);

        // Store the AgeGroupPartition
        populationDescriptionBuilder.ageGroupPartition(ageGroupPartition);

        // Each key is a pair of the contact group type and the string id in the file (assumed to be unique for type)
        final Map<Pair<GroupTypeId, String>, Integer> groupIndexMap = new LinkedHashMap<>();
        Counter groupCounter = new Counter();

        // This will be used to consolidate RegionId object references
        Map<String, StringRegionId> censusTractRegionIdMap = new HashMap<>();

        CloseableIterator<PopulationDescriptionFileRecord> populationDescriptionFileRecordIterator = CsvParser
                .mapTo(PopulationDescriptionFileRecord.class)
                .iterator(populationFilePath.toFile());

        System.out.println("Setup: " + (System.currentTimeMillis() - startTime)/1000.0);

        // Iterate over the file and add data
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
                        //populationDescriptionBuilder.addGroupTypeByGroupId(ContactGroupType.HOME);
                        return groupCounter.increment().getCount();
                    }
            );
            populationDescriptionBuilder.addHomeGroupIdByPersonId(groupId);

            // Add school if present
            if (populationDescriptionFileRecord.schoolId().isPresent()) {
                groupId = groupIndexMap.computeIfAbsent(
                        new Pair<>(ContactGroupType.SCHOOL, populationDescriptionFileRecord.schoolId().get()),
                        (fileGroupId) -> {
                            //populationDescriptionBuilder.addGroupTypeByGroupId(ContactGroupType.SCHOOL);
                            return groupCounter.increment().getCount();
                        }
                );
                populationDescriptionBuilder.addSchoolGroupIdByPersonId(groupId);
            } else {
                populationDescriptionBuilder.addSchoolGroupIdByPersonId(-1);
            }

            // Add workplace if present
            if (populationDescriptionFileRecord.workplaceId().isPresent()) {
                String workplaceId = populationDescriptionFileRecord.workplaceId().get();
//                // Corresponding census tract is the first 11 characters of the workplace ID
//                String workplaceTractString = workplaceId.substring(1, 12);
//                RegionId workplaceRegionId = censusTractRegionIdMap.computeIfAbsent(workplaceTractString,
//                        StringRegionId::of);
                groupId = groupIndexMap.computeIfAbsent(
                        new Pair<>(ContactGroupType.WORK, workplaceId),
                        (fileGroupId) -> {
                            //populationDescriptionBuilder.addGroupTypeByGroupId(ContactGroupType.WORK);
                            return groupCounter.increment().getCount();
                        }
                );
                populationDescriptionBuilder.addWorkGroupIdByPersonId(groupId);
            } else {
                populationDescriptionBuilder.addWorkGroupIdByPersonId(-1);
            }
        }
        System.out.println("Finished Parsing: " + (System.currentTimeMillis() - startTime)/1000.0);

        CompactPopulationDescription populationDescription = populationDescriptionBuilder.build();
        long endTime = java.lang.System.currentTimeMillis();

        System.out.println(populationDescription + " loaded in " + (endTime-startTime)/1000.0 + "s");
        System.out.println(populationDescription.ageGroupIndexByPersonId().size());

        System.out.println(GraphLayout.parseInstance(populationDescription).toFootprint());
    }

    @Test
    public void testLoadingSimpleFlatMapperAltCompact() throws IOException {

        final Path ageGroupFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(AGE_GROUP_FILE);
        AgeGroupPartition ageGroupPartition = CoreEpiBootstrapUtil.loadAgeGroupsFromFile(ageGroupFilePath);

        final Path populationFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(POPULATION_FILE);
        String identifier = populationFilePath.toString();

        // Read synthetic population from csv files

        long startTime = java.lang.System.currentTimeMillis();

        // Builder to store population description
        final ImmutableAltCompactPopulationDescription.Builder populationDescriptionBuilder =
                ImmutableAltCompactPopulationDescription.builder();

        // Store the string identifier
        populationDescriptionBuilder.id(identifier);

        // Store the AgeGroupPartition
        populationDescriptionBuilder.ageGroupPartition(ageGroupPartition);

        // Person property arrays
        final List<Object> ageGroupIndexByPersonId = new ArrayList<>();

        // Group membership map
        final Map<GroupTypeId, List<Integer>> groupIdByTypeAndPersonId = new HashMap<>();

        // Each key is a pair of the contact group type and the string id in the file (assumed to be unique for type)
        final Map<Pair<GroupTypeId, String>, Integer> groupIndexMap = new LinkedHashMap<>();
        Counter groupCounter = new Counter();

        // This will be used to consolidate RegionId object references
        Map<String, StringRegionId> censusTractRegionIdMap = new HashMap<>();

        CloseableIterator<PopulationDescriptionFileRecord> populationDescriptionFileRecordIterator = CsvParser
                .mapTo(PopulationDescriptionFileRecord.class)
                .iterator(populationFilePath.toFile());

        System.out.println("Setup: " + (System.currentTimeMillis() - startTime)/1000.0);

        // Iterate over the file and add data
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
            ageGroupIndexByPersonId.add(ageGroupPartition.getAgeGroupIndexFromAge(populationDescriptionFileRecord.age()));

            // Add home
            List<Integer> membershipList = groupIdByTypeAndPersonId.computeIfAbsent(ContactGroupType.HOME, groupTypeId -> new ArrayList<>());
            Integer groupId = groupIndexMap.computeIfAbsent(
                    new Pair<>(ContactGroupType.HOME, populationDescriptionFileRecord.homeId()),
                    fileGroupId -> groupCounter.increment().getCount()
            );
            membershipList.add(groupId);

            // Add school if present
            membershipList = groupIdByTypeAndPersonId.computeIfAbsent(ContactGroupType.SCHOOL, groupTypeId -> new ArrayList<>());
            if (populationDescriptionFileRecord.schoolId().isPresent()) {
                groupId = groupIndexMap.computeIfAbsent(
                        new Pair<>(ContactGroupType.SCHOOL, populationDescriptionFileRecord.schoolId().get()),
                        fileGroupId -> groupCounter.increment().getCount()
                );
                membershipList.add(groupId);
            } else {
                membershipList.add(-1);
            }

            // Add workplace if present
            membershipList = groupIdByTypeAndPersonId.computeIfAbsent(ContactGroupType.WORK, groupTypeId -> new ArrayList<>());
            if (populationDescriptionFileRecord.workplaceId().isPresent()) {
                String workplaceId = populationDescriptionFileRecord.workplaceId().get();
//                // Corresponding census tract is the first 11 characters of the workplace ID
//                String workplaceTractString = workplaceId.substring(1, 12);
//                RegionId workplaceRegionId = censusTractRegionIdMap.computeIfAbsent(workplaceTractString,
//                        StringRegionId::of);
                groupId = groupIndexMap.computeIfAbsent(
                        new Pair<>(ContactGroupType.WORK, workplaceId),
                        (fileGroupId) -> groupCounter.increment().getCount()
                );
                membershipList.add(groupId);
            } else {
                membershipList.add(-1);
            }


        }
        System.out.println("Finished Parsing: " + (System.currentTimeMillis() - startTime)/1000.0);

        populationDescriptionBuilder.putPersonPropertyValueByPersonId(PersonProperty.AGE_GROUP_INDEX, ageGroupIndexByPersonId);
        populationDescriptionBuilder.putAllGroupIdByTypeAndPersonId(groupIdByTypeAndPersonId);
        AltCompactPopulationDescription populationDescription = populationDescriptionBuilder.build();
        long endTime = java.lang.System.currentTimeMillis();

        System.out.println(populationDescription + " loaded in " + (endTime-startTime)/1000.0 + "s");
        System.out.println(populationDescription.personPropertyValueByPersonId().get(PersonProperty.AGE_GROUP_INDEX).size());

        System.out.println(GraphLayout.parseInstance(populationDescription).toFootprint());
    }

    class Counter {
        private int count = -1;

        public int getCount() {
            return count;
        }

        public Counter increment()  {
            count++;
            return this;
        }

    }

}
