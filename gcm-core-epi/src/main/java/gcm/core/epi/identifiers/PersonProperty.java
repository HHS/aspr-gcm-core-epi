package gcm.core.epi.identifiers;

import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;

public enum PersonProperty implements DefinedPersonProperty {

    AGE_GROUP_INDEX(TypedPropertyDefinition.builder().type(Integer.class).defaultValue(0).build()),

    HAD_INFECTIOUS_CONTACT(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    IS_INFECTIOUS(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    WILL_BE_SYMPTOMATIC(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    IS_SYMPTOMATIC(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    EVER_HAD_SEVERE_ILLNESS(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    DID_NOT_RECEIVE_HOSPITAL_BED(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    IS_DEAD(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    ACTIVITY_LEVEL_CHANGED(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    IMMUNITY_WANED(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build()),

    IS_STAYING_HOME(TypedPropertyDefinition.builder().type(Boolean.class).defaultValue(false).build());

    private final TypedPropertyDefinition propertyDefinition;

    PersonProperty(TypedPropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    @Override
    public TypedPropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

}
