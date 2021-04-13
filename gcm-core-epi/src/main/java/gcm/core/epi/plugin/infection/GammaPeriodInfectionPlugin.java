package gcm.core.epi.plugin.infection;

import gcm.core.epi.util.distributions.GammaHelper;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.DefinedPersonProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import plugins.gcm.agents.Environment;
import plugins.people.support.PersonId;

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

        LATENT_PERIOD(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build()),

        LATENT_PERIOD_COV(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build()),

        SYMPTOMATIC_INFECTIOUS_PERIOD(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build()),

        SYMPTOMATIC_INFECTIOUS_PERIOD_COV(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build()),

        FRACTION_LATENT_PERIOD_INFECTIOUS(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(1.0).isMutable(false).build()),

        TRANSMISSION_OVERDISPERSION(TypedPropertyDefinition.builder()
                .type(Double.class).defaultValue(0.0).isMutable(false).build());

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

    private enum PersonProperty implements DefinedPersonProperty {

        RELATIVE_INFECTIOUSNESS(TypedPropertyDefinition.builder()
                .type(Float.class).defaultValue(1.0f).build());

        private final TypedPropertyDefinition propertyDefinition;

        PersonProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }

    }

}
