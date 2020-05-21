package gcm.core.epi.util.loading;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * This class represents the input data associated with a given hospital
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableHospitalDataFileRecord.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface HospitalDataFileRecord {

    /**
     * @return The census tract in which the hospital is located
     */
    String regionId();

    /**
     * @return The number of beds in the hospital
     */
    int beds();

}
