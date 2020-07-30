package gcm.core.epi.util.time;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.simulation.Environment;

import java.time.DayOfWeek;

public class TimeUtils {

    public static DayOfWeek getCurrentDayOfWeek(Environment environment) {
        return getDayOfWeek(environment, environment.getTime());
    }

    public static DayOfWeek getDayOfWeek(Environment environment, double time) {
        DayOfWeek simulationStartDay = environment.getGlobalPropertyValue(GlobalProperty.SIMULATION_START_DAY);
        return simulationStartDay.plus((long) Math.floor(time));
    }

}
