package gcm.core.epi.plugin.transmission;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.Plugin;
import plugins.gcm.agents.Environment;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.people.support.PersonId;

import java.util.Optional;

public interface TransmissionPlugin extends Plugin {

    /*
        Get the (generally reduced) probability of infection for the specified person
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

    /*
        Get the relative transmissibility for the specified person
     */
    default double getRelativeTransmissibility(Environment environment, PersonId personId) {
        // Do not change this by default
        return 1.0;
    }

}
