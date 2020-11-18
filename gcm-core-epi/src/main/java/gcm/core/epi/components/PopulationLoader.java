package gcm.core.epi.components;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.*;
import gcm.core.epi.population.HospitalData;
import gcm.core.epi.population.ImmutableHospitalData;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import gcm.core.epi.propertytypes.ImmutableInfectionData;
import gcm.core.epi.util.loading.HospitalDataFileRecord;
import gcm.scenario.GroupId;
import gcm.scenario.PersonId;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;
import gcm.simulation.PersonConstructionInfo;
import gcm.simulation.Plan;
import gcm.simulation.partition.LabelSet;
import gcm.simulation.partition.Partition;
import gcm.simulation.partition.PartitionSampler;
import gcm.util.geolocator.GeoLocator;
import org.apache.commons.math3.distribution.BinomialDistribution;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

public class PopulationLoader extends AbstractComponent {

    /*
        Adds a the specified person to the group with id given by groupIdInteger
            If the group does not exist it creates it with the specified type
            This function requires that new groups when added by integer id are added sequentially
     */
    private static void addPersonToGroupWithType(Environment environment, PersonId personId, Integer groupIdInteger,
                                                 ContactGroupType contactGroupType, List<RegionId> groupRegionIds) {
        if (!groupIdInteger.equals(PopulationDescription.NO_GROUP_ASSIGNED)) {
            GroupId groupId = new GroupId(groupIdInteger);
            if (!environment.groupExists(groupId)) {
                // Adds new group, checking that the group ID matches what is expected in the population description
                if (environment.addGroup(contactGroupType).getValue() != groupId.getValue()) {
                    throw new RuntimeException("Group ID assignment expected to be sequential");
                }
            } else if (environment.getGroupType(groupId) != contactGroupType) {
                throw new RuntimeException("Group ID has the incorrect type");
            }
            environment.addPersonToGroup(personId, groupId);
            if (contactGroupType == ContactGroupType.WORK) {
                RegionId groupRegionId = groupRegionIds.get(groupIdInteger);
                if (!groupRegionId.equals(PopulationDescription.NO_REGION_ID)) {
                    environment.setGroupPropertyValue(groupId, WorkplaceProperty.REGION_ID, groupRegionId);
                }
            }
        }
    }

    @Override
    public void init(Environment environment) {
        environment.addPlan(new InitializePopulationPlan(), 0.0);
    }

    @Override
    public void executePlan(Environment environment, Plan plan) {
        PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);

        // Iterate over people, adding them to the simulation together with their groups
        for (RegionId regionId : populationDescription.regionByPersonId()) {
            // Add person and set properties
            // This relies on the fact that person ids are assigned sequentially
            int personIdInt = environment.getPopulationCount();
            PersonConstructionInfo personConstructionInfo = PersonConstructionInfo.builder()
                    .setPersonRegionId(regionId)
                    .setPersonCompartmentId(Compartment.SUSCEPTIBLE)
                    .setPersonPropertyValue(PersonProperty.AGE_GROUP_INDEX,
                            populationDescription.ageGroupIndexByPersonId().get(personIdInt))
                    .build();
            PersonId personId = environment.addPerson(personConstructionInfo);
            if (personId.getValue() != personIdInt) {
                throw new RuntimeException("Person ID assignment expected to be sequential");
            }

            // Add groups
            addPersonToGroupWithType(environment, personId, populationDescription.homeGroupIdByPersonId().get(personIdInt),
                    ContactGroupType.HOME, populationDescription.regionByGroupId());

            addPersonToGroupWithType(environment, personId, populationDescription.schoolGroupIdByPersonId().get(personIdInt),
                    ContactGroupType.SCHOOL, populationDescription.regionByGroupId());

            addPersonToGroupWithType(environment, personId, populationDescription.workGroupIdByPersonId().get(personIdInt),
                    ContactGroupType.WORK, populationDescription.regionByGroupId());
        }

        // Set up hospitals, if needed
        String hospitalInputFile = environment.getGlobalPropertyValue(GlobalProperty.HOSPITAL_DATA_FILE);
        if (!hospitalInputFile.equals("")) {
            Path hospitalInputPath = Paths.get(System.getProperty("user.dir")).resolve(hospitalInputFile);
            ObjectMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            try (
                    MappingIterator<HospitalDataFileRecord> hospitalDataFileRecordMappingIterator = csvMapper
                            .readerFor(HospitalDataFileRecord.class)
                            .with(schema)
                            .readValues(hospitalInputPath.toFile())) {

                List<HospitalDataFileRecord> hospitalDataFileRecordList = hospitalDataFileRecordMappingIterator
                        .readAll();

//                double bedStaffRatio = environment.getGlobalPropertyValue(GlobalProperty.HOSPITAL_BED_STAFF_RATIO);

                // The regionIds used in this simulation (for filtering)
                Set<RegionId> regionIds =
                        ((PopulationDescription) environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION))
                                .regionIds();

//                // The worker flow data to be used to assign hospital staff
//                Path workerFlowInputPath = Paths.get(System.getProperty("user.dir")).resolve(
//                        (String) environment.getGlobalPropertyValue(GlobalProperty.REGION_WORKER_FLOW_DATA_FILE));


//                // Build up outflow data for region ids in the simulation to parameterize enumerated random distributions
//                Map<RegionId, List<Pair<RegionId, Double>>> outflowData = new HashMap<>();
//
//                try (MappingIterator<RegionWorkFlowFileRecord> regionWorkerFlowFileRecordMappingIterator =
//                             csvMapper.readerFor(RegionWorkFlowFileRecord.class)
//                                     .with(schema)
//                                     .readValues(workerFlowInputPath.toFile())) {
//                    while (regionWorkerFlowFileRecordMappingIterator.hasNext()) {
//                        RegionWorkFlowFileRecord workFlowFileRecord = regionWorkerFlowFileRecordMappingIterator.next();
//                        RegionId targetRegionId = StringRegionId.of(workFlowFileRecord.targetRegionId());
//                        RegionId sourceRegionId = StringRegionId.of(workFlowFileRecord.sourceRegionId());
//                        if (regionIds.contains(targetRegionId) & regionIds.contains(sourceRegionId)) {
//                            List<Pair<RegionId, Double>> outflows = outflowData.computeIfAbsent(targetRegionId,
//                                    regionId -> new ArrayList<>());
//                            outflows.add(new Pair<>(sourceRegionId, workFlowFileRecord.outflowFraction()));
//                        }
//                    }
//                }


//                // Finally assemble the distributions
//                Map<RegionId, EnumeratedDistribution<RegionId>> outflowDistributions = outflowData.entrySet().stream()
//                        .collect(Collectors.toMap(
//                                Map.Entry::getKey,
//                                entry -> new EnumeratedDistribution<>(environment.getRandomGeneratorFromId(
//                                        RandomId.HOSPITAL_WORKPLACE_ASSIGNMENT), entry.getValue())
//                        ));

                // Build indices to choose random workers
//                int totalWorkers = 0;
//                for (RegionId regionId : regionIds) {
//                    FilterBuilder filterBuilder = new FilterBuilder();
//                    filterBuilder.openAnd();
//                    filterBuilder.addRegion(regionId);
//                    filterBuilder.addGroupsForPersonAndGroupType(ContactGroupType.WORK, Equality.GREATER_THAN_EQUAL, 1);
//                    filterBuilder.closeLogical();
//                    environment.addPopulationIndex(filterBuilder.build(), WorkerRegionIdKey.of(regionId));
//                    totalWorkers += environment.getIndexedPeople(WorkerRegionIdKey.of(regionId)).size();
//                }

                // Assemble the hospital data
                List<HospitalData> hospitalDataList = new ArrayList<>();
                for (HospitalDataFileRecord hospitalDataFileRecord : hospitalDataFileRecordList) {
                    RegionId hospitalRegionId = StringRegionId.of(hospitalDataFileRecord.regionId());
                    if (regionIds.contains(hospitalRegionId)) {
                        ImmutableHospitalData.Builder hospitalDataBuilder = ImmutableHospitalData.builder();

                        hospitalDataBuilder
                                .regionId(hospitalRegionId)
                                .beds(hospitalDataFileRecord.beds());
                        // Add staff
//                        int staff = (int) Math.ceil(hospitalDataFileRecord.beds() * bedStaffRatio);
                        GroupId hospitalStaffGroupId = environment.addGroup(ContactGroupType.WORK);
//                        EnumeratedDistribution<RegionId> outflowDistribution = outflowDistributions.get(hospitalRegionId);
//                        int staffAssigned = 0;
//                        while (staffAssigned < Math.min(staff, totalWorkers)) {
//                            RegionId staffHomeRegionId = outflowDistribution.sample();
//                            Optional<PersonId> staffCandidate = environment.getRandomIndexedPersonFromGenerator(
//                                    WorkerRegionIdKey.of(staffHomeRegionId), RandomId.HOSPITAL_WORKPLACE_ASSIGNMENT);
//                            if (staffCandidate.isPresent()) {
//                                PersonId staffId = staffCandidate.get();
//                                // There must be at least one workplace (and we know there is only one), so take first
//                                GroupId workplaceId = environment.getGroupsForGroupTypeAndPerson(ContactGroupType.WORK,
//                                        staffId).get(0);
//                                environment.removePersonFromGroup(staffId, workplaceId);
//                                environment.addPersonToGroup(staffId, hospitalStaffGroupId);
//                                staffAssigned++;
//                            }
//                        }
                        hospitalDataBuilder.staffWorkplaceGroup(hospitalStaffGroupId);

                        // Add patient group
                        GroupId hospitalPatientsGroupId = environment.addGroup(HospitalGroupType.PATIENTS);
                        hospitalDataBuilder.patientGroup(hospitalPatientsGroupId);

                        // Add bed resources
                        double hospitalBedOccupancy = environment.getGlobalPropertyValue(GlobalProperty.HOSPITAL_BED_OCCUPANCY);
                        int bedsAvailable = new BinomialDistribution(environment.getRandomGeneratorFromId(
                                RandomId.HOSPITAL_WORKPLACE_ASSIGNMENT), hospitalDataFileRecord.beds(),
                                1 - hospitalBedOccupancy).sample();
                        environment.addResourceToRegion(Resource.HOSPITAL_BED, hospitalRegionId, bedsAvailable);

                        hospitalDataList.add(hospitalDataBuilder.build());
                    }
                }

                // Store hospital data
                environment.setGlobalPropertyValue(GlobalProperty.HOSPITAL_DATA, hospitalDataList);

                // Remove indices
//                for (RegionId regionId : regionIds) {
//                    environment.removePopulationIndex(WorkerRegionIdKey.of(regionId));
//                }

                // Add GeoLocator for hospitals
                GeoLocator.Builder<HospitalData> geoLocatorBuilder = GeoLocator.builder();
                for (HospitalData hospitalData : hospitalDataList) {
                    RegionId regionId = hospitalData.regionId();
                    double lat = environment.getRegionPropertyValue(regionId, RegionProperty.LAT);
                    double lon = environment.getRegionPropertyValue(regionId, RegionProperty.LON);
                    geoLocatorBuilder.addLocation(lat, lon, hospitalData);
                }
                environment.setGlobalPropertyValue(GlobalProperty.HOSPITAL_GEOLOCATOR, geoLocatorBuilder.build());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Initial Infections
        FipsCodeDouble initialInfectionSpecification = environment.getGlobalPropertyValue(GlobalProperty.INITIAL_INFECTIONS);
        Map<FipsCode, Double> initialInfections = initialInfectionSpecification.getFipsCodeValues(environment);
        final Object initialInfectionPartitionKey = new Object();

        // Make a partition for susceptible people
        environment.addPartition(Partition.builder()
                        .setRegionFunction(regionId -> initialInfectionSpecification.scope().getFipsSubCode(regionId))
                        .setCompartmentFunction(compartmentId -> compartmentId == Compartment.SUSCEPTIBLE)
                        .build(),
                initialInfectionPartitionKey);

        // Infect random susceptible people from the selected regions
        initialInfections.forEach(
                (key, value) -> IntStream.range(0, (int) Math.round(value))
                        .forEach(
                                i -> {
                                    Optional<PersonId> targetPersonId = environment.samplePartition(
                                            initialInfectionPartitionKey, PartitionSampler.builder()
                                                    .setLabelSet(LabelSet.builder()
                                                        .setRegionLabel(key)
                                                            // Only use those in the susceptible compartment
                                                        .setCompartmentLabel(true)
                                                        .build())
                                                    .setRandomNumberGeneratorId(RandomId.INITIAL_INFECTIONS)
                                                    .build());
                                    // Will only infect if there are susceptible people that remain
                                    targetPersonId.ifPresent(personId -> {
                                                environment.setPersonCompartment(personId, Compartment.INFECTED);
                                                // Store the data about this infection event for reporting
                                                environment.setGlobalPropertyValue(GlobalProperty.MOST_RECENT_INFECTION_DATA,
                                                        Optional.of(ImmutableInfectionData.builder()
                                                                .targetPersonId(personId)
                                                                .transmissionSetting(ContactGroupType.GLOBAL)
                                                                .transmissionOccurred(true)
                                                                .build()));
                                            }
                                    );
                                }
                        ));

        // Remove partition that is no longer needed
        environment.removePartition(initialInfectionPartitionKey);

    }

    private static class InitializePopulationPlan implements Plan {
        // No data associated with this plan
    }

//    private static class WorkerRegionIdKey {
//        private final RegionId regionId;
//
//        private WorkerRegionIdKey(RegionId regionId) {
//            this.regionId = regionId;
//        }
//
//        private static WorkerRegionIdKey of(RegionId regionId) {
//            return new WorkerRegionIdKey(regionId);
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            WorkerRegionIdKey that = (WorkerRegionIdKey) o;
//            return Objects.equals(regionId, that.regionId);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(regionId);
//        }
//    }

}
