package gcm.core.epi.plugin.infection;

import org.immutables.value.Value;

@Value.Immutable
public interface DiseaseCourseData {

    /*
        Gets the onset time of infectiousness relative to the time of exposure
     */
    double infectiousOnsetTime();

    /*
        Gets the time of recovery relative to the time of exposure
     */
    double recoveryTime();

    /*
        Gets the time of symptom onset relative to the time of exposure
     */
    double symptomOnsetTime();

}
