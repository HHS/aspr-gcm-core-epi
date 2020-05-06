package gcm.core.epi.population;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.scenario.PersonId;
import gcm.simulation.Environment;

public class Util {

    public static AgeGroup getAgeGroupForPerson(Environment environment, PersonId personId) {
        PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);
        Integer ageGroupIndex = environment.getPersonPropertyValue(personId, PersonProperty.AGE_GROUP_INDEX);
        return populationDescription.ageGroupPartition().getAgeGroupFromIndex(ageGroupIndex);
    }

}
