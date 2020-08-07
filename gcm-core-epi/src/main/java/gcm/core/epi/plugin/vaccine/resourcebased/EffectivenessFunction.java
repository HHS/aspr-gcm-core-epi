package gcm.core.epi.plugin.vaccine.resourcebased;

import org.immutables.value.Value;

@Value.Immutable
public abstract class EffectivenessFunction {

    @Value.Default
    double initialDelay() {
        return 0.0;
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
    double afterPeakDecay() {
        return 0.0;
    }

}
