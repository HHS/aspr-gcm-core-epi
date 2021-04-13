package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.identifiers.GlobalProperty;
import org.immutables.value.Value;
import plugins.gcm.agents.Environment;

import java.time.DayOfWeek;
import java.util.Set;

@Value.Immutable
@JsonDeserialize(as = ImmutableDayOfWeekSchedule.class)
public abstract class DayOfWeekSchedule {

    public static DayOfWeekSchedule mondayToFriday() {
        return ImmutableDayOfWeekSchedule.builder()
                .addActiveDays(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY)
                .build();
    }

    public static DayOfWeekSchedule everyDay() {
        return ImmutableDayOfWeekSchedule.builder()
                .addActiveDays(DayOfWeek.values())
                .build();
    }

    public abstract Set<DayOfWeek> activeDays();

    @Value.Default
    public double startOffset() {
        return 0.0;
    }

    @Value.Default
    public double duration() {
        return Double.POSITIVE_INFINITY;
    }

    @Value.Default
    public double restartOffset() {
        return 0.0;
    }

    @Value.Default
    public double fractionActive() {
        return (double) activeDays().size() / 7.0;
    }

    public boolean isActiveOn(DayOfWeek dayOfWeek) {
        return activeDays().contains(dayOfWeek);
    }

    public boolean isActiveAt(Environment environment, double time) {
        DayOfWeek simulationStartDay = environment.getGlobalPropertyValue(GlobalProperty.SIMULATION_START_DAY);
        boolean isActiveDayOfWeek = isActiveOn(simulationStartDay.plus((long) Math.floor(time)));
        boolean isActiveTimePeriod = time >= startOffset() && (time - startOffset()) % (duration() + restartOffset()) < duration();
        return isActiveDayOfWeek && isActiveTimePeriod;
    }

}
