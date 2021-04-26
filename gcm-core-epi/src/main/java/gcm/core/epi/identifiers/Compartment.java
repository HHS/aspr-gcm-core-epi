package gcm.core.epi.identifiers;


import gcm.core.epi.components.compartment.InfectedCompartment;
import gcm.core.epi.components.compartment.SusceptibleCompartment;

import nucleus.AgentContext;
import plugins.compartments.support.CompartmentId;

import java.util.function.Consumer;
import java.util.function.Supplier;

public enum Compartment implements CompartmentId {

    SUSCEPTIBLE(() -> new SusceptibleCompartment()::init),

    INFECTED(() -> new InfectedCompartment()::init);

    private final Supplier<Consumer<AgentContext>> componentInit;

    Compartment(Supplier<Consumer<AgentContext>> componentInit) {
        this.componentInit = componentInit;
    }

    public Supplier<Consumer<AgentContext>> getComponentInit() {
        return componentInit;
    }

}
