package gcm.core.epi.propertytypes;

import org.immutables.value.Value;

import java.util.Map;

public abstract class Weights<T> {

    public abstract Map<T, Double> values();

    @Value.Parameter
    @Value.Default
    public double defaultValue() {
        return 0.0;
    }

    public double getWeight(T t) {
        return values().getOrDefault(t, defaultValue());
    }

}
