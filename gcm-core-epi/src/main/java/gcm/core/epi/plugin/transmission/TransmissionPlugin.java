package gcm.core.epi.plugin.transmission;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.Plugin;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.PersonId;
import gcm.simulation.Environment;

import java.util.Optional;

public interface TransmissionPlugin extends Plugin {

    /*
    Get the (generally reduced) probability of infection for the specified person due to behavior change
 */
    default double getInfectionProbability(Environment environment, PersonId personId) {
        // Do not change this by default
        return 1.0;
    }

    @Override
    default void load(ExperimentBuilder experimentBuilder) {
        Plugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalPropertyValue(GlobalProperty.TRANSMISSION_PLUGIN, Optional.of(this));
    }

}
