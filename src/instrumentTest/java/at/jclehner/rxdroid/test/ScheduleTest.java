package at.jclehner.rxdroid.test;

import android.test.AndroidTestCase;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.db.SchedulePart;
import at.jclehner.rxdroid.util.DateTime;

public class ScheduleTest extends AndroidTestCase
{
	private static final String TAG = ScheduleTest.class.getSimpleName();
	private static final String[] WEEKDAYS = {
			"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
	};

	Date begin;
	Date end;

	@Override
	protected void setUp()
	{
		try
		{
			super.setUp();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		//GlobalContext.set(mContext);
		RxDroid.setContext(mContext);
		Settings.init();
		Database.setInMemoryOnly(true);

		begin = DateTime.today();
		end = DateTime.add(begin, Calendar.DAY_OF_MONTH, 14);
	}

	public void testDrugWithScheduleParts1()
	{
		/* Drug schedule:
		 *
		 * Mon	0 - 0 - 1/4 - 0
		 * Tue	0 - 0 - 1/4 - 0
		 * Wed 	(null)
		 * Thu	0 - 0 - 1/2 - 0
		 * Fri  (null)
		 * Sat	0 - 0 - 1/2 - 0
		 * Sun	0 - 3/4 - 0	- 0
		 *
		 * ==> 3 Parts
		 */

		final SchedulePart part1 = new SchedulePart(
				SchedulePart.MONDAY | SchedulePart.TUESDAY, toFractionArray(0, 0, "1/4", 0));
		final SchedulePart part2 = new SchedulePart(
				SchedulePart.THURSDAY | SchedulePart.SATURDAY, toFractionArray(0, 0, "1/2", 0));
		final SchedulePart part3 = new SchedulePart(
				SchedulePart.SUNDAY, toFractionArray(0, "3/4", 0, 0));

		final Schedule schedule = new Schedule();
		for(int doseTime : Schedule.DOSE_TIMES)
			schedule.setDose(doseTime, Fraction.ZERO);

		schedule.setDose(Schedule.TIME_MORNING, new Fraction(1));

		schedule.setBegin(begin);
		schedule.setEnd(end);
		schedule.setScheduleParts(new SchedulePart[] { part1, part2, part3 });

		final Drug drug = new Drug();
		drug.setName("Drug SchedulePart 1");
		drug.addSchedule(schedule);
		drug.setRepeatMode(Drug.REPEAT_CUSTOM);

		Log.d(TAG, "testDrugWithScheduleParts1");

		for(Date date = begin; date.before(plusDays(end, 7)); date = nextDay(date))
		{
			int weekday = DateTime.getIsoWeekDayNumberIndex(date);

			Log.d(TAG, "  " + DateTime.toDateString(date) + " " + (WEEKDAYS[weekday]));

			if(drug.hasDoseOnDate(date))
			{
				for(int doseTime : Schedule.DOSE_TIMES)
					Log.d(TAG, "    " + doseTime + ": " + drug.getDose(doseTime, date));
			}
			else
				Log.d(TAG, "    (no doses)");
		}
	}

	private static Fraction[] toFractionArray(Object... args)
	{
		Fraction[] ret = new Fraction[args.length];

		for(int i = 0; i != args.length; ++i)
		{
			final Object arg = args[i];
			if(arg.getClass() == int.class || arg.getClass() == Integer.class)
				ret[i] = new Fraction((Integer) arg);
			else if(arg.getClass() == String.class)
				ret[i] = Fraction.valueOf((String) arg);
			else if(arg.getClass() == Fraction.class)
				ret[i] = (Fraction) arg;
			else
				throw new IllegalArgumentException("Index " + i + ": " + arg.getClass());
		}

		return ret;
	}

	private static Date nextDay(Date date) {
		return plusDays(date, 1);
	}

	private static Date plusDays(Date date, int days) {
		return DateTime.add(date, Calendar.DAY_OF_MONTH, days);
	}

}
