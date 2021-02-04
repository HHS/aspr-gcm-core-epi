package gcm.core.epi.test.manual.vaccine;

import gcm.core.epi.plugin.vaccine.resourcebased.EfficacyFunction;
import gcm.core.epi.plugin.vaccine.resourcebased.ImmutableEfficacyFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEfficacyFunction {

    @Test
    public void test() {
        EfficacyFunction efficacyFunction = ImmutableEfficacyFunction.builder()
                .initialDelay(24.0)
                .peakTime(31.0)
                .peakDuration(30.0)
                .afterPeakHalfLife(180)
                .build();

        assertEquals(efficacyFunction.getValue(0.0), 0.0);
        assertEquals(efficacyFunction.getValue(14.0), 0.0);
        assertEquals(efficacyFunction.getValue(24.0), 0.0);
        assertEquals(efficacyFunction.getValue((24.0 + 31.0)/2), 0.5);
        assertEquals(efficacyFunction.getValue(31.0), 1.0);
        assertEquals(efficacyFunction.getValue(61.0), 1.0);
        assertTrue(efficacyFunction.getValue(62.0) < 1.0);

        efficacyFunction = ImmutableEfficacyFunction.builder()
                .initialDelay(7.0)
                .earlyPeakTime(14.0)
                .earlyPeakDuration(14.0)
                .earlyPeakHeight(0.5)
                .peakTime(35.0)
                .peakDuration(30.0)
                .afterPeakHalfLife(180)
                .build();

        assertEquals(efficacyFunction.getValue(0.0), 0.0);
        assertEquals(efficacyFunction.getValue(7.0), 0.0);
        assertEquals(efficacyFunction.getValue(21.0/2.0), 0.25);
        assertEquals(efficacyFunction.getValue(14.0), 0.5);
        assertEquals(efficacyFunction.getValue(28.0), 0.5);
        assertEquals(efficacyFunction.getValue((28.0 + 35.0)/2), 0.75);
        assertEquals(efficacyFunction.getValue(35.0), 1.0);
        assertEquals(efficacyFunction.getValue(65.0), 1.0);
        assertTrue(efficacyFunction.getValue(66.0) < 1.0);

    }

}
