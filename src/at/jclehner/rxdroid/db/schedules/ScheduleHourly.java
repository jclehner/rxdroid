/**
 * Copyright (C) 2011, 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
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

import at.caspase.rxdroid.Fraction;
import at.jclehner.rxdroid.DumbTime;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.util.DateTime;

public class ScheduleHourly extends ScheduleBase
{
	private static final long serialVersionUID = -7584590071001608009L;

	private int mMode = 0;
	private Fraction[] mDoses = new Fraction[4];
	private int mDoseTimeToSkip = Drug.TIME_INVALID;

	private boolean mIn8HourMode = true;

	private DumbTime mOffset;

	public void enable8HourMode(boolean enable)
	{
		if(!(mIn8HourMode = enable))
			mDoseTimeToSkip = Drug.TIME_INVALID;
	}

	public boolean isIn8HourMode() {
		return mIn8HourMode;
	}

	public void setDoseTimeToSkip(int doseTime) {
		mDoseTimeToSkip = doseTime;
	}

	public int getDoseTimeToSkip() {
		return mDoseTimeToSkip;
	}

	public void setOffset(DumbTime offset)
	{
		final long max = 1000L * 3600 * (mIn8HourMode ? 8 : 6);
		if(offset.getMillisFromMidnight() >= max)
			throw new IllegalArgumentException();

		mOffset = offset;
	}

	@Override
	public Fraction getDose(Date date, int doseTime)
	{
		if(doseTime != mDoseTimeToSkip)
			return mDoses[doseTime];

		return new Fraction();
	}

	@Override
	public boolean hasDoseOnDate(Date date) {
		return true;
	}

	@Override
	public void setDose(Date date, int doseTime, Fraction dose)
	{
		if(doseTime != mDoseTimeToSkip)
			mDoses[doseTime] = dose;
	}

	@Override
	public Date getDoseTimeBegin(Date date, int doseTime)
	{
		if(!mIn8HourMode && doseTime == mDoseTimeToSkip)
			throw new IllegalArgumentException();

		Date begin = DateTime.add(date, Calendar.MILLISECOND, (int) mOffset.getMillisFromMidnight());

		return null;

	}
}
