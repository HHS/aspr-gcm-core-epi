package gcm.core.epi.test.manual.propertytypes;

import org.junit.Test;

import java.time.DayOfWeek;

public class TestDayOfWeek {

    @Test
    public void test() {
        DayOfWeek startDay = DayOfWeek.SUNDAY;

        double time = 3.25;

        System.out.println(startDay.plus((long) Math.floor(time)));

    }

}
