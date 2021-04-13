package gcm.core.epi.population;

import org.immutables.value.Value;
import plugins.personproperties.support.PersonPropertyId;
import plugins.regions.support.RegionId;

import java.util.Map;

@Value.Immutable
public interface PersonData {

    RegionId regionId();

    Map<PersonPropertyId, Object> personPropertyValues();

}
