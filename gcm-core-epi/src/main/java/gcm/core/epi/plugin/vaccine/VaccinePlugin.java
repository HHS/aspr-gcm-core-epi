package gcm.core.epi.plugin.vaccine;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.Plugin;
import gcm.scenario.ExperimentBuilder;
import gcm.scenario.GlobalComponentId;
import gcm.scenario.PersonId;
import gcm.scenario.RandomNumberGeneratorId;
import gcm.simulation.Environment;

import java.util.Optional;

public interface VaccinePlugin extends Plugin {

    /*
        Unique ID for the single global component added by all vaccine modules
     */
    GlobalComponentId VACCINE_MANAGER_IDENTIFIER = new GlobalComponentId() {
    };

    /*
        Get the reduction in probability that a vaccinated susceptible person will be infected by an exposure
     */
    double getVES(Environment environment, PersonId personId);

    /*
        Get the reduction in probability that a vaccinated infected person will transmit infection
     */
    double getVEI(Environment environment, PersonId personId);

    /*
        Get the reduction in probability that a vaccinated infected person will have a severe outcome
     */
    double getVEP(Environment environment, PersonId personId);

    /*
        Get the probability that vaccine fails to prevent transmission, taking into account VEi and VEs of the
            source and target respectively
     */
    default double getProbabilityVaccineFailsToPreventTransmission(Environment environment,
                                                                   PersonId sourcePersonId,
                                                                   PersonId targetPersonId) {
        double sourceVEI = getVEI(environment, sourcePersonId);
        double targetVES = getVES(environment, targetPersonId);
        return (1.0 - sourceVEI) * (1.0 - targetVES);
    }

    @Override
    default void load(ExperimentBuilder experimentBuilder) {
        Plugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalPropertyValue(GlobalProperty.VACCINE_PLUGIN, Optional.of(this));
    }

    /*

     */
    enum VaccineRandomId implements RandomNumberGeneratorId {
        ID
    }

}
