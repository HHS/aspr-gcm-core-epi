package gcm.core.epi.test.manual;

import gcm.core.epi.plugin.vaccine.resourcebased.EffectivenessFunction;
import gcm.core.epi.plugin.vaccine.resourcebased.ImmutableEffectivenessFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEffectivenessFunction {

    @Test
    public void test() {
        EffectivenessFunction effectivenessFunction = ImmutableEffectivenessFunction.builder()
                .initialDelay(24.0)
                .peakTime(31.0)
                .peakDuration(30.0)
                .afterPeakHalfLife(180)
                .build();

        assertEquals(effectivenessFunction.getValue(0.0), 0.0);
        assertEquals(effectivenessFunction.getValue(14.0), 0.0);
        assertEquals(effectivenessFunction.getValue(24.0), 0.0);
        assertEquals(effectivenessFunction.getValue((24.0 + 31.0)/2), 0.5);
        assertEquals(effectivenessFunction.getValue(31.0), 1.0);
        assertEquals(effectivenessFunction.getValue(61.0), 1.0);
        assertTrue(effectivenessFunction.getValue(62.0) < 1.0);

        effectivenessFunction = ImmutableEffectivenessFunction.builder()
                .initialDelay(7.0)
                .earlyPeakTime(14.0)
                .earlyPeakDuration(14.0)
                .earlyPeakHeight(0.5)
                .peakTime(35.0)
                .peakDuration(30.0)
                .afterPeakHalfLife(180)
                .build();

        assertEquals(effectivenessFunction.getValue(0.0), 0.0);
        assertEquals(effectivenessFunction.getValue(7.0), 0.0);
        assertEquals(effectivenessFunction.getValue(21.0/2.0), 0.25);
        assertEquals(effectivenessFunction.getValue(14.0), 0.5);
        assertEquals(effectivenessFunction.getValue(28.0), 0.5);
        assertEquals(effectivenessFunction.getValue((28.0 + 35.0)/2), 0.75);
        assertEquals(effectivenessFunction.getValue(35.0), 1.0);
        assertEquals(effectivenessFunction.getValue(65.0), 1.0);
        assertTrue(effectivenessFunction.getValue(66.0) < 1.0);

    }

}
