package gcm.core.epi.plugin.transmission;

import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.scenario.PersonId;
import gcm.scenario.PropertyDefinition;
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

        TRANSMISSION_DECLINE_START(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),
        TRANSMISSION_DECLINE_DURATION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),
        TRANSMISSION_NADIR_DURATION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),
        TRANSMISSION_INCLINE_DURATION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build()),
        RELATIVE_TRANSMISSIBILITY_AT_NADIR(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;

        SeasonalTransmissionGlobalProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

    }

}
