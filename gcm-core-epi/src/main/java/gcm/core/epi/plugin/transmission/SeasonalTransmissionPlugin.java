package gcm.core.epi.plugin.transmission;

import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.scenario.PersonId;
import gcm.simulation.Environment;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class SeasonalTransmissionPlugin implements TransmissionPlugin {

    @Override
    public double getInfectionProbability(Environment environment, PersonId personId) {
        double transmissionDeclineStart = environment.getGlobalPropertyValue(
                SeasonalTransmissionGlobalProperty.TRANSMISSION_DECLINE_START);
        double transmissionDeclineDuration = environment.getGlobalPropertyValue(
                SeasonalTransmissionGlobalProperty.TRANSMISSION_DECLINE_DURATION);
        double transmissionNadirDuration = environment.getGlobalPropertyValue(
                SeasonalTransmissionGlobalProperty.TRANSMISSION_NADIR_DURATION);
        double transmissionInclineDuration = environment.getGlobalPropertyValue(
                SeasonalTransmissionGlobalProperty.TRANSMISSION_INCLINE_DURATION);
        double relativeTransmissibilityAtNadir = environment.getGlobalPropertyValue(
                SeasonalTransmissionGlobalProperty.RELATIVE_TRANSMISSIBILITY_AT_NADIR);

        // Cheat and adjust time to think that we're back in first year so things happen yearly
        double time = environment.getTime() % 365.0;
        if (transmissionDeclineStart + transmissionDeclineDuration + transmissionNadirDuration + +transmissionInclineDuration > 365.0)
            throw new IllegalArgumentException("Seasonality duration cannot be longer than one year. Use negative start time if required.");

        if (time <= transmissionDeclineStart |
                time > transmissionDeclineStart + transmissionDeclineDuration +
                        transmissionNadirDuration + transmissionInclineDuration) {
            return 1.0;
        } else if (time <= transmissionDeclineStart + transmissionDeclineDuration) {
            // Linearly interpolate
            double fraction = (time - transmissionDeclineStart) / transmissionDeclineDuration;
            return 1 - fraction * (1 - relativeTransmissibilityAtNadir);
        } else if (time <= transmissionDeclineStart + transmissionDeclineDuration + transmissionNadirDuration) {
            return relativeTransmissibilityAtNadir;
        } else {
            // time <= transmissionDeclineStart + transmissionDeclineDuration + transmissionNadirDuration + transmissionInclineDuration
            double fraction = (time - transmissionDeclineStart - transmissionDeclineDuration - transmissionNadirDuration) /
                    transmissionInclineDuration;
            return relativeTransmissibilityAtNadir + fraction * (1 - relativeTransmissibilityAtNadir);
        }
    }

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(SeasonalTransmissionGlobalProperty.class));
    }

    public enum SeasonalTransmissionGlobalProperty implements DefinedGlobalProperty {

        TRANSMISSION_DECLINE_START(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),
        TRANSMISSION_DECLINE_DURATION(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),
        TRANSMISSION_NADIR_DURATION(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),
        TRANSMISSION_INCLINE_DURATION(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build()),
        RELATIVE_TRANSMISSIBILITY_AT_NADIR(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build());

        private final TypedPropertyDefinition propertyDefinition;

        SeasonalTransmissionGlobalProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

    }

}
