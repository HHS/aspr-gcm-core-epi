package gcm.core.epi.identifiers;

import gcm.core.epi.util.property.DefinedRegionProperty;
import gcm.scenario.PropertyDefinition;

public enum RegionProperty implements DefinedRegionProperty {

    LAT(PropertyDefinition.builder().setType(Double.class).setPropertyValueMutability(false).build()),

    LON(PropertyDefinition.builder().setType(Double.class).setPropertyValueMutability(false).build());

    private final PropertyDefinition propertyDefinition;

    RegionProperty(PropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    @Override
    public PropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

}
