package gcm.core.epi.propertytypes;

import gcm.core.epi.identifiers.ContactGroupType;
import org.immutables.value.Value;
import plugins.people.support.PersonId;

import java.util.Optional;

/*
    This class represents the data that describe an attempted infection event
        This is stored as a global property and used to support reporting
 */
@Value.Immutable
public interface InfectionData {

    /*
        The source of the infection
     */
    Optional<PersonId> sourcePersonId();

    /*
        The person infected by the source
     */
    Optional<PersonId> targetPersonId();

    /*
        The group in which the infection occurred
     */
    ContactGroupType transmissionSetting();

    /*
        Whether or not the transmission occurred
     */
    boolean transmissionOccurred();

}
