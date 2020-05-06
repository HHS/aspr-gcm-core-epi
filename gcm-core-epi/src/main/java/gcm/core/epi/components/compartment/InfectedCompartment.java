package gcm.core.epi.components.compartment;

import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.identifiers.RandomId;
import gcm.core.epi.plugin.infection.DiseaseCourseData;
import gcm.core.epi.plugin.infection.InfectionPlugin;
import gcm.scenario.PersonId;
import gcm.simulation.Environment;
import gcm.simulation.Plan;

public class InfectedCompartment extends DiseaseCompartment {

    @Override
    public void init(final Environment environment) {

        // Register that we wish to observe people who arrive in this compartment
        environment.observeCompartmentPersonArrival(true, Compartment.INFECTED);

    }

    @Override
    public void observeCompartmentPersonArrival(final Environment environment, final PersonId personId) {

        // Get disease course data from infection module
        InfectionPlugin infectionPlugin = environment.getGlobalPropertyValue(GlobalProperty.INFECTION_PLUGIN);
        DiseaseCourseData diseaseCourseData = infectionPlugin.getDiseaseCourseData(environment, personId);

        // Schedule onset of infectiousness
        environment.addPlan(new InfectiousnessOnsetPlan(personId),
                environment.getTime() + diseaseCourseData.infectiousOnsetTime());

        // Determine if person should be symptomatic
        final boolean willBeSymptomatic = environment.getRandomGeneratorFromId(RandomId.INFECTED_COMPARTMENT).nextDouble() <=
                (double) environment.getGlobalPropertyValue(GlobalProperty.FRACTION_SYMPTOMATIC);
        if (willBeSymptomatic) {
            environment.setPersonPropertyValue(personId, PersonProperty.WILL_BE_SYMPTOMATIC, true);
            environment.addPlan(new SymptomOnsetPlan(personId),
                    environment.getTime() + diseaseCourseData.symptomOnsetTime());
        }
        // Implicitly by default environment.setPersonPropertyValue(personId, PersonProperty.WILL_BE_SYMPTOMATIC, false);

        // Schedule onset of recovery
        environment.addPlan(new CompartmentChangePlan(personId, Compartment.RECOVERED),
                environment.getTime() + diseaseCourseData.recoveryTime());

    }

    private void handleSymptomsOnRecovery(final Environment environment, PersonId personId) {
        // Remove symptomatic flag if set
        boolean isSymptomatic = environment.getPersonPropertyValue(personId, PersonProperty.IS_SYMPTOMATIC);
        if (isSymptomatic) {
            environment.setPersonPropertyValue(personId, PersonProperty.IS_SYMPTOMATIC, false);
        }
    }

    @Override
    public void executePlan(final Environment environment, Plan plan) {

        if (plan.getClass().equals(InfectiousnessOnsetPlan.class)) {
            /*
                Infectiousness onset
             */
            PersonId personId = ((InfectiousnessOnsetPlan) plan).personId;
            environment.setPersonPropertyValue(personId, PersonProperty.IS_INFECTIOUS, true);
        } else if (plan.getClass().equals(SymptomOnsetPlan.class)) {
            /*
                Symptom onset
             */
            PersonId personId = ((SymptomOnsetPlan) plan).personId;
            environment.setPersonPropertyValue(personId, PersonProperty.IS_SYMPTOMATIC, true);
        } else if (plan.getClass().equals(CompartmentChangePlan.class)) {
            /*
                Recovery
             */
            PersonId personId = ((CompartmentChangePlan) plan).personId;
            environment.setPersonPropertyValue(personId, PersonProperty.IS_INFECTIOUS, false);
            handleSymptomsOnRecovery(environment, personId);
            // Actually perform compartment change
            super.executePlan(environment, plan);
        }

    }

    private static class InfectiousnessOnsetPlan implements Plan {
        private final PersonId personId;

        private InfectiousnessOnsetPlan(PersonId personId) {
            this.personId = personId;
        }
    }

    private static class SymptomOnsetPlan implements Plan {
        private final PersonId personId;

        private SymptomOnsetPlan(PersonId personId) {
            this.personId = personId;
        }
    }

}
