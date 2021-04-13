package gcm.core.epi.identifiers;


import gcm.core.epi.components.compartment.InfectedCompartment;
import gcm.core.epi.components.compartment.SusceptibleCompartment;

import plugins.compartments.support.CompartmentId;
import plugins.components.agents.Component;

public enum Compartment implements CompartmentId {

    SUSCEPTIBLE(SusceptibleCompartment.class),

    INFECTED(InfectedCompartment.class);

    private final Class<? extends Component> componentClass;

    Compartment(Class<? extends Component> componentClass) {
        this.componentClass = componentClass;
    }

    public Class<? extends Component> getComponentClass() {
        return componentClass;
    }

}
