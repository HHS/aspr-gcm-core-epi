package gcm.core.epi.identifiers;

import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.scenario.MapOption;
import gcm.scenario.PropertyDefinition;

public enum PersonProperty implements DefinedPersonProperty {

    AGE_GROUP_INDEX(PropertyDefinition.builder().setType(Integer.class).setDefaultValue(0).setMapOption(MapOption.ARRAY).build()),

    HAD_INFECTIOUS_CONTACT(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build()),

    IS_INFECTIOUS(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build()),

    WILL_BE_SYMPTOMATIC(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build()),

    IS_SYMPTOMATIC(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build()),

    EVER_HAD_SEVERE_ILLNESS(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build()),

    DID_NOT_RECEIVE_HOSPITAL_BED(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build()),

    IS_DEAD(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build()),

    ACTIVITY_LEVEL_CHANGED(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build()),

    IMMUNITY_WANED(PropertyDefinition.builder().setType(Boolean.class).setDefaultValue(false).build());

    private final PropertyDefinition propertyDefinition;

    PersonProperty(PropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    @Override
    public PropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

}
