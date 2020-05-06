package gcm.core.epi.util.loading;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.behavior.*;
import gcm.core.epi.population.AgeGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PropertyDeserializerUtil {

    private static final Map<String, PropertyDeserializer> propertyDeserializers;

    static {
        propertyDeserializers = new HashMap<>();
        propertyDeserializers.put(GlobalProperty.CASE_HOSPITALIZATION_RATIO.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DELAY_MEAN.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DELAY_SD.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DURATION_MEAN.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DURATION_SD.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DURATION_MEAN.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.CASE_FATALITY_RATIO.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_TO_DEATH_DELAY_MEAN.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_TO_DEATH_DELAY_SD.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        // Telework
        propertyDeserializers.put(
                TeleworkBehaviorPlugin.TeleworkGlobalProperty.WORKPLACE_TELEWORK_CONTACT_SUBSTITUTION_WEIGHTS.toString(),
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        // School Closure
        propertyDeserializers.put(
                SchoolClosureBehaviorPlugin.SchoolClosureGlobalProperty.SCHOOL_CLOSED_CONTACT_SUBSTITUTION_WEIGHTS.toString(),
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        propertyDeserializers.put(
                SchoolClosureBehaviorPlugin.SchoolClosureGlobalProperty.SUMMER_CONTACT_SUBSTITUTION_WEIGHTS.toString(),
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        // Combination Behavior
        propertyDeserializers.put(
                CombinationBehaviorPlugin.CombinationBehaviorGlobalProperty.INFECTION_RATE_REDUCTION.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        // Location Infection Reduction
        propertyDeserializers.put(
                LocationInfectionReductionPlugin.LocationInfectionReductionGlobalAndRegionProperty.LOCATION_INFECTION_REDUCTION.toString(),
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Map<ContactGroupType, Double>>>() {
                }));
        propertyDeserializers.put(
                LocationInfectionReductionPlugin.LocationInfectionReductionGlobalProperty.LOCATION_INFECTION_REDUCTION_TRIGGER_OVERRIDES.toString(),
                new PropertyDeserializer(new TypeReference<List<TriggeredPropertyOverride>>() {
                }));
        // Contact Tracing
        propertyDeserializers.put(
                ContactTracingBehaviorPlugin.ContactTracingGlobalProperty.FRACTION_CONTACTS_TRACED_AND_ISOLATED.toString(),
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        propertyDeserializers.put(
                ContactTracingBehaviorPlugin.ContactTracingGlobalProperty.CONTACT_TRACING_DELAY.toString(),
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        // Random Testing
        propertyDeserializers.put(
                RandomTestingBehaviorPlugin.RandomTestingGlobalProperty.TEST_ISOLATION_TRANSMISSION_REDUCTION.toString(),
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
    }

    public static Optional<PropertyDeserializer> getPropertyDeserializer(String propertyName) {
        PropertyDeserializer propertyDeserializer = propertyDeserializers.get(propertyName);
        if (propertyDeserializer != null) {
            return Optional.of(propertyDeserializer);
        } else {
            return Optional.empty();
        }
    }

}
