package gcm.core.epi.identifiers;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.core.epi.plugin.behavior.BehaviorPlugin;
import gcm.core.epi.plugin.infection.ExponentialPeriodInfectionPlugin;
import gcm.core.epi.plugin.infection.InfectionPlugin;
import gcm.core.epi.plugin.transmission.TransmissionPlugin;
import gcm.core.epi.plugin.vaccine.VaccinePlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.HospitalData;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.*;
import gcm.core.epi.trigger.TriggerContainer;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.scenario.RegionId;
import gcm.scenario.RegionPropertyId;
import gcm.util.geolocator.GeoLocator;
import org.apache.commons.math3.distribution.EnumeratedDistribution;

import java.time.DayOfWeek;
import java.util.*;


public enum GlobalProperty implements DefinedGlobalProperty {

    AGE_WEIGHTS_TEST(TypedPropertyDefinition.builder()
            .type(AgeWeights.class).defaultValue(ImmutableAgeWeights.builder().build()).build()),

    AVERAGE_TRANSMISSION_RATIO(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    POPULATION_DESCRIPTION(TypedPropertyDefinition.builder()
            .type(PopulationDescription.class).isMutable(false).build()),

    TRANSMISSION_STRUCTURE(TypedPropertyDefinition.builder()
            .type(TransmissionStructure.class).isMutable(false).build()),

    TRANSMISSION_RATIOS(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<AgeGroup, Double>()).build(), false),

    FRACTION_OF_GLOBAL_CONTACTS_IN_HOME_REGION(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.1).isMutable(false).build()),

    RADIATION_FLOW_GEOLOCATOR(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<GeoLocator<Object>>() {
            })
            .defaultValue(getDefaultGeoLocator()).build(), false),

    RADIATION_FLOW_MAX_RADIUS_KM(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(100.0).isMutable(false).build()),

    RADIATION_FLOW_TARGET_DISTRIBUTIONS(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<RegionId, EnumeratedDistribution<RegionId>>>() {
            })
            .defaultValue(new HashMap<RegionId, EnumeratedDistribution<RegionId>>()).build(), false),

    INITIAL_INFECTIONS(TypedPropertyDefinition.builder()
            .type(FipsCodeDouble.class)
            .defaultValue(ImmutableFipsCodeDouble.builder().build())
            .isMutable(false).build()),

    FRACTION_SYMPTOMATIC(TypedPropertyDefinition.builder()
            .type(AgeWeights.class).defaultValue(ImmutableAgeWeights.builder().build()).isMutable(false).build()),

    ASYMPTOMATIC_INFECTIOUSNESS(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(1.0).isMutable(false).build()),

    MOST_RECENT_INFECTION_DATA(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Optional<InfectionData>>() {
            })
            .defaultValue(Optional.empty()).build(), false),

    HOSPITAL_DATA(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<List<HospitalData>>() {
            })
            .defaultValue(new ArrayList<HospitalData>()).build(), false),

    HOSPITAL_DATA_FILE(TypedPropertyDefinition.builder()
            .type(String.class).defaultValue("").isMutable(false).build()),

    HOSPITAL_BED_STAFF_RATIO(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    HOSPITAL_BED_OCCUPANCY(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    REGION_WORKER_FLOW_DATA_FILE(TypedPropertyDefinition.builder()
            .type(String.class).defaultValue("").isMutable(false).build()),

    HOSPITAL_GEOLOCATOR(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<GeoLocator<Object>>() {
            })
            .defaultValue(getDefaultGeoLocator()).build(), false),

    CASE_HOSPITALIZATION_RATIO(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<>()).isMutable(false).build()),

    HOSPITALIZATION_DELAY_MEAN(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<>()).isMutable(false).build()),

    HOSPITALIZATION_DELAY_SD(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<>()).isMutable(false).build()),

    HOSPITALIZATION_DURATION_MEAN(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<>()).isMutable(false).build()),

    HOSPITALIZATION_DURATION_SD(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<>()).isMutable(false).build()),

    HOSPITALIZATION_MAX_RADIUS_KM(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    CASE_FATALITY_RATIO(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<>()).isMutable(false).build()),

    HOSPITALIZATION_TO_DEATH_DELAY_MEAN(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<>()).isMutable(false).build()),

    HOSPITALIZATION_TO_DEATH_DELAY_SD(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<AgeGroup, Double>>() {
            })
            .defaultValue(new HashMap<>()).isMutable(false).build()),

    INFECTION_PLUGIN(TypedPropertyDefinition.builder()
            .type(InfectionPlugin.class).defaultValue(new ExponentialPeriodInfectionPlugin()).build(), false),

    BEHAVIOR_PLUGIN(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Optional<BehaviorPlugin>>() {
            })
            .defaultValue(Optional.empty()).build(), false),

    TRANSMISSION_PLUGIN(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Optional<TransmissionPlugin>>() {
            })
            .defaultValue(Optional.empty()).build(), false),

    VACCINE_PLUGIN(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Optional<VaccinePlugin>>() {
            })
            .defaultValue(Optional.empty()).build(), false),

    TRIGGER_CONTAINER(TypedPropertyDefinition.builder()
            .type(TriggerContainer.class).isMutable(false).build(), false),

    TRIGGER_CALLBACKS(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<String, Set<RegionPropertyId>>>() {
            })
            .defaultValue(new HashMap<String, Set<RegionPropertyId>>()).build(), false),

    MAX_SIMULATION_LENGTH(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(Double.POSITIVE_INFINITY).build()),

    SIMULATION_START_DAY(TypedPropertyDefinition.builder()
            .type(DayOfWeek.class).defaultValue(DayOfWeek.SUNDAY).build()),

    WORK_SCHEDULE(TypedPropertyDefinition.builder()
            .type(ImmutableDayOfWeekSchedule.class).defaultValue(DayOfWeekSchedule.everyDay()).build()),

    SCHOOL_SCHEDULE(TypedPropertyDefinition.builder()
            .type(ImmutableDayOfWeekSchedule.class).defaultValue(DayOfWeekSchedule.everyDay()).build()),

    CONTACT_GROUP_SCHEDULE_WEIGHT(TypedPropertyDefinition.builder()
            .typeReference(new TypeReference<Map<ContactGroupType, Double>>() {
            })
            .defaultValue(new EnumMap<ContactGroupType, Double>(ContactGroupType.class)).build(), false),

    IMMUNITY_WANES_TIME_MEAN(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    IMMUNITY_WANES_TIME_SD(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    IMMUNITY_WANES_PROBABILITY(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    IMMUNITY_WANES_DECREASED_PROBABILITY_FROM_SEVERE_ILLNESS(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    IMMUNITY_WANES_INCREASED_PROBABILITY_FROM_ASYMPTOMATIC(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build()),

    IMMUNITY_WANES_RESIDUAL_IMMUNITY(TypedPropertyDefinition.builder()
            .type(Double.class).defaultValue(0.0).isMutable(false).build());

    private final TypedPropertyDefinition propertyDefinition;
    private final boolean isExternal;

    GlobalProperty(TypedPropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
        this.isExternal = true;
    }

    GlobalProperty(TypedPropertyDefinition propertyDefinition, boolean isExternal) {
        this.propertyDefinition = propertyDefinition;
        this.isExternal = isExternal;
    }

    private static Object getDefaultGeoLocator() {
        GeoLocator.Builder<Object> builder = GeoLocator.builder();
        builder.addLocation(0, 0, new Object());
        return builder.build();
    }

    @Override
    public TypedPropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

    @Override
    public boolean isExternalProperty() {
        return isExternal;
    }
}
