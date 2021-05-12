package gcm.core.epi.plugin.vaccine;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.variants.VariantId;
import gcm.core.epi.variants.VariantsDescription;
import plugins.gcm.agents.Environment;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.globals.support.GlobalComponentId;
import plugins.people.support.PersonId;
import plugins.stochastics.support.RandomNumberGeneratorId;

import java.util.Optional;

public interface VaccinePlugin extends Plugin {

    /*
        Unique ID for the single global component added by all vaccine plugins
     */
    GlobalComponentId VACCINE_MANAGER_IDENTIFIER = new GlobalComponentId() {
    };

    /*
        Get the reduction in probability that a vaccinated susceptible person will be infected by an exposure
     */
    double getVES(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the reduction in probability that a vaccinated infected person will transmit infection
     */
    double getVEI(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the reduction in probability that a vaccinated infected person will have a severe outcome
     */
    double getVEP(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the reduction in probability of hospitalization / death for a vaccinated infected person
     */
    double getVED(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the probability that vaccine fails to prevent transmission, taking into account VEi and VEs of the
            source and target respectively
     */
    default double getProbabilityVaccineFailsToPreventTransmission(Environment environment,
                                                                   PersonId sourcePersonId,
                                                                   PersonId targetPersonId) {
        VariantsDescription variantsDescription = environment.getGlobalPropertyValue(GlobalProperty.VARIANTS_DESCRIPTION);
        int sourceStrainId = environment.getPersonPropertyValue(sourcePersonId, PersonProperty.PRIOR_INFECTION_STRAIN_INDEX_1);
        VariantId sourceVariant = variantsDescription.variantIdList().get(sourceStrainId);
        double sourceVEI = getVEI(environment, sourcePersonId, sourceVariant);
        double targetVES = getVES(environment, targetPersonId, sourceVariant);
        return (1.0 - sourceVEI) * (1.0 - targetVES);
    }

    @Override
    default void load(ExperimentBuilder experimentBuilder) {
        Plugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalPropertyValue(GlobalProperty.VACCINE_PLUGIN, Optional.of(this));
    }

    /*
        The random generator ID used by all vaccine plugins
     */
    enum VaccineRandomId implements RandomNumberGeneratorId {
        ID,
        COVERAGE_ID
    }

}
