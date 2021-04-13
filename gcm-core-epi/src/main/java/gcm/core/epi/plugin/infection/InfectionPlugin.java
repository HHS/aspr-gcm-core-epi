package gcm.core.epi.plugin.infection;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.Plugin;
import plugins.gcm.agents.Environment;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.people.support.PersonId;

public interface InfectionPlugin extends Plugin {

    /*
        Calculates the next transmission time for the person assuming a given overall transmission ratio
     */
    double getNextTransmissionTime(Environment environment, PersonId personId, double transmissionRatio);

    /*
        Get disease course data
     */
    DiseaseCourseData getDiseaseCourseData(Environment environment, PersonId personId);

    @Override
    default void load(ExperimentBuilder experimentBuilder) {
        Plugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalPropertyValue(GlobalProperty.INFECTION_PLUGIN, this);
    }

}
