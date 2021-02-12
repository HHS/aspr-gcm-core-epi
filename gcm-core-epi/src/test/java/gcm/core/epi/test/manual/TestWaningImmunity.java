package gcm.core.epi.test.manual;

import gcm.core.epi.variants.ImmutableWaningImmunityFunction;
import gcm.core.epi.variants.WaningImmunityFunction;
import org.junit.jupiter.api.Test;

public class TestWaningImmunity {

    @Test
    void test() {
        WaningImmunityFunction waningImmunityFunction = ImmutableWaningImmunityFunction.builder()
                .waningMean(365.0).waningSD(131.4).build();
        System.out.println(waningImmunityFunction.getResidualImmunity(0.0));
        System.out.println(waningImmunityFunction.getResidualImmunity(10.0));
        System.out.println(waningImmunityFunction.getResidualImmunity(365.0));
    }

}
