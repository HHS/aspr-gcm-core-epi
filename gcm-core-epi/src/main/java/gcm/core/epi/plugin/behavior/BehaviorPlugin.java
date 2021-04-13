package gcm.core.epi.plugin.behavior;

import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.plugin.Plugin;
import plugins.gcm.agents.Environment;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.people.support.PersonId;

import java.util.Optional;

public abstract class BehaviorPlugin implements Plugin {

    /*
        Handle a person learning they may be infected
     */
    public void handleSuspectedInfected(Environment environment, PersonId personId) {
        // Do nothing by default
    }

    /*
        Handle a person becoming aware of an infection in their home
     */
    public void handleHomeInfection(Environment environment, PersonId personId) {
        // Do nothing by default
    }

    /*
        Gets the relative frequency of attempted transmission for the person taking into account behavior change
     */
    public double getRelativeActivityLevel(Environment environment, PersonId personId) {
        // Do not change this by default
        return 1.0;
    }

    /*
        Get a substitute contact group
     */
    public Optional<ContactGroupType> getSubstitutedContactGroup(Environment environment, PersonId personId,
                                                                 ContactGroupType selectedContactGroupType) {
        // Do not substitute contact group by default
        return Optional.of(selectedContactGroupType);
    }

    /*
        Get the (generally reduced) probability of infection for the specified person due to behavior change
     */
    public double getInfectionProbability(Environment environment, ContactGroupType contactSetting, PersonId personId) {
        // Do not change this by default
        return 1.0;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        Plugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalPropertyValue(GlobalProperty.BEHAVIOR_PLUGIN, Optional.of(this));
    }

}
