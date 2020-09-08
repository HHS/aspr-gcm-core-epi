package gcm.core.epi.identifiers;

import gcm.core.epi.plugin.infection.ExponentialPeriodInfectionPlugin;
import gcm.core.epi.plugin.infection.InfectionPlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.HospitalData;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.*;
import gcm.core.epi.trigger.TriggerContainer;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.scenario.PropertyDefinition;
import gcm.scenario.RegionId;
import gcm.scenario.RegionPropertyId;
import gcm.util.geolocator.GeoLocator;
import org.apache.commons.math3.distribution.EnumeratedDistribution;

import java.time.DayOfWeek;
import java.util.*;


public enum GlobalProperty implements DefinedGlobalProperty {

    AGE_WEIGHTS_TEST(PropertyDefinition.builder()
            .setType(AgeWeights.class).setDefaultValue(ImmutableAgeWeights.builder().build()).build()),

    AVERAGE_TRANSMISSION_RATIO(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    POPULATION_DESCRIPTION(PropertyDefinition.builder()
            .setType(PopulationDescription.class).setPropertyValueMutability(false).build()),

    TRANSMISSION_STRUCTURE(PropertyDefinition.builder()
            .setType(TransmissionStructure.class).setPropertyValueMutability(false).build()),

    TRANSMISSION_RATIOS(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<AgeGroup, Double>()).build(), false),

    FRACTION_OF_GLOBAL_CONTACTS_IN_HOME_REGION(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.1).setPropertyValueMutability(false).build()),

    RADIATION_FLOW_GEOLOCATOR(PropertyDefinition.builder()
            .setType(GeoLocator.class).setDefaultValue(getDefaultGeoLocator()).build(), false),

    RADIATION_FLOW_MAX_RADIUS_KM(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(100.0).setPropertyValueMutability(false).build()),

    RADIATION_FLOW_TARGET_DISTRIBUTIONS(PropertyDefinition.builder()
            .setType(Map.class)
            .setDefaultValue(new HashMap<RegionId, EnumeratedDistribution<RegionId>>()).build(), false),

    INITIAL_INFECTIONS(PropertyDefinition.builder()
            .setType(FipsCodeDouble.class)
            .setDefaultValue(ImmutableFipsCodeDouble.builder().build())
            .setPropertyValueMutability(false).build()),

    FRACTION_SYMPTOMATIC(PropertyDefinition.builder()
            .setType(AgeWeights.class).setDefaultValue(ImmutableAgeWeights.builder().build()).setPropertyValueMutability(false).build()),

    ASYMPTOMATIC_INFECTIOUSNESS(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(1.0).setPropertyValueMutability(false).build()),

    MOST_RECENT_INFECTION_DATA(PropertyDefinition.builder()
            .setType(Optional.class).setDefaultValue(Optional.empty()).build(), false),

    HOSPITAL_DATA(PropertyDefinition.builder()
            .setType(List.class).setDefaultValue(new ArrayList<HospitalData>()).build(), false),

    HOSPITAL_DATA_FILE(PropertyDefinition.builder()
            .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

    HOSPITAL_BED_STAFF_RATIO(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    HOSPITAL_BED_OCCUPANCY(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    REGION_WORKER_FLOW_DATA_FILE(PropertyDefinition.builder()
            .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

    HOSPITAL_GEOLOCATOR(PropertyDefinition.builder()
            .setType(GeoLocator.class).setDefaultValue(getDefaultGeoLocator()).build(), false),

    CASE_HOSPITALIZATION_RATIO(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

    HOSPITALIZATION_DELAY_MEAN(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

    HOSPITALIZATION_DELAY_SD(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

    HOSPITALIZATION_DURATION_MEAN(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

    HOSPITALIZATION_DURATION_SD(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

    HOSPITALIZATION_MAX_RADIUS_KM(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    CASE_FATALITY_RATIO(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

    HOSPITALIZATION_TO_DEATH_DELAY_MEAN(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

    HOSPITALIZATION_TO_DEATH_DELAY_SD(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<>()).setPropertyValueMutability(false).build()),

    INFECTION_PLUGIN(PropertyDefinition.builder()
            .setType(InfectionPlugin.class).setDefaultValue(new ExponentialPeriodInfectionPlugin()).build(), false),

    BEHAVIOR_PLUGIN(PropertyDefinition.builder()
            .setType(Optional.class).setDefaultValue(Optional.empty()).build(), false),

    TRANSMISSION_PLUGIN(PropertyDefinition.builder()
            .setType(Optional.class).setDefaultValue(Optional.empty()).build(), false),

    VACCINE_PLUGIN(PropertyDefinition.builder()
            .setType(Optional.class).setDefaultValue(Optional.empty()).build(), false),

    TRIGGER_CONTAINER(PropertyDefinition.builder()
            .setType(TriggerContainer.class).setPropertyValueMutability(false).build(), false),

    TRIGGER_CALLBACKS(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new HashMap<String, Set<RegionPropertyId>>()).build(), false),

    MAX_SIMULATION_LENGTH(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(Double.POSITIVE_INFINITY).build()),

    SIMULATION_START_DAY(PropertyDefinition.builder()
            .setType(DayOfWeek.class).setDefaultValue(DayOfWeek.SUNDAY).build()),

    WORK_SCHEDULE(PropertyDefinition.builder()
            .setType(ImmutableDayOfWeekSchedule.class).setDefaultValue(DayOfWeekSchedule.everyDay()).build()),

    SCHOOL_SCHEDULE(PropertyDefinition.builder()
            .setType(ImmutableDayOfWeekSchedule.class).setDefaultValue(DayOfWeekSchedule.everyDay()).build()),

    CONTACT_GROUP_SCHEDULE_WEIGHT(PropertyDefinition.builder()
            .setType(Map.class).setDefaultValue(new EnumMap<ContactGroupType, Double>(ContactGroupType.class)).build(), false),

    IMMUNITY_WANES_TIME_MEAN(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    IMMUNITY_WANES_TIME_SD(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    IMMUNITY_WANES_PROBABILITY(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    IMMUNITY_WANES_DECREASED_PROBABILITY_FROM_SEVERE_ILLNESS(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    IMMUNITY_WANES_INCREASED_PROBABILITY_FROM_ASYMPTOMATIC(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

    IMMUNITY_WANES_RESIDUAL_IMMUNITY(PropertyDefinition.builder()
            .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build());

    private final PropertyDefinition propertyDefinition;
    private final boolean isExternal;

    GlobalProperty(PropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
        this.isExternal = true;
    }

    GlobalProperty(PropertyDefinition propertyDefinition, boolean isExternal) {
        this.propertyDefinition = propertyDefinition;
        this.isExternal = isExternal;
    }

    private static Object getDefaultGeoLocator() {
        GeoLocator.Builder<Object> builder = GeoLocator.builder();
        builder.addLocation(0, 0, new Object());
        return builder.build();
    }

    @Override
    public PropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

    @Override
    public boolean isExternalProperty() {
        return isExternal;
    }
}
