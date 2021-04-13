package gcm.core.epi.util.property;

import plugins.globals.support.GlobalPropertyId;

public interface DefinedGlobalProperty extends DefinedProperty, GlobalPropertyId {

    boolean isExternalProperty();

}
