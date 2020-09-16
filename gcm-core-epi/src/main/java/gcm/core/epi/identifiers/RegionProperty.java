package gcm.core.epi.identifiers;

import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;

public enum RegionProperty implements DefinedRegionProperty {

    LAT(TypedPropertyDefinition.builder().type(Double.class).isMutable(false).build()),

    LON(TypedPropertyDefinition.builder().type(Double.class).isMutable(false).build());

    private final TypedPropertyDefinition propertyDefinition;

    RegionProperty(TypedPropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    @Override
    public TypedPropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

}
