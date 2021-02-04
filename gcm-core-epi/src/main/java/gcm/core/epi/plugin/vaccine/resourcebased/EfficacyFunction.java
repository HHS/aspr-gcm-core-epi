package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableEfficacyFunction.class)
public abstract class EfficacyFunction {

    @Value.Default
    double initialDelay() {
        return 0.0;
    }

    @Value.Default
    double earlyPeakTime() {
        return peakTime();
    }

    @Value.Default
    double earlyPeakDuration() {
        return 0.0;
    }

    @Value.Default
    double earlyPeakHeight() {
        return 1.0;
    }

    @Value.Default
    double peakTime() {
        return 0.0;
    }

    @Value.Default
    double peakDuration() {
        return Double.POSITIVE_INFINITY;
    }

    @Value.Default
    double afterPeakHalfLife() {
        return Double.POSITIVE_INFINITY;
    }

    @Value.Check
    protected void check() {
        if (earlyPeakTime() + earlyPeakDuration() > peakTime()) {
            throw new IllegalStateException("Peak time must occur after early peak period (time + duration)");
        }
        // TODO: More validation
    }

    public double getValue(double time) {
        if (time < initialDelay()) {
            return 0.0;
        } else if (time < earlyPeakTime()) {
            return earlyPeakHeight() * (time - initialDelay()) / (earlyPeakTime() - initialDelay());
        } else if (time < earlyPeakTime() + earlyPeakDuration()) {
            return earlyPeakHeight();
        } else if (time < peakTime()) {
            return earlyPeakHeight() + (1 - earlyPeakHeight()) *
                    (time - earlyPeakTime() - earlyPeakDuration()) / (peakTime() - earlyPeakTime() - earlyPeakDuration());
        } else if (time < peakTime() + peakDuration()) {
            return 1.0;
        } else {
            double decayRate = Math.log(2) / afterPeakHalfLife();
            return Math.exp(-decayRate * (time - peakTime() - peakDuration()));
        }
    }

}
