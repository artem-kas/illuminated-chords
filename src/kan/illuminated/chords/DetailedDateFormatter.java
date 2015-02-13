package kan.illuminated.chords;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static kan.illuminated.chords.DateUtils.*;

/**
 * @author KAN
 */
public class DetailedDateFormatter {

	private Calendar now = Calendar.getInstance();

	private SimpleDateFormat monthFormat;
	private SimpleDateFormat monthYearFormat;

	public DetailedDateFormatter() {

		DateFormatSymbols dfs = new DateFormatSymbols();

		String[] months = ChordsApplication.appContext.getResources().getStringArray(R.array.months);
		if (months != null) {
			dfs.setMonths(months);
		}

		monthFormat = new SimpleDateFormat("MMMM", dfs);
		monthYearFormat = new SimpleDateFormat("MMMM yyyy", dfs);
	}

	public String monthString(Date date) {

		if (date.getTime() > now.getTime().getTime()) {

			// future dates are fully specified

			return monthYearFormat.format(date);
		}

		Calendar c = Calendar.getInstance();
		c.setTime(date);

		if (date.equals(today())) {

			// today

			return ChordsApplication.appContext.getResources().getString(R.string.today);
		}

		if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
				now.get(Calendar.MONTH) == c.get(Calendar.MONTH)) {

			// special value for current month

			return ChordsApplication.appContext.getResources().getString(R.string.current_month);
		}

		if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR) ||
				(now.get(Calendar.YEAR) == c.get(Calendar.YEAR) + 1 &&
						now.get(Calendar.MONTH) < c.get(Calendar.MONTH))
				) {

			// less then year ago - show only month

			return monthFormat.format(date);
		} else {

			// older then a year ago - full format

			return monthYearFormat.format(date);
		}
	}
}
