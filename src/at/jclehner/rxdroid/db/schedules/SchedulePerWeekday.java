/**
 * RxDroid - A Medication Reminder
 * Copyright 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid.db.schedules;

import java.util.Calendar;
import java.util.Date;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

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
