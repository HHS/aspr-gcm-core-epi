package gcm.core.epi.identifiers;

import gcm.components.Component;
import gcm.core.epi.components.compartment.InfectedCompartment;
import gcm.core.epi.components.compartment.RecoveredCompartment;
import gcm.core.epi.components.compartment.SusceptibleCompartment;
import gcm.scenario.CompartmentId;

public enum Compartment implements CompartmentId {

    SUSCEPTIBLE(SusceptibleCompartment.class),

    INFECTED(InfectedCompartment.class),

    RECOVERED(RecoveredCompartment.class);

    private final Class<? extends Component> componentClass;

    Compartment(Class<? extends Component> componentClass) {
        this.componentClass = componentClass;
    }

    public Class<? extends Component> getComponentClass() {
        return componentClass;
    }

}
