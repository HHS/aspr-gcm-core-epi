package gcm.core.epi.plugin.vaccine.twodose;

import gcm.scenario.PersonId;
import gcm.scenario.PersonPropertyId;
import gcm.simulation.Environment;

public class TwoDoseVaccineHelper {

    /*
        Determines if the person in question is vaccine protected by the first dose
            Uses the specified vaccine status ID
     */
    private static boolean isVaccineDose1Protected(Environment environment, PersonId personId,
                                                   PersonPropertyId vaccineStatusId) {
        final TwoDoseVaccineStatus vaccineStatus =
                environment.getPersonPropertyValue(personId, vaccineStatusId);
        return vaccineStatus == TwoDoseVaccineStatus.VACCINATED_ONE_DOSE_PROTECTED ||
                vaccineStatus == TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PARTIALLY_PROTECTED;
    }


    /*
        Determines if the person in question is vaccine protected by the second dose assuming the vaccine status
            is stored in the corresponding vaccineStatusId
    */
    public static boolean isVaccineDose2Protected(Environment environment, PersonId personId,
                                                  PersonPropertyId vaccineStatusId) {
        return environment.getPersonPropertyValue(personId, vaccineStatusId) ==
                TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PROTECTED;
    }

    /*
        Computes VES for the person in the specified environment assuming the vaccine status
            is stored in the corresponding vaccineStatusId
     */
    public static double getVES(Environment environment, PersonId personId, PersonPropertyId vaccineStatusId,
                                TwoDoseVaccineEfficacySpecification vaccineEfficacySpecification) {
        return isVaccineDose2Protected(environment, personId, vaccineStatusId) ?
                vaccineEfficacySpecification.doseTwoVES() :
                isVaccineDose1Protected(environment, personId, vaccineStatusId) ?
                        vaccineEfficacySpecification.doseOneVES() : 0.0;
    }

    /*
        Computes VEI for the person in the specified environment assuming the vaccine status
            is stored in the corresponding vaccineStatusId
    */
    public static double getVEI(Environment environment, PersonId personId, PersonPropertyId vaccineStatusId,
                                TwoDoseVaccineEfficacySpecification vaccineEfficacySpecification) {
        return isVaccineDose2Protected(environment, personId, vaccineStatusId) ?
                vaccineEfficacySpecification.doseTwoVEI() :
                isVaccineDose1Protected(environment, personId, vaccineStatusId) ?
                        vaccineEfficacySpecification.doseOneVEI() : 0.0;
    }

    /*
        Computes VEP for the person in the specified environment assuming the vaccine status
            is stored in the corresponding vaccineStatusId
    */
    public static double getVEP(Environment environment, PersonId personId, PersonPropertyId vaccineStatusId,
                                TwoDoseVaccineEfficacySpecification vaccineEfficacySpecification) {
        return isVaccineDose2Protected(environment, personId, vaccineStatusId) ?
                vaccineEfficacySpecification.doseTwoVEP() :
                isVaccineDose1Protected(environment, personId, vaccineStatusId) ?
                        vaccineEfficacySpecification.doseOneVEP() : 0.0;
    }


}
