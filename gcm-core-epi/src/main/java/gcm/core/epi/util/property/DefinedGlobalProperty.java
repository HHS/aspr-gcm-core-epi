package gcm.core.epi.util.property;

import gcm.scenario.GlobalPropertyId;

public interface DefinedGlobalProperty extends DefinedProperty, GlobalPropertyId {

    boolean isExternalProperty();

}
