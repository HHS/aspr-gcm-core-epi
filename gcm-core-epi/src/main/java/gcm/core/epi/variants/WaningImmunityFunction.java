package gcm.core.epi.variants;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.util.distributions.GammaHelper;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableWaningImmunityFunction.class)
public abstract class WaningImmunityFunction {

    @Value.Default
    double waningMean() {
        return Double.POSITIVE_INFINITY;
    }

    @Value.Default
    double waningSD() {
        return waningMean();
    }

    @Value.Derived
    GammaDistribution waningDistribution() {
        return new GammaDistribution(GammaHelper.getShapeFromMeanAndSD(waningMean(), waningSD()),
                GammaHelper.getScaleFromMeanAndSD(waningMean(), waningSD()));
    }

    public double getInfectionProbability(double time) {
        if (time < 0) {
            return 1.0;
        }
        if (waningMean() == Double.POSITIVE_INFINITY) {
            return 0.0;
        } else {
            return waningDistribution().cumulativeProbability(time);
        }
    }

}
