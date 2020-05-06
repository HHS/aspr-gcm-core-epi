package gcm.core.epi.identifiers;

import gcm.core.epi.util.property.DefinedGroupProperty;
import gcm.scenario.PropertyDefinition;
import gcm.scenario.RegionId;

public enum WorkplaceProperty implements DefinedGroupProperty {

    REGION_ID(PropertyDefinition.builder().setType(RegionId.class).setDefaultValue(StringRegionId.of("")).build());

    private final PropertyDefinition propertyDefinition;

    WorkplaceProperty(PropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    @Override
    public PropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }

}
