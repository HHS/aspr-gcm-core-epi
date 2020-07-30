package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.time.DayOfWeek;
import java.util.Set;

@Value.Immutable
@JsonDeserialize(as = ImmutableDayOfWeekSchedule.class)
public abstract class DayOfWeekSchedule {

    public abstract Set<DayOfWeek> activeDays();

    @Value.Default
    public double fractionActive() {
        return (double) activeDays().size() / 7.0;
    }

    public boolean isActiveOn(DayOfWeek dayOfWeek) {
        return activeDays().contains(dayOfWeek);
    }

    public static DayOfWeekSchedule mondayToFriday() {
        return ImmutableDayOfWeekSchedule.builder()
                .addActiveDays(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY)
                .build();
    }

}
