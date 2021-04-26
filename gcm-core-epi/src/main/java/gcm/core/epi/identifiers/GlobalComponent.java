package gcm.core.epi.identifiers;

import gcm.core.epi.components.*;
import nucleus.AgentContext;
import plugins.globals.support.GlobalComponentId;

import java.util.function.Consumer;
import java.util.function.Supplier;

public enum GlobalComponent implements GlobalComponentId {

    POPULATION_LOADER(() -> new PopulationLoader()::init),

    CONTACT_MANAGER(() -> new ContactManager()::init),

    HOSPITALIZATION_MANAGER(() -> new HospitalizationManager()::init),

    TRIGGER_MANAGER(() -> new TriggerManager()::init),

    SIMULATION_STOP_MANAGER(() -> new SimulationStopManager()::init);

    private final Supplier<Consumer<AgentContext>> componentInit;

    GlobalComponent(Supplier<Consumer<AgentContext>> componentInit) {
        this.componentInit = componentInit;
    }

    public Supplier<Consumer<AgentContext>> getComponentInit() {
        return componentInit;
    }

}
