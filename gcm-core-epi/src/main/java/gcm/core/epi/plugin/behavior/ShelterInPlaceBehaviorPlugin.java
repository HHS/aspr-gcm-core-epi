package gcm.core.epi.plugin.behavior;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.trigger.TriggerCallback;
import gcm.core.epi.trigger.TriggerUtils;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.*;
import gcm.simulation.Environment;

import java.util.*;

public class ShelterInPlaceBehaviorPlugin extends BehaviorPlugin {

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(ShelterInPlaceGlobalProperty.class));
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(ShelterInPlaceRandomId.ID);
        return randomIds;
    }

    @Override
    public Set<DefinedRegionProperty> getRegionProperties() {
        return new HashSet<>(EnumSet.allOf(ShelterInPlaceRegionProperty.class));
    }

    @Override
    public Map<String, Set<TriggerCallback>> getTriggerCallbacks(Environment environment) {
        Map<String, Set<TriggerCallback>> triggerCallbacks = new HashMap<>();
        String triggerId = environment.getGlobalPropertyValue(ShelterInPlaceGlobalProperty.SHELTER_IN_PLACE_START);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, ShelterInPlaceRegionProperty.SHELTER_IN_PLACE_TRIGGER_START);
        triggerId = environment.getGlobalPropertyValue(ShelterInPlaceGlobalProperty.SHELTER_IN_PLACE_END);
        TriggerUtils.addBooleanCallback(triggerCallbacks, triggerId, ShelterInPlaceRegionProperty.SHELTER_IN_PLACE_TRIGGER_END);
        return triggerCallbacks;
    }

    @Override
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId, ContactGroupType selectedContactGroupType) {
        RegionId regionId = environment.getPersonRegion(personId);
        boolean triggerIsInEffect = TriggerUtils.checkIfTriggerIsInEffect(environment, regionId,
                ShelterInPlaceRegionProperty.SHELTER_IN_PLACE_TRIGGER_START,
                ShelterInPlaceRegionProperty.SHELTER_IN_PLACE_TRIGGER_END);
        if (selectedContactGroupType != ContactGroupType.HOME && triggerIsInEffect) {
            double communityContactReduction = environment.getGlobalPropertyValue(ShelterInPlaceGlobalProperty.COMMUNITY_CONTACT_REDUCTION);
            if (environment.getRandomGeneratorFromId(ShelterInPlaceRandomId.ID).nextDouble() < communityContactReduction) {
                return Optional.of(ContactGroupType.HOME);
            } else {
                return Optional.of(ContactGroupType.GLOBAL);
            }
        } else {
            return Optional.of(selectedContactGroupType);
        }
    }

    public enum ShelterInPlaceGlobalProperty implements DefinedGlobalProperty {

        COMMUNITY_CONTACT_REDUCTION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),

        SHELTER_IN_PLACE_START(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build()),

        SHELTER_IN_PLACE_END(PropertyDefinition.builder()
                .setType(String.class).setDefaultValue("").setPropertyValueMutability(false).build());


        private final PropertyDefinition propertyDefinition;

        ShelterInPlaceGlobalProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

    }

    private enum ShelterInPlaceRandomId implements RandomNumberGeneratorId {
        ID
    }

    public enum ShelterInPlaceRegionProperty implements DefinedRegionProperty {

        SHELTER_IN_PLACE_TRIGGER_START(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build()),

        SHELTER_IN_PLACE_TRIGGER_END(PropertyDefinition.builder()
                .setType(Boolean.class).setDefaultValue(false)
                .setTimeTrackingPolicy(TimeTrackingPolicy.TRACK_TIME).build());

        private final PropertyDefinition propertyDefinition;

        ShelterInPlaceRegionProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }
}
