package gcm.core.epi.plugin.vaccine.onedose;

import plugins.gcm.agents.Environment;
import plugins.people.support.PersonId;
import plugins.personproperties.support.PersonPropertyId;

public class OneDoseVaccineHelper {

    /*
        Determines if the person in question is vaccine protected by the vaccine
            Uses the specified vaccine status ID
     */
    private static boolean isVaccineProtected(Environment environment, PersonId personId,
                                              PersonPropertyId vaccineStatusId) {
        final OneDoseVaccineStatus vaccineStatus =
                environment.getPersonPropertyValue(personId, vaccineStatusId);
        return vaccineStatus == OneDoseVaccineStatus.VACCINE_PROTECTED;
    }

    /*
        Computes VES for the person in the specified environment assuming the vaccine status
            is stored in the corresponding vaccineStatusId
     */
    public static double getVES(Environment environment, PersonId personId, PersonPropertyId vaccineStatusId,
                                OneDoseVaccineEfficacySpecification vaccineEfficacySpecification) {
        return isVaccineProtected(environment, personId, vaccineStatusId) ?
                vaccineEfficacySpecification.vES() : 0.0;
    }

    /*
        Computes VEI for the person in the specified environment assuming the vaccine status
            is stored in the corresponding vaccineStatusId
    */
    public static double getVEI(Environment environment, PersonId personId, PersonPropertyId vaccineStatusId,
                                OneDoseVaccineEfficacySpecification vaccineEfficacySpecification) {
        return isVaccineProtected(environment, personId, vaccineStatusId) ?
                vaccineEfficacySpecification.vEI() : 0.0;
    }

    /*
        Computes VEP for the person in the specified environment assuming the vaccine status
            is stored in the corresponding vaccineStatusId
    */
    public static double getVEP(Environment environment, PersonId personId, PersonPropertyId vaccineStatusId,
                                OneDoseVaccineEfficacySpecification vaccineEfficacySpecification) {
        return isVaccineProtected(environment, personId, vaccineStatusId) ?
                vaccineEfficacySpecification.vEP() : 0.0;
    }


}
