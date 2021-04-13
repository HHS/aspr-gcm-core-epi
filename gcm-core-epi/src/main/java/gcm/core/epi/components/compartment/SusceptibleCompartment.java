package gcm.core.epi.components.compartment;

import gcm.core.epi.identifiers.Compartment;
import gcm.core.epi.identifiers.PersonProperty;
import plugins.gcm.agents.Environment;
import plugins.people.support.PersonId;
import plugins.personproperties.support.PersonPropertyId;

public class SusceptibleCompartment extends DiseaseCompartment {

    @Override
    public void init(final Environment environment) {
        // Register that we wish to observe people who have infectious contacts
        environment.observeCompartmentalPersonPropertyChange(true, Compartment.SUSCEPTIBLE,
                PersonProperty.HAD_INFECTIOUS_CONTACT);
    }

    @Override
    public void observePersonPropertyChange(final Environment environment, final PersonId personId,
                                            final PersonPropertyId propertyId) {
        // Safe cast because we know this will only be called by the simulation for this type
        PersonProperty personProperty = (PersonProperty) propertyId;

        // This should only be called when a person has an infectious contact
        if (personProperty != PersonProperty.HAD_INFECTIOUS_CONTACT) {
            throw new RuntimeException("SusceptibleCompartment Error: Person with ID: " + personId +
                    " expected to have had 'HAD_INFECTIOUS_CONTACT' property changed but instead " +
                    personProperty + " was changed.");
        } else {
            // Move this person to INFECTED
            environment.setPersonCompartment(personId, Compartment.INFECTED);
        }
    }


}