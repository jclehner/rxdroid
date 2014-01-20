/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid.db;

import java.util.Date;
import java.util.List;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.util.DateTime;

/* package */ final class Schedules
{
	static Schedule getActiveSchedule(List<Schedule> schedules, Date date)
	{
		if(schedules != null)
		{
			for(Schedule s : schedules)
			{
				if(isDateWithinSchedule(date, s))
					return s;
			}
		}

		return null;
	}

	static Fraction getDose(Date date, int doseTime, List<Schedule> schedules)
	{
		final Schedule active = getActiveSchedule(schedules, date);
		if(active != null)
			return active.getDose(date, doseTime);

		return Fraction.ZERO;
	}

	static boolean hasDoseOnDate(Date date, List<Schedule> schedules)
	{
		final Schedule active = getActiveSchedule(schedules, date);
		if(active != null)
			return active.hasDoseOnDate(date);

		return false;
	}

	static boolean hasNoDoses(List<Schedule> schedules)
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
