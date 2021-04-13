package gcm.core.epi.components.compartment;

import gcm.core.epi.identifiers.Compartment;
import nucleus.Plan;
import plugins.gcm.agents.AbstractComponent;
import plugins.gcm.agents.Environment;
import plugins.people.support.PersonId;

abstract class DiseaseCompartment extends AbstractComponent {

    @Override
    public void init(final Environment environment) { /* Do nothing */ }

    @Override
    public void executePlan(final Environment environment, final Plan plan) {
        final CompartmentChangePlan compartmentChangePlan = (CompartmentChangePlan) plan;
        environment.setPersonCompartment(compartmentChangePlan.personId,
                compartmentChangePlan.getDestinationCompartmentType());
    }

    static class CompartmentChangePlan implements Plan {
        final PersonId personId;
        final Compartment destinationCompartmentType;

        CompartmentChangePlan(final PersonId personId, final Compartment destinationCompartmentType) {
            this.personId = personId;
            this.destinationCompartmentType = destinationCompartmentType;
        }

        Compartment getDestinationCompartmentType() {
            return destinationCompartmentType;
        }

        @Override
        public String toString() {
            return "CompartmentChangePlan [personId=" +
                    personId +
                    ", destinationCompartmentType=" +
                    destinationCompartmentType +
                    "]";
        }

    }

}
