package gcm.core.epi.components.compartment;

import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.RandomId;
import gcm.core.epi.util.distributions.GammaHelper;
import gcm.scenario.PersonId;
import gcm.simulation.Environment;
import gcm.simulation.Plan;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

public class RecoveredCompartment extends DiseaseCompartment {

    @Override
    public void init(final Environment environment) {
        // Only bother if immunity does wane
        if ((double) environment.getGlobalPropertyValue(GlobalProperty.IMMUNITY_WANES_PROBABILITY) != 0) {
            // Register that we wish to observe people who arrive in this compartment
            environment.observeCompartmentPersonArrival(true, Compartment.RECOVERED);
        }
    }

    @Override
    public void observeCompartmentPersonArrival(final Environment environment, final PersonId personId) {
        // First off, determine if this person will have waning immunity
        double baseWaningProbability = environment.getGlobalPropertyValue(GlobalProperty.IMMUNITY_WANES_PROBABILITY);
        double waningProbability = baseWaningProbability;
        double decreasedProbabilityDueToSevereIllness = environment.getGlobalPropertyValue(GlobalProperty.IMMUNITY_WANES_DECREASED_PROBABILITY_FROM_SEVERE_ILLNESS);
        double increasedProbabilityDueToAsymptomatic = environment.getGlobalPropertyTime(GlobalProperty.IMMUNITY_WANES_INCREASED_PROBABILITY_FROM_ASYMPTOMATIC);

        // Did they have severe illness?
        boolean everHadSevereIllness = environment.getPersonPropertyValue(personId, PersonProperty.EVER_HAD_SEVERE_ILLNESS);
        if (everHadSevereIllness) {
            waningProbability = baseWaningProbability * (1.0 - decreasedProbabilityDueToSevereIllness);
        }

        // Were they symptomatic
        boolean willBeSymptomatic = environment.getPersonPropertyValue(personId, PersonProperty.WILL_BE_SYMPTOMATIC);
        if (willBeSymptomatic) {
            waningProbability = waningProbability * (1.0 + increasedProbabilityDueToAsymptomatic);
        }

        // Will they wane?
        if (environment.getRandomGeneratorFromId(RandomId.RECOVERED_COMPARTMENT).nextDouble() <= waningProbability) {

            // When will they wane?
            double waningTimeMean = environment.getGlobalPropertyValue(GlobalProperty.IMMUNITY_WANES_TIME_MEAN);
            double waningTimeSD = environment.getGlobalPropertyValue(GlobalProperty.IMMUNITY_WANES_TIME_SD);

            double waningDelay;
            if (waningTimeMean == 0.0 | waningTimeSD == 0.0) {
                waningDelay = waningTimeMean;
            } else {
                RealDistribution waningImmunityDistribution = new GammaDistribution(
                        environment.getRandomGeneratorFromId(RandomId.RECOVERED_COMPARTMENT),
                        GammaHelper.getShapeFromMeanAndSD(waningTimeMean, waningTimeSD),
                        GammaHelper.getScaleFromMeanAndSD(waningTimeMean, waningTimeSD));

                waningDelay = waningImmunityDistribution.sample();
            }

            environment.addPlan(new WaningImmunityPlan(personId), environment.getTime() + waningDelay);
        }
    }

    // Move the person back to SUSCEPTIBLE
    @Override
    public void executePlan(Environment environment, Plan plan) {
        PersonId personId = ((WaningImmunityPlan) plan).personId;

        environment.setPersonPropertyValue(personId, PersonProperty.IMMUNITY_WANED, true);
        environment.setPersonCompartment(personId, Compartment.SUSCEPTIBLE);
    }

    private static class WaningImmunityPlan implements Plan {
        final PersonId personId;

        WaningImmunityPlan(PersonId personId) {
            this.personId = personId;
        }
    }
}