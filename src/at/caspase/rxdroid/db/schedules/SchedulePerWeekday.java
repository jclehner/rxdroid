package at.caspase.rxdroid.db.schedules;

import java.util.Calendar;
import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Util;

public class SchedulePerWeekday extends ScheduleBase
{
	private static final long serialVersionUID = -4286054864425596076L;

	private Fraction[][] mDoses = new Fraction[7][Drug.TIME_INVALID];

	@Override
	public Fraction getDose(Date date, int doseTime)
	{
		final int weekday = getDateIndex(date);
		return mDoses[weekday][doseTime];
	}

	@Override
	public boolean hasDoseOnDate(Date date)
	{
		final int weekday = getDateIndex(date);

		for(int doseTime : Constants.DOSE_TIMES)
		{
			if(!mDoses[weekday][doseTime].isZero())
				return true;
		}

		return false;
	}

	@Override
	public void setDose(Date date, int doseTime, Fraction dose)
	{
		final int weekday = getDateIndex(date);
		mDoses[weekday][doseTime] = dose;
	}

	private SchedulePerWeekday()
	{
		for(int weekday = 0; weekday != mDoses.length; ++weekday)
		{
			for(int doseTime : Constants.DOSE_TIMES)
				mDoses[weekday][doseTime] = new Fraction();
		}
	}

	private static int getDateIndex(Date date) {
		return Util.calWeekdayToIndex(DateTime.get(date, Calendar.DAY_OF_WEEK));
	}

}
