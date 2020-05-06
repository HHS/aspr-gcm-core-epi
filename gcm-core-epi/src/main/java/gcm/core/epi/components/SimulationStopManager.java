package gcm.core.epi.components;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

public class SimulationStopManager extends AbstractComponent {

    @Override
    public void init(Environment environment) {
        double maxSimulationLength = environment.getGlobalPropertyValue(GlobalProperty.MAX_SIMULATION_LENGTH);

        if (maxSimulationLength < Double.POSITIVE_INFINITY) {
            environment.addPlan(new SimulationStopPlan(), maxSimulationLength);
        }
    }

    @Override
    public void executePlan(Environment environment, Plan plan) {
        environment.halt();
    }

    private static final class SimulationStopPlan implements Plan {
    }

}
