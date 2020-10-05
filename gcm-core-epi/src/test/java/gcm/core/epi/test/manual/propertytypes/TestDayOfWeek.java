package gcm.core.epi.test.manual.propertytypes;

import gcm.components.Component;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.propertytypes.DayOfWeekSchedule;
import gcm.core.epi.propertytypes.ImmutableDayOfWeekSchedule;
import gcm.output.OutputItem;
import gcm.scenario.*;
import gcm.simulation.*;
import gcm.simulation.group.GroupSampler;
import gcm.simulation.partition.*;
import gcm.util.MemoryPartition;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TestDayOfWeek {

    static DayOfWeek getDayOfWeek(Environment environment, double time) {
        DayOfWeek simulationStartDay = environment.getGlobalPropertyValue(GlobalProperty.SIMULATION_START_DAY);
        return simulationStartDay.plus((long) Math.floor(time));
    }

    @Test
    public void test() {

        Environment environment = new DummyEnvironment(DayOfWeek.SUNDAY);

        DayOfWeekSchedule schedule = ImmutableDayOfWeekSchedule.builder()
                .addActiveDays(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
                .startOffset(3.0)
                .duration(7.0)
                .restartOffset(6.0)
                .build();

        // Starts on SUNDAY, so false
        assertEquals(getDayOfWeek(environment, 0.0), DayOfWeek.SUNDAY);
        assertEquals(schedule.isActiveAt(environment, 0.0), false);

        // Time 1.0 is MONDAY, but not past the start offset so false
        assertEquals(getDayOfWeek(environment, 1.0), DayOfWeek.MONDAY);
        assertEquals(schedule.isActiveAt(environment, 1.0), false);

        // Time 2.9999 is TUESDAY, and not past the start offset so false
        assertEquals(getDayOfWeek(environment, 2.9999), DayOfWeek.TUESDAY);
        assertEquals(schedule.isActiveAt(environment, 2.9999), false);

        // Time 3.0 is WEDNESDAY, and past the start offset so true
        assertEquals(getDayOfWeek(environment, 3.0), DayOfWeek.WEDNESDAY);
        assertEquals(schedule.isActiveAt(environment, 3.0), true);

        // Time 3.9999 is WEDNESDAY, and past the start offset so true
        assertEquals(getDayOfWeek(environment, 3.9999), DayOfWeek.WEDNESDAY);
        assertEquals(schedule.isActiveAt(environment, 3.9999), true);

        // Time 4.0 is THURSDAY, past offset, but not active day of week so false
        assertEquals(getDayOfWeek(environment, 4.0), DayOfWeek.THURSDAY);
        assertEquals(schedule.isActiveAt(environment, 4.0), false);

        // Time 7.9999 is SUNDAY, past offset, but not active day of week so false
        assertEquals(getDayOfWeek(environment, 7.9999), DayOfWeek.SUNDAY);
        assertEquals(schedule.isActiveAt(environment, 7.9999), false);

        // Time 8.0 is MONDAY, past offset and still in duration so true
        assertEquals(getDayOfWeek(environment, 8.0), DayOfWeek.MONDAY);
        assertEquals(schedule.isActiveAt(environment, 8.0), true);

        // Time 9.9999 is TUESDAY, past offset and still in duration so true
        assertEquals(getDayOfWeek(environment, 9.9999), DayOfWeek.TUESDAY);
        assertEquals(schedule.isActiveAt(environment, 9.9999), true);

        // Time 10.0 is WEDNESDAY, past offset plus duration so false
        assertEquals(getDayOfWeek(environment, 10.0), DayOfWeek.WEDNESDAY);
        assertEquals(schedule.isActiveAt(environment, 10.0), false);

        // Time 15.9999 is MONDAY, before restart so false
        assertEquals(getDayOfWeek(environment, 15.9999), DayOfWeek.MONDAY);
        assertEquals(schedule.isActiveAt(environment, 15.9999), false);

        // Time 16.0 is TUESDAY, after restart so true
        assertEquals(getDayOfWeek(environment, 16.0), DayOfWeek.TUESDAY);
        assertEquals(schedule.isActiveAt(environment, 16.0), true);

        // Time 17.0 is WEDNESDAY, after restart so true
        assertEquals(getDayOfWeek(environment, 17.0), DayOfWeek.WEDNESDAY);
        assertEquals(schedule.isActiveAt(environment, 17.0), true);

        // Time 22.9999 is MONDAY, after restart and within duration so true
        assertEquals(getDayOfWeek(environment, 22.9999), DayOfWeek.MONDAY);
        assertEquals(schedule.isActiveAt(environment, 22.9999), true);

        // Time 23 is MONDAY, after restart but past duration so false
        assertEquals(getDayOfWeek(environment, 23.0), DayOfWeek.TUESDAY);
        assertEquals(schedule.isActiveAt(environment, 23.0), false);

    }

    class DummyEnvironment implements Environment {

        private final DayOfWeek startDayOfWeek;

        DummyEnvironment(DayOfWeek startDayOfWeek) {
            this.startDayOfWeek = startDayOfWeek;
        }

        @Override
        public GroupId addGroup(GroupTypeId groupTypeId) {
            return null;
        }

        @Override
        public PersonId addPerson(RegionId regionId, CompartmentId compartmentId) {
            return null;
        }

        @Override
        public void addPersonToGroup(PersonId personId, GroupId groupId) {

        }

        @Override
        public void addPlan(Plan plan, double planTime) {

        }

        @Override
        public void addPlan(Plan plan, double planTime, Object key) {

        }

        @Override
        public void addPopulationIndex(Filter filter, Object key) {

        }

        @Override
        public void addResourceToRegion(ResourceId resourceId, RegionId regionId, long amount) {

        }

        @Override
        public boolean batchExists(BatchId batchId) {
            return false;
        }

        @Override
        public BatchId convertStageToBatch(StageId stageId, MaterialId materialId, double amount) {
            return null;
        }

        @Override
        public void convertStageToResource(StageId stageId, ResourceId resourceId, long amount) {

        }

        @Override
        public BatchId createBatch(MaterialId materialId, double amount) {
            return null;
        }

        @Override
        public StageId createStage() {
            return null;
        }

        @Override
        public void destroyBatch(BatchId batchId) {

        }

        @Override
        public void destroyStage(StageId stageId, boolean destroyBatches) {

        }

        @Override
        public double getBatchAmount(BatchId batchId) {
            return 0;
        }

        @Override
        public <T extends MaterialId> T getBatchMaterial(BatchId batchId) {
            return null;
        }

        @Override
        public <T> T getBatchProducer(BatchId batchId) {
            return null;
        }

        @Override
        public PropertyDefinition getBatchPropertyDefinition(MaterialId materialId, BatchPropertyId batchPropertyId) {
            return null;
        }

        @Override
        public <T extends BatchPropertyId> Set<T> getBatchPropertyIds(MaterialId materialId) {
            return null;
        }

        @Override
        public double getBatchPropertyTime(BatchId batchId, BatchPropertyId batchPropertyId) {
            return 0;
        }

        @Override
        public <T> T getBatchPropertyValue(BatchId batchId, BatchPropertyId batchPropertyId) {
            return null;
        }

        @Override
        public Optional<StageId> getBatchStageId(BatchId batchId) {
            return Optional.empty();
        }

        @Override
        public double getBatchTime(BatchId batchId) {
            return 0;
        }

        @Override
        public <T extends CompartmentId> Set<T> getCompartmentIds() {
            return null;
        }

        @Override
        public MapOption getCompartmentMapOption() {
            return null;
        }

        @Override
        public int getCompartmentPopulationCount(CompartmentId compartmentId) {
            return 0;
        }

        @Override
        public double getCompartmentPopulationCountTime(CompartmentId compartmentId) {
            return 0;
        }

        @Override
        public PropertyDefinition getCompartmentPropertyDefinition(CompartmentId compartmentId, CompartmentPropertyId compartmentPropertyId) {
            return null;
        }

        @Override
        public <T extends CompartmentPropertyId> Set<T> getCompartmentPropertyIds(CompartmentId compartmentId) {
            return null;
        }

        @Override
        public double getCompartmentPropertyTime(CompartmentId compartmentId, CompartmentPropertyId compartmentPropertyId) {
            return 0;
        }

        @Override
        public <T> T getCompartmentPropertyValue(CompartmentId compartmentId, CompartmentPropertyId compartmentPropertyId) {
            return null;
        }

        @Override
        public <T extends GlobalComponentId> Set<T> getGlobalComponentIds() {
            return null;
        }

        @Override
        public Class<? extends Component> getGlobalComponentClass(GlobalComponentId globalComponentId) {
            return null;
        }

        @Override
        public Class<? extends Component> getCompartmentComponentClass(CompartmentId compartmentId) {
            return null;
        }

        @Override
        public Class<? extends Component> getMaterialsProducerComponentClass(MaterialsProducerId materialsProducerId) {
            return null;
        }

        @Override
        public Class<? extends Component> getRegionComponentClass(RegionId regionId) {
            return null;
        }

        @Override
        public PropertyDefinition getGlobalPropertyDefinition(GlobalPropertyId globalPropertyId) {
            return null;
        }

        @Override
        public <T extends GlobalPropertyId> Set<T> getGlobalPropertyIds() {
            return null;
        }

        @Override
        public double getGlobalPropertyTime(GlobalPropertyId globalPropertyId) {
            return 0;
        }

        @Override
        public <T> T getGlobalPropertyValue(GlobalPropertyId globalPropertyId) {
            // Just return day of week start day
            if (globalPropertyId.equals(GlobalProperty.SIMULATION_START_DAY)) {
                return (T) startDayOfWeek;
            } else {
                throw new IllegalArgumentException("Dummy method called improperly");
            }
        }

        @Override
        public int getGroupCountForGroupType(GroupTypeId groupTypeId) {
            return 0;
        }

        @Override
        public int getGroupCountForGroupTypeAndPerson(GroupTypeId groupTypeId, PersonId personId) {
            return 0;
        }

        @Override
        public int getGroupCountForPerson(PersonId personId) {
            return 0;
        }

        @Override
        public List<GroupId> getGroupIds() {
            return null;
        }

        @Override
        public PropertyDefinition getGroupPropertyDefinition(GroupTypeId groupTypeId, GroupPropertyId groupPropertyId) {
            return null;
        }

        @Override
        public <T extends GroupPropertyId> Set<T> getGroupPropertyIds(GroupTypeId groupTypeId) {
            return null;
        }

        @Override
        public double getGroupPropertyTime(GroupId groupId, GroupPropertyId groupPropertyId) {
            return 0;
        }

        @Override
        public <T> T getGroupPropertyValue(GroupId groupId, GroupPropertyId groupPropertyId) {
            return null;
        }

        @Override
        public List<GroupId> getGroupsForGroupType(GroupTypeId groupTypeId) {
            return null;
        }

        @Override
        public List<GroupId> getGroupsForGroupTypeAndPerson(GroupTypeId groupTypeId, PersonId personId) {
            return null;
        }

        @Override
        public List<GroupId> getGroupsForPerson(PersonId personId) {
            return null;
        }

        @Override
        public <T extends GroupTypeId> T getGroupType(GroupId groupId) {
            return null;
        }

        @Override
        public int getGroupTypeCountForPerson(PersonId personId) {
            return 0;
        }

        @Override
        public <T extends GroupTypeId> Set<T> getGroupTypeIds() {
            return null;
        }

        @Override
        public <T extends GroupTypeId> List<T> getGroupTypesForPerson(PersonId personId) {
            return null;
        }

        @Override
        public List<PersonId> getIndexedPeople(Object key) {
            return null;
        }

        @Override
        public int getIndexSize(Object key) {
            return 0;
        }

        @Override
        public List<BatchId> getInventoryBatches(MaterialsProducerId materialsProducerId) {
            return null;
        }

        @Override
        public List<BatchId> getInventoryBatchesByMaterialId(MaterialsProducerId materialsProducerId, MaterialId materialId) {
            return null;
        }

        @Override
        public <T extends MaterialId> Set<T> getMaterialIds() {
            return null;
        }

        @Override
        public <T extends MaterialsProducerId> Set<T> getMaterialsProducerIds() {
            return null;
        }

        @Override
        public PropertyDefinition getMaterialsProducerPropertyDefinition(MaterialsProducerPropertyId materialsProducerPropertyId) {
            return null;
        }

        @Override
        public <T extends MaterialsProducerPropertyId> Set<T> getMaterialsProducerPropertyIds() {
            return null;
        }

        @Override
        public double getMaterialsProducerPropertyTime(MaterialsProducerId materialsProducerId, MaterialsProducerPropertyId materialsProducerPropertyId) {
            return 0;
        }

        @Override
        public <T> T getMaterialsProducerPropertyValue(MaterialsProducerId materialsProducerId, MaterialsProducerPropertyId materialsProducerPropertyId) {
            return null;
        }

        @Override
        public long getMaterialsProducerResourceLevel(MaterialsProducerId materialsProducerId, ResourceId resourceId) {
            return 0;
        }

        @Override
        public double getMaterialsProducerResourceTime(MaterialsProducerId materialsProducerId, ResourceId resourceId) {
            return 0;
        }

        @Override
        public Optional<PersonId> sampleGroup(GroupId groupId, GroupSampler groupSampler) {
            return Optional.empty();
        }

        @Override
        public ObservableEnvironment getObservableEnvironment() {
            return null;
        }

        @Override
        public List<StageId> getOfferedStages(MaterialsProducerId materialsProducerId) {
            return null;
        }

        @Override
        public List<PersonId> getPeople() {
            return null;
        }

        @Override
        public List<PersonId> getPeopleForGroup(GroupId groupId) {
            return null;
        }

        @Override
        public List<PersonId> getPeopleForGroupType(GroupTypeId groupTypeId) {
            return null;
        }

        @Override
        public List<PersonId> getPeopleInCompartment(CompartmentId compartmentId) {
            return null;
        }

        @Override
        public List<PersonId> getPeopleInRegion(RegionId regionId) {
            return null;
        }

        @Override
        public List<PersonId> getPeopleWithoutResource(ResourceId resourceId) {
            return null;
        }

        @Override
        public List<PersonId> getPeopleWithPropertyValue(PersonPropertyId personPropertyId, Object personPropertyValue) {
            return null;
        }

        @Override
        public int getPersonCountForPropertyValue(PersonPropertyId personPropertyId, Object personPropertyValue) {
            return 0;
        }

        @Override
        public List<PersonId> getPeopleWithResource(ResourceId resourceId) {
            return null;
        }

        @Override
        public <T extends CompartmentId> T getPersonCompartment(PersonId personId) {
            return null;
        }

        @Override
        public double getPersonCompartmentArrivalTime(PersonId personId) {
            return 0;
        }

        @Override
        public TimeTrackingPolicy getPersonCompartmentArrivalTrackingPolicy() {
            return null;
        }

        @Override
        public int getPersonCountForGroup(GroupId groupId) {
            return 0;
        }

        @Override
        public int getPersonCountForGroupType(GroupTypeId groupTypeId) {
            return 0;
        }

        @Override
        public PropertyDefinition getPersonPropertyDefinition(PersonPropertyId personPropertyId) {
            return null;
        }

        @Override
        public <T extends PersonPropertyId> Set<T> getPersonPropertyIds() {
            return null;
        }

        @Override
        public double getPersonPropertyTime(PersonId personId, PersonPropertyId personPropertyId) {
            return 0;
        }

        @Override
        public <T> T getPersonPropertyValue(PersonId personId, PersonPropertyId personPropertyId) {
            return null;
        }

        @Override
        public <T extends RegionId> T getPersonRegion(PersonId personId) {
            return null;
        }

        @Override
        public double getPersonRegionArrivalTime(PersonId personId) {
            return 0;
        }

        @Override
        public TimeTrackingPolicy getPersonRegionArrivalTrackingPolicy() {
            return null;
        }

        @Override
        public long getPersonResourceLevel(PersonId personId, ResourceId resourceId) {
            return 0;
        }

        @Override
        public double getPersonResourceTime(PersonId personId, ResourceId resourceId) {
            return 0;
        }

        @Override
        public TimeTrackingPolicy getPersonResourceTimeTrackingPolicy(ResourceId resourceId) {
            return null;
        }

        @Override
        public <T> Optional<T> getPlan(Object key) {
            return Optional.empty();
        }

        @Override
        public double getPlanTime(Object key) {
            return 0;
        }

        @Override
        public int getPopulationCount() {
            return 0;
        }

        @Override
        public double getPopulationTime() {
            return 0;
        }

        @Override
        public RandomGenerator getRandomGenerator() {
            return null;
        }

        @Override
        public RandomGenerator getRandomGeneratorFromId(RandomNumberGeneratorId randomNumberGeneratorId) {
            return null;
        }

        @Override
        public Optional<PersonId> sampleIndex(Object key) {
            return Optional.empty();
        }

        @Override
        public Optional<PersonId> sampleIndex(Object key, RandomNumberGeneratorId randomNumberGeneratorId) {
            return Optional.empty();
        }

        @Override
        public Optional<PersonId> sampleIndex(Object key, PersonId excludedPersonId) {
            return Optional.empty();
        }

        @Override
        public Optional<PersonId> sampleIndex(Object key, RandomNumberGeneratorId randomNumberGeneratorId, PersonId excludedPersonId) {
            return Optional.empty();
        }

        @Override
        public Set<RegionId> getRegionIds() {
            return null;
        }

        @Override
        public MapOption getRegionMapOption() {
            return null;
        }

        @Override
        public int getRegionPopulationCount(RegionId regionId) {
            return 0;
        }

        @Override
        public double getRegionPopulationCountTime(RegionId regionId) {
            return 0;
        }

        @Override
        public PropertyDefinition getRegionPropertyDefinition(RegionPropertyId regionPropertyId) {
            return null;
        }

        @Override
        public <T extends RegionPropertyId> Set<T> getRegionPropertyIds() {
            return null;
        }

        @Override
        public double getRegionPropertyTime(RegionId regionId, RegionPropertyId regionPropertyId) {
            return 0;
        }

        @Override
        public <T> T getRegionPropertyValue(RegionId regionId, RegionPropertyId regionPropertyId) {
            return null;
        }

        @Override
        public long getRegionResourceLevel(RegionId regionId, ResourceId resourceId) {
            return 0;
        }

        @Override
        public double getRegionResourceTime(RegionId regionId, ResourceId resourceId) {
            return 0;
        }

        @Override
        public ReplicationId getReplicationId() {
            return null;
        }

        @Override
        public <T extends ResourceId> Set<T> getResourceIds() {
            return null;
        }

        @Override
        public PropertyDefinition getResourcePropertyDefinition(ResourceId resourceId, ResourcePropertyId resourcePropertyId) {
            return null;
        }

        @Override
        public <T extends ResourcePropertyId> Set<T> getResourcePropertyIds(ResourceId resourceId) {
            return null;
        }

        @Override
        public double getResourcePropertyTime(ResourceId resourceId, ResourcePropertyId resourcePropertyId) {
            return 0;
        }

        @Override
        public <T> T getResourcePropertyValue(ResourceId resourceId, ResourcePropertyId resourcePropertyId) {
            return null;
        }

        @Override
        public ScenarioId getScenarioId() {
            return null;
        }

        @Override
        public List<BatchId> getStageBatches(StageId stageId) {
            return null;
        }

        @Override
        public List<BatchId> getStageBatchesByMaterialId(StageId stageId, MaterialId materialId) {
            return null;
        }

        @Override
        public <T extends MaterialsProducerId> T getStageProducer(StageId stageId) {
            return null;
        }

        @Override
        public List<StageId> getStages(MaterialsProducerId materialsProducerId) {
            return null;
        }

        @Override
        public int getSuggestedPopulationSize() {
            return 0;
        }

        @Override
        public double getTime() {
            return 0;
        }

        @Override
        public boolean groupExists(GroupId groupId) {
            return false;
        }

        @Override
        public void halt() {

        }

        @Override
        public boolean isGroupMember(PersonId personId, GroupId groupId) {
            return false;
        }

        @Override
        public boolean isStageOffered(StageId stageId) {
            return false;
        }

        @Override
        public void moveBatchToInventory(BatchId batchId) {

        }

        @Override
        public void moveBatchToStage(BatchId batchId, StageId stageId) {

        }

        @Override
        public void observeCompartmentalPersonPropertyChange(boolean observe, CompartmentId compartmentId, PersonPropertyId personPropertyId) {

        }

        @Override
        public void observeCompartmentalPersonResourceChange(boolean observe, CompartmentId compartmentId, ResourceId resourceId) {

        }

        @Override
        public void observeCompartmentPersonArrival(boolean observe, CompartmentId compartmentId) {

        }

        @Override
        public void observeCompartmentPersonDeparture(boolean observe, CompartmentId compartmentId) {

        }

        @Override
        public void observeCompartmentPropertyChange(boolean observe, CompartmentId compartmentId, CompartmentPropertyId compartmentPropertyId) {

        }

        @Override
        public void observeGlobalPersonArrival(boolean observe) {

        }

        @Override
        public void observeGlobalPersonDeparture(boolean observe) {

        }

        @Override
        public void observeGlobalPersonPropertyChange(boolean observe, PersonPropertyId personPropertyId) {

        }

        @Override
        public void observeGlobalPersonResourceChange(boolean observe, ResourceId resourceId) {

        }

        @Override
        public void observeGlobalPropertyChange(boolean observe, GlobalPropertyId globalPropertyId) {

        }

        @Override
        public void observeMaterialsProducerPropertyChange(boolean observe, MaterialsProducerId materialProducerId, MaterialsProducerPropertyId materialsProducerPropertyId) {

        }

        @Override
        public void observeMaterialsProducerResourceChangeByMaterialsProducerId(boolean observe, MaterialsProducerId materialsProducerId, ResourceId resourceId) {

        }

        @Override
        public void observeMaterialsProducerResourceChange(boolean observe, ResourceId resourceId) {

        }

        @Override
        public void observePersonCompartmentChange(boolean observe, PersonId personId) {

        }

        @Override
        public void observePersonPropertyChange(boolean observe, PersonId personId, PersonPropertyId personPropertyId) {

        }

        @Override
        public void observePersonRegionChange(boolean observe, PersonId personId) {

        }

        @Override
        public void observePersonResourceChange(boolean observe, PersonId personId, ResourceId resourceId) {

        }

        @Override
        public void observeRegionPersonArrival(boolean observe, RegionId regionId) {

        }

        @Override
        public void observeRegionPersonDeparture(boolean observe, RegionId regionId) {

        }

        @Override
        public void observeRegionPersonPropertyChange(boolean observe, RegionId regionId, PersonPropertyId personPropertyId) {

        }

        @Override
        public void observeRegionPersonResourceChange(boolean observe, RegionId regionId, ResourceId resourceId) {

        }

        @Override
        public void observeRegionPropertyChange(boolean observe, RegionId regionId, RegionPropertyId regionPropertyId) {

        }

        @Override
        public void observeGlobalRegionPropertyChange(boolean observe, RegionPropertyId regionPropertyId) {

        }

        @Override
        public void observeRegionResourceChange(boolean observe, RegionId regionId, ResourceId resourceId) {

        }

        @Override
        public void observeResourcePropertyChange(boolean observe, ResourceId resourceId, ResourcePropertyId resourcePropertyId) {

        }

        @Override
        public void observeStageOfferChange(boolean observe) {

        }

        @Override
        public void observeStageOfferChangeByStageId(boolean observe, StageId stageId) {

        }

        @Override
        public void observeStageTransfer(boolean observe) {

        }

        @Override
        public void observeStageTransferByStageId(boolean observe, StageId stageId) {

        }

        @Override
        public void observeStageTransferBySourceMaterialsProducerId(boolean observe, MaterialsProducerId sourceMaterialsProducerId) {

        }

        @Override
        public void observeStageTransferByDestinationMaterialsProducerId(boolean observe, MaterialsProducerId destinationMaterialsProducerId) {

        }

        @Override
        public boolean personExists(PersonId personId) {
            return false;
        }

        @Override
        public boolean personIsInPopulationIndex(PersonId personId, Object key) {
            return false;
        }

        @Override
        public boolean populationIndexExists(Object key) {
            return false;
        }

        @Override
        public void removeGroup(GroupId groupId) {

        }

        @Override
        public void removePerson(PersonId personId) {

        }

        @Override
        public void removePersonFromGroup(PersonId personId, GroupId groupId) {

        }

        @Override
        public <T> Optional<T> removePlan(Object key) {
            return Optional.empty();
        }

        @Override
        public void removePopulationIndex(Object key) {

        }

        @Override
        public void removeResourceFromPerson(ResourceId resourceId, PersonId personId, long amount) {

        }

        @Override
        public void removeResourceFromRegion(ResourceId resourceId, RegionId regionId, long amount) {

        }

        @Override
        public void setBatchPropertyValue(BatchId batchId, BatchPropertyId batchPropertyId, Object batchPropertyValue) {

        }

        @Override
        public void setCompartmentPropertyValue(CompartmentId compartmentId, CompartmentPropertyId compartmentPropertyId, Object compartmentPropertyValue) {

        }

        @Override
        public void setGlobalPropertyValue(GlobalPropertyId globalPropertyId, Object globalPropertyValue) {

        }

        @Override
        public void setGroupPropertyValue(GroupId groupId, GroupPropertyId groupPropertyId, Object groupPropertyValue) {

        }

        @Override
        public void setMaterialsProducerPropertyValue(MaterialsProducerId materialsProducerId, MaterialsProducerPropertyId materialsProducerPropertyId, Object materialsProducerPropertyValue) {

        }

        @Override
        public void setPersonCompartment(PersonId personId, CompartmentId compartmentId) {

        }

        @Override
        public void setPersonPropertyValue(PersonId personId, PersonPropertyId personPropertyId, Object personPropertyValue) {

        }

        @Override
        public void setPersonRegion(PersonId personId, RegionId regionId) {

        }

        @Override
        public void setRegionPropertyValue(RegionId regionId, RegionPropertyId regionPropertyId, Object regionPropertyValue) {

        }

        @Override
        public void setResourcePropertyValue(ResourceId resourceId, ResourcePropertyId resourcePropertyId, Object resourcePropertyValue) {

        }

        @Override
        public void setStageOffer(StageId stageId, boolean offer) {

        }

        @Override
        public void shiftBatchContent(BatchId sourceBatchId, BatchId destinationBatchId, double amount) {

        }

        @Override
        public boolean stageExists(StageId stageId) {
            return false;
        }

        @Override
        public void transferOfferedStageToMaterialsProducer(StageId stageId, MaterialsProducerId materialsProducerId) {

        }

        @Override
        public void transferProducedResourceToRegion(MaterialsProducerId materialsProducerId, ResourceId resourceId, RegionId regionId, long amount) {

        }

        @Override
        public void transferResourceBetweenRegions(ResourceId resourceId, RegionId sourceRegionId, RegionId destinationRegionId, long amount) {

        }

        @Override
        public void transferResourceFromPerson(ResourceId resourceId, PersonId personId, long amount) {

        }

        @Override
        public void transferResourceToPerson(ResourceId resourceId, PersonId personId, long amount) {

        }

        @Override
        public <T extends ComponentId> T getCurrentComponentId() {
            return null;
        }

        @Override
        public void releaseOutputItem(OutputItem outputItem) {

        }

        @Override
        public void observeGroupArrival(boolean observe) {

        }

        @Override
        public void observeGroupArrivalByPerson(boolean observe, PersonId personId) {

        }

        @Override
        public void observeGroupArrivalByType(boolean observe, GroupTypeId groupTypeId) {

        }

        @Override
        public void observeGroupArrivalByGroup(boolean observe, GroupId groupId) {

        }

        @Override
        public void observeGroupArrivalByTypeAndPerson(boolean observe, GroupTypeId groupTypeId, PersonId personId) {

        }

        @Override
        public void observeGroupArrivalByGroupAndPerson(boolean observe, GroupId groupId, PersonId personId) {

        }

        @Override
        public void observeGroupDeparture(boolean observe) {

        }

        @Override
        public void observeGroupDepartureByPerson(boolean observe, PersonId personId) {

        }

        @Override
        public void observeGroupDepartureByType(boolean observe, GroupTypeId groupTypeId) {

        }

        @Override
        public void observeGroupDepartureByGroup(boolean observe, GroupId groupId) {

        }

        @Override
        public void observeGroupDepartureByTypeAndPerson(boolean observe, GroupTypeId groupTypeId, PersonId personId) {

        }

        @Override
        public void observePopulationIndexChange(boolean observe, Object key) {

        }

        @Override
        public void observePartitionChange(boolean observe, Object key) {

        }

        @Override
        public void observeGroupDepartureByGroupAndPerson(boolean observe, GroupId groupId, PersonId personId) {

        }

        @Override
        public void observeGroupConstruction(boolean observe) {

        }

        @Override
        public void observeGroupConstructionByType(boolean observe, GroupTypeId groupTypeId) {

        }

        @Override
        public void observeGroupDestruction(boolean observe) {

        }

        @Override
        public void observeGroupDestructionByGroup(boolean observe, GroupId groupId) {

        }

        @Override
        public void observeGroupDestructionByType(boolean observe, GroupTypeId groupTypeId) {

        }

        @Override
        public void observeGroupPropertyChange(boolean observe) {

        }

        @Override
        public void observeGroupPropertyChangeByType(boolean observe, GroupTypeId groupTypeId) {

        }

        @Override
        public void observeGroupPropertyChangeByTypeAndProperty(boolean observe, GroupTypeId groupTypeId, GroupPropertyId groupPropertyId) {

        }

        @Override
        public void observeGroupPropertyChangeByGroup(boolean observe, GroupId groupId) {

        }

        @Override
        public void observeGroupPropertyChangeByGroupAndProperty(boolean observe, GroupId groupId, GroupPropertyId groupPropertyId) {

        }

        @Override
        public <T> T getProfiledProxy(T instance) {
            return null;
        }

        @Override
        public List<Object> getPlanKeys() {
            return null;
        }

        @Override
        public <T extends RandomNumberGeneratorId> Set<T> getRandomNumberGeneratorIds() {
            return null;
        }

        @Override
        public void addGlobalComponent(GlobalComponentId globalComponentId, Class<? extends Component> globalComponentClass) {

        }

        @Override
        public List<PersonId> getPartitionPeople(Object key, LabelSet labelSet) {
            return null;
        }

        @Override
        public List<PersonId> getPartitionPeople(Object key) {
            return null;
        }

        @Override
        public void addPartition(Partition partition, Object key) {

        }

        @Override
        public int getPartitionSize(Object key, LabelSet labelSet) {
            return 0;
        }

        @Override
        public void removePartition(Object key) {

        }

        @Override
        public boolean populationPartitionExists(Object key) {
            return false;
        }

        @Override
        public boolean personIsInPopulationPartition(PersonId personId, Object key, LabelSet labelSet) {
            return false;
        }

        @Override
        public Optional<PersonId> samplePartition(Object key, PartitionSampler partitionSampler) {
            return Optional.empty();
        }

        @Override
        public void init(Context context) {

        }

        @Override
        public void collectMemoryLinks(MemoryPartition memoryPartition) {

        }
    }

}
