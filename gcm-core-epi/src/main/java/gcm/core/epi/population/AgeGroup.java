package gcm.core.epi.population;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableAgeGroup.class)
public abstract class AgeGroup {

    public abstract String name();

    @Value.Default
    public int minAge() {
        return 0;
    }

    @Value.Default
    public int maxAge() {
        return Integer.MAX_VALUE;
    }

    public boolean contains(int age) {
        return minAge() <= age && maxAge() >= age;
    }

    @Override
    public String toString() {
        return name();
    }

}
