package gcm.core.epi.components;


import gcm.core.epi.identifiers.GlobalProperty;
import plugins.gcm.agents.Plan;
import plugins.gcm.agents.AbstractComponent;
import plugins.gcm.agents.Environment;

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
