package gcm.core.epi.plugin.infection;

import gcm.core.epi.util.distributions.GammaHelper;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.scenario.PersonId;
import gcm.scenario.PropertyDefinition;
import gcm.simulation.Environment;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class GammaPeriodInfectionPlugin implements InfectionPlugin {

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet<>(EnumSet.allOf(GlobalProperty.class));
    }

    @Override
    public double getNextTransmissionTime(Environment environment, PersonId personId, double transmissionRatio) {
        double latentPeriod = environment.getGlobalPropertyValue(GlobalProperty.LATENT_PERIOD);
        double fractionLatentPeriodInfectious = environment.getGlobalPropertyValue(GlobalProperty.FRACTION_LATENT_PERIOD_INFECTIOUS);
        double symptomaticInfectiousPeriod = environment.getGlobalPropertyValue(GlobalProperty.SYMPTOMATIC_INFECTIOUS_PERIOD);
        double infectiousPeriod = fractionLatentPeriodInfectious * latentPeriod + symptomaticInfectiousPeriod;
        float relativeInfectiousness = environment.getPersonPropertyValue(personId, PersonProperty.RELATIVE_INFECTIOUSNESS);
        return new ExponentialDistribution(environment.getRandomGenerator(),
                infectiousPeriod / transmissionRatio / relativeInfectiousness).sample();
    }

    @Override
    public DiseaseCourseData getDiseaseCourseData(Environment environment, PersonId personId) {
        double latentPeriod = environment.getGlobalPropertyValue(GlobalProperty.LATENT_PERIOD);
        double latentPeriodCOV = environment.getGlobalPropertyValue(GlobalProperty.LATENT_PERIOD_COV);
        double symptomaticInfectiousPeriod = environment.getGlobalPropertyValue(GlobalProperty.SYMPTOMATIC_INFECTIOUS_PERIOD);
        double symptomaticInfectiousPeriodCOV = environment.getGlobalPropertyValue(GlobalProperty.SYMPTOMATIC_INFECTIOUS_PERIOD_COV);
        double fractionLatentPeriodInfectious = environment.getGlobalPropertyValue(GlobalProperty.FRACTION_LATENT_PERIOD_INFECTIOUS);
        // Subdivide gamma-distributed latent period into pre-infectious and pre-symptomatic infectious periods
        RealDistribution preInfectiousPeriodDistribution = new GammaDistribution(environment.getRandomGenerator(),
                GammaHelper.getShapeFromCOV(latentPeriodCOV) * (1 - fractionLatentPeriodInfectious),
                GammaHelper.getScaleFromMeanAndCOV(latentPeriod, latentPeriodCOV));
        double preInfectiousPeriod = preInfectiousPeriodDistribution.sample();

        RealDistribution preSymptomaticInfectiousPeriodDistribution = new GammaDistribution(environment.getRandomGenerator(),
                GammaHelper.getShapeFromCOV(latentPeriodCOV) * fractionLatentPeriodInfectious,
                GammaHelper.getScaleFromMeanAndCOV(latentPeriod, latentPeriodCOV));
        double preSymptomaticInfectiousPeriod = preSymptomaticInfectiousPeriodDistribution.sample();

        RealDistribution symptomaticInfectiousPeriodDistribution = new GammaDistribution(environment.getRandomGenerator(),
                GammaHelper.getShapeFromCOV(symptomaticInfectiousPeriodCOV),
                GammaHelper.getScaleFromMeanAndCOV(symptomaticInfectiousPeriod, symptomaticInfectiousPeriodCOV));
        double recoveryPeriod = symptomaticInfectiousPeriodDistribution.sample();

        // Handle infectiousness overdispersion
        double infectiousnessOverdispersion = environment.getGlobalPropertyValue(GlobalProperty.TRANSMISSION_OVERDISPERSION);
        if (infectiousnessOverdispersion > 0) {
            // Choose this to have mean 1 and cov == overdispersion
            RealDistribution infectiousnessDistribution = new GammaDistribution(environment.getRandomGenerator(),
                    GammaHelper.getShapeFromCOV(infectiousnessOverdispersion),
                    GammaHelper.getScaleFromMeanAndCOV(1, infectiousnessOverdispersion));
            environment.setPersonPropertyValue(personId, PersonProperty.RELATIVE_INFECTIOUSNESS,
                    (float) infectiousnessDistribution.sample());
        }

        return ImmutableDiseaseCourseData.builder()
                .infectiousOnsetTime(preInfectiousPeriod)
                .symptomOnsetTime(preInfectiousPeriod + preSymptomaticInfectiousPeriod)
                .recoveryTime(preInfectiousPeriod + preSymptomaticInfectiousPeriod + recoveryPeriod)
                .build();
    }

    @Override
    public Set<DefinedPersonProperty> getPersonProperties() {
        return new HashSet<>(EnumSet.allOf(PersonProperty.class));
    }

    private enum GlobalProperty implements DefinedGlobalProperty {

        LATENT_PERIOD(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(1.0).setPropertyValueMutability(false).build()),

        LATENT_PERIOD_COV(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(1.0).setPropertyValueMutability(false).build()),

        SYMPTOMATIC_INFECTIOUS_PERIOD(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(1.0).setPropertyValueMutability(false).build()),

        SYMPTOMATIC_INFECTIOUS_PERIOD_COV(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(1.0).setPropertyValueMutability(false).build()),

        FRACTION_LATENT_PERIOD_INFECTIOUS(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(1.0).setPropertyValueMutability(false).build()),

        TRANSMISSION_OVERDISPERSION(PropertyDefinition.builder()
                .setType(Double.class).setDefaultValue(0.0).setPropertyValueMutability(false).build());

        private final PropertyDefinition propertyDefinition;

        GlobalProperty(PropertyDefinition propertyDefinition) {
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

    private enum PersonProperty implements DefinedPersonProperty {

        RELATIVE_INFECTIOUSNESS(PropertyDefinition.builder()
                .setType(Float.class).setDefaultValue(1.0f).build());

        private final PropertyDefinition propertyDefinition;

        PersonProperty(PropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public PropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

}
