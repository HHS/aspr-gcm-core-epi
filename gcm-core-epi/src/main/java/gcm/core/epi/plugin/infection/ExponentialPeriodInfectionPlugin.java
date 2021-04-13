package gcm.core.epi.plugin.infection;

import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import plugins.gcm.agents.Environment;
import plugins.people.support.PersonId;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class ExponentialPeriodInfectionPlugin implements InfectionPlugin {

    @Override
    public double getNextTransmissionTime(Environment environment, PersonId personId, double transmissionRatio) {
        double infectiousPeriod = environment.getGlobalPropertyValue(GlobalProperty.INFECTIOUS_PERIOD);
        return new ExponentialDistribution(environment.getRandomGenerator(),
                infectiousPeriod / transmissionRatio).sample();
    }

    @Override
    public DiseaseCourseData getDiseaseCourseData(Environment environment, PersonId personId) {
        double latentPeriod = environment.getGlobalPropertyValue(GlobalProperty.LATENT_PERIOD);
        double infectiousPeriod = environment.getGlobalPropertyValue(GlobalProperty.INFECTIOUS_PERIOD);
        final double infectiousOnsetTime = new ExponentialDistribution(environment.getRandomGenerator(),
                latentPeriod).sample();
        return ImmutableDiseaseCourseData.builder()
                .infectiousOnsetTime(infectiousOnsetTime)
                .recoveryTime(infectiousOnsetTime +
                        new ExponentialDistribution(environment.getRandomGenerator(), infectiousPeriod).sample())
                .symptomOnsetTime(infectiousOnsetTime)
                .build();
    }

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(GlobalProperty.class));
    }

    private enum GlobalProperty implements DefinedGlobalProperty {

        LATENT_PERIOD(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build()),

        INFECTIOUS_PERIOD(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build());

        private final TypedPropertyDefinition propertyDefinition;

        GlobalProperty(TypedPropertyDefinition propertyDefinition) {
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
