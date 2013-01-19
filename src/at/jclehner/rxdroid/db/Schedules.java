package at.jclehner.rxdroid.db;

import java.util.Date;

import android.util.Log;
import at.jclehner.rxdroid.Fraction;

/* package */ final class Schedules
{
	static Fraction getDose(Date date, int doseTime, Schedule[] schedules)
	{
		if(schedules != null)
		{
			for(Schedule s : schedules)
			{
				if(isDateWithinSchedule(date, s))
					return s.getDose(date, doseTime);
			}
		}

		return Fraction.ZERO;
	}

	static boolean hasDoseOnDate(Date date, Schedule[] schedules)
	{
		for(Schedule s : schedules)
		{
			if(isDateWithinSchedule(date, s))
				return s.hasDoseOnDate(date);
		}

		return false;
	}

	static boolean hasNoDoses(Schedule[] schedules)
	{
		for(Schedule s : schedules)
		{
			if(!s.hasNoDoses())
				return false;
		}

		return true;
	}

	static boolean isDateWithinSchedule(Date date, Schedule schedule)
	{
		if(date.before(schedule.begin))
			return false;
		else if(schedule.end != null)
			return !date.after(schedule.end);
		else
			return true;
	}

	private Schedules() {}
}
