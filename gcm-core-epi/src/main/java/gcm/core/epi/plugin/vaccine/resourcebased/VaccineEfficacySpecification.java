package gcm.core.epi.plugin.vaccine.resourcebased;

import org.immutables.value.Value;

@Value.Immutable
public abstract class VaccineEfficacySpecification {

    EffectivenessFunction effectivenessFunction() {
        return ImmutableEffectivenessFunction.builder().build();
    }

    double vES() {
        return 0;
    }

    double vEI() {
        return 0;
    }

    double vEP() {
        return 0;
    }

}
