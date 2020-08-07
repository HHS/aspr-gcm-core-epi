package gcm.core.epi.plugin.vaccine.twodose;

public enum TwoDoseVaccineStatus {

    NOT_VACCINATED,

    // Received first dose but is not yet protected and has not received the second
    VACCINATED_ONE_DOSE_NOT_YET_PROTECTED,

    // Protected by first dose and has not received the second
    VACCINATED_ONE_DOSE_PROTECTED,

    // Has received two doses but is not protected by either
    VACCINATED_TWO_DOSES_NOT_YET_PROTECTED,

    // Protected by first dose but not second, though has received two doses
    VACCINATED_TWO_DOSES_PARTIALLY_PROTECTED,

    // Protected by both doses
    VACCINATED_TWO_DOSES_PROTECTED,

    // Vaccine protection has waned
    VACCINATED_NO_LONGER_PROTECTED

}
