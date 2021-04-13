package gcm.core.epi.identifiers;

import gcm.core.epi.util.property.DefinedGroupProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import plugins.regions.support.RegionId;

public enum WorkplaceProperty implements DefinedGroupProperty {

    REGION_ID(TypedPropertyDefinition.builder().type(RegionId.class).defaultValue(StringRegionId.of("")).build());

    private final TypedPropertyDefinition propertyDefinition;

    WorkplaceProperty(TypedPropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    @Override
    public TypedPropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

}
