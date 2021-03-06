package kan.illuminated.chords;

import java.util.Calendar;
import java.util.Date;

/**
 * @author KAN
 */
public class DateUtils {

	public static Date today() {
		return toDay(new Date());
	}

	public static Date yesterday() {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());

		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		c.add(Calendar.DATE, -1);

		return c.getTime();
	}

	public static Date toDay(Date date) {

		Calendar c = Calendar.getInstance();
		c.setTime(date);

		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		return c.getTime();
	}

	public static Date firstWeekDate() {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());

		c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		return c.getTime();
	}

	public static Date toMonth(Date date) {

		Calendar c = Calendar.getInstance();
		c.setTime(date);

		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		return c.getTime();
	}

}
