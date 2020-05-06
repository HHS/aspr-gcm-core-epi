package gcm.core.epi.identifiers;

import gcm.components.Component;
import gcm.core.epi.components.*;
import gcm.scenario.GlobalComponentId;

public enum GlobalComponent implements GlobalComponentId {

    POPULATION_LOADER(PopulationLoader.class),

    CONTACT_MANAGER(ContactManager.class),

    HOSPITALIZATION_MANAGER(HospitalizationManager.class),

    TRIGGER_MANAGER(TriggerManager.class),

    SIMULATION_STOP_MANAGER(SimulationStopManager.class);

    private final Class<? extends Component> componentClass;

    GlobalComponent(Class<? extends Component> componentClass) {
        this.componentClass = componentClass;
    }

    public Class<? extends Component> getComponentClass() {
        return componentClass;
    }

}
