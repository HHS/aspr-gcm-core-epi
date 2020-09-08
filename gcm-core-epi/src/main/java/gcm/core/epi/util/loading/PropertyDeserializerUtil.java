package gcm.core.epi.util.loading;

import com.fasterxml.jackson.core.type.TypeReference;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.behavior.*;
import gcm.core.epi.plugin.vaccine.resourcebased.ResourceBasedVaccinePlugin;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import gcm.core.epi.propertytypes.FipsCodeValue;
import gcm.core.epi.util.property.DefinedProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PropertyDeserializerUtil {

    private static final Map<DefinedProperty, PropertyDeserializer> propertyDeserializers;

    static {
        propertyDeserializers = new HashMap<>();
        propertyDeserializers.put(GlobalProperty.CASE_HOSPITALIZATION_RATIO,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DELAY_MEAN,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DELAY_SD,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DURATION_MEAN,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DURATION_SD,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_DURATION_MEAN,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.CASE_FATALITY_RATIO,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_TO_DEATH_DELAY_MEAN,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        propertyDeserializers.put(GlobalProperty.HOSPITALIZATION_TO_DEATH_DELAY_SD,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        // Telework
        propertyDeserializers.put(
                TeleworkBehaviorPlugin.TeleworkGlobalAndRegionProperty.WORKPLACE_TELEWORK_CONTACT_SUBSTITUTION_WEIGHTS,
                new PropertyDeserializer(new TypeReference<FipsCodeValue<Map<ContactGroupType, Double>>>() {
                }));
        propertyDeserializers.put(
                TeleworkBehaviorPlugin.TeleworkGlobalProperty.TELEWORK_TRIGGER_OVERRIDES,
                new PropertyDeserializer(new TypeReference<List<TriggeredPropertyOverride>>() {
                }));
        // School Closure
        propertyDeserializers.put(
                SchoolClosureBehaviorPlugin.SchoolClosureGlobalProperty.SCHOOL_CLOSED_CONTACT_SUBSTITUTION_WEIGHTS,
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        propertyDeserializers.put(
                SchoolClosureBehaviorPlugin.SchoolClosureGlobalProperty.COHORTING_CONTACT_SUBSTITUTION_WEIGHTS,
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        propertyDeserializers.put(
                SchoolClosureBehaviorPlugin.SchoolClosureGlobalProperty.SUMMER_CONTACT_SUBSTITUTION_WEIGHTS,
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        // Combination Behavior
        propertyDeserializers.put(
                CombinationBehaviorPlugin.CombinationBehaviorGlobalProperty.INFECTION_RATE_REDUCTION,
                new PropertyDeserializer(new TypeReference<Map<AgeGroup, Double>>() {
                }));
        // Location Infection Reduction
        propertyDeserializers.put(
                LocationInfectionReductionPlugin.LocationInfectionReductionGlobalAndRegionProperty.LOCATION_INFECTION_REDUCTION,
                new PropertyDeserializer(new TypeReference<FipsCodeValue<Map<AgeGroup, Map<ContactGroupType, Double>>>>() {
                }));
        propertyDeserializers.put(
                LocationInfectionReductionPlugin.LocationInfectionReductionGlobalProperty.LOCATION_INFECTION_REDUCTION_TRIGGER_OVERRIDES,
                new PropertyDeserializer(new TypeReference<List<TriggeredPropertyOverride>>() {
                }));
        // Contact Tracing
        propertyDeserializers.put(
                ContactTracingBehaviorPlugin.ContactTracingGlobalProperty.FRACTION_CONTACTS_TRACED_AND_ISOLATED,
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        propertyDeserializers.put(
                ContactTracingBehaviorPlugin.ContactTracingGlobalProperty.CONTACT_TRACING_DELAY,
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        // Random Testing
        propertyDeserializers.put(
                RandomTestingBehaviorPlugin.RandomTestingGlobalProperty.TEST_ISOLATION_TRANSMISSION_REDUCTION,
                new PropertyDeserializer(new TypeReference<Map<ContactGroupType, Double>>() {
                }));
        // Resource-Based Vaccine
        propertyDeserializers.put(
                ResourceBasedVaccinePlugin.VaccineGlobalProperty.VACCINE_DELIVERIES,
                new PropertyDeserializer(new TypeReference<Map<Double, FipsCodeDouble>>() {
                }));
    }

    public static Optional<PropertyDeserializer> getPropertyDeserializer(DefinedProperty property) {
        PropertyDeserializer propertyDeserializer = propertyDeserializers.get(property);
        if (propertyDeserializer != null) {
            return Optional.of(propertyDeserializer);
        } else {
            return Optional.empty();
        }
    }

}
