package gcm.core.epi.population;

import gcm.scenario.PersonPropertyId;
import gcm.scenario.RegionId;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
public interface PersonData {

    RegionId regionId();

    Map<PersonPropertyId, Object> personPropertyValues();

}
