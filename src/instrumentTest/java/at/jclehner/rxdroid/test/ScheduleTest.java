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

	Date today;

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

		today = DateTime.today();
	}

	public void testDrugWithScheduleParts1()
	{
		/* Drug schedule:
		 *
		 * WEEK 1
		 *
		 * Mon	0 - 0 - 1/4 - 0
		 * Tue	0 - 0 - 1/4 - 0
		 * Wed 	(null)
		 * Thu	0 - 0 - 1/2 - 0
		 * Fri  (null)
		 * Sat	0 - 0 - 1/2 - 0
		 * Sun	0 - 3/4 - 0	- 0
		 *
		 * WEEK 2
		 *
		 * Mon	1 - 0 		- 0 - 0
		 * Tue	0 - 1 1/2	- 0 - 0
		 * Wed 	1 - 0		- 0 - 0
		 * Thu	(null)
		 * Fri  (null)
		 * Sat  (null)
		 * Sun	1 - 0		- 0 - 0
		 *
		 * WEEK 3
		 * (null)
		 *
		 * ==> 3 Parts
		 */

		final Object[][] testCases = new Object[][] {
				// week 1
				{ Drug.TIME_EVENING, "1/4" },
				{ Drug.TIME_EVENING, "1/4" },
				null,
				{ Drug.TIME_EVENING, "1/2" },
				null,
				{ Drug.TIME_EVENING, "1/2" },
				{ Drug.TIME_NOON, "3/4" },

				// week 2
				{ Drug.TIME_MORNING, "1" },
				{ Drug.TIME_NOON, "1 1/2" },
				{ Drug.TIME_MORNING, "1" },
				null,
				null,
				null,
				{ Drug.TIME_MORNING, "1" },

				// week 3
				null,
				null,
				null,
				null,
				null,
				null,
				null
		};

		// WEEK 1

		final SchedulePart part1 = new SchedulePart(
				SchedulePart.MONDAY | SchedulePart.TUESDAY, toFractionArray(0, 0, "1/4", 0));
		final SchedulePart part2 = new SchedulePart(
				SchedulePart.THURSDAY | SchedulePart.SATURDAY, toFractionArray(0, 0, "1/2", 0));
		final SchedulePart part3 = new SchedulePart(
				SchedulePart.SUNDAY, toFractionArray(0, "3/4", 0, 0));

		final Schedule schedule1 = new Schedule();
		for(int doseTime : Schedule.DOSE_TIMES)
			schedule1.setDose(doseTime, Fraction.ZERO);

		// 9 is October - idiots...
		schedule1.setBegin(DateTime.date(2013, 9, 14));
		schedule1.setEnd(plusDays(schedule1.getBegin(), 6));
		schedule1.setScheduleParts(new SchedulePart[] { part1, part2, part3 });

		// WEEK 2

		final SchedulePart part4 = new SchedulePart(
				SchedulePart.MONDAY | SchedulePart.WEDNESDAY | SchedulePart.SUNDAY, toFractionArray(1, 0, 0, 0));

		final SchedulePart part5 = new SchedulePart(
				SchedulePart.TUESDAY, toFractionArray(0, "1 1/2", 0, 0));

		final Schedule schedule2 = new Schedule();
		for(int doseTime : Schedule.DOSE_TIMES)
			schedule2.setDose(doseTime, Fraction.ZERO);

		schedule2.setBegin(plusDays(schedule1.getEnd(), 1));
		schedule2.setEnd(plusDays(schedule2.getBegin(), 6));
		schedule2.setScheduleParts(new SchedulePart[] { part4, part5 });

		// WEEK 3
		final Schedule schedule3 = new Schedule();
		schedule3.setBegin(plusDays(schedule2.getEnd(), 1));
		schedule3.setEnd(plusDays(schedule3.getBegin(), 6));

		for(int doseTime : Schedule.DOSE_TIMES)
			schedule3.setDose(doseTime, Fraction.ZERO);

		////////////////////////////////

		final Drug drug = new Drug();
		drug.setName("Drug SchedulePart 1");
		drug.addSchedule(schedule1);
		drug.addSchedule(schedule2);
		drug.addSchedule(schedule3);
		drug.setRepeatMode(Drug.REPEAT_CUSTOM);

		Log.d(TAG, "testDrugWithScheduleParts1");

		Date date = null;

		for(Object[] testCase : testCases)
		{
			if(date == null)
				date = schedule1.getBegin();
			else
				date = plusDays(date, 1);

			int weekday = DateTime.getIsoWeekDayNumberIndex(date);
			Log.d(TAG, "  " + DateTime.toDateString(date) + " " + (WEEKDAYS[weekday]));

			if(testCase == null)
			{
				if(drug.hasDoseOnDate(date))
					fail("hasDoseOnDate returned true");
			}
			else
			{
				int doseTime = (Integer) testCase[0];
				Fraction expected = Fraction.valueOf((String) testCase[1]);
				Fraction actual = drug.getDose(doseTime, date);

				assertEquals(expected, actual);
			}
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

	private static void assertEquals(Fraction frac1, Fraction frac2) {
		assertEquals(frac1.toString(), frac2.toString());
	}

	private static Date nextDay(Date date) {
		return plusDays(date, 1);
	}

	private static Date plusDays(Date date, int days) {
		return DateTime.add(date, Calendar.DAY_OF_MONTH, days);
	}

}
