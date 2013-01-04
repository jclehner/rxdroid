/**
 * Copyright (C) 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid.db;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.Fraction.MutableFraction;
import at.jclehner.rxdroid.db.schedules.ScheduleBase;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Handles advanced dose schedules.
 *
 * @see ScheduleBase
 *
 * @author Joseph Lehner
 *
 */
@DatabaseTable(tableName="schedules")
public final class Schedule extends Entry
{
	private static final long serialVersionUID = 7534352445550766725L;

	public static final int TIME_MORNING = 0;
	public static final int TIME_NOON    = 1;
	public static final int TIME_EVENING = 2;
	public static final int TIME_NIGHT   = 3;
	public static final int TIME_INVALID = 4;

	@SuppressWarnings("serial")
	public static abstract class Repetiton implements Serializable
	{
		public abstract boolean hasDoseOnDate(Date date);
		public abstract Date getNextDoseDate(Date date);
		public abstract int getSupplyDaysLeft(Fraction supplyLeft);
		public abstract Class<? extends Activity> getPreferenceActivityClass();
	}

	@DatabaseField(canBeNull = true)
	private String name;

	@DatabaseField(canBeNull = true)
	private Date begin;

	@DatabaseField(canBeNull = true)
	private Date end;

	@DatabaseField(dataType = DataType.SERIALIZABLE, canBeNull = true)
	private Repetiton repetition;

	@DatabaseField(persisterClass = FractionArrayPersister.class)
	private Fraction[] dosesDefault;

	@DatabaseField(persisterClass = FractionArrayPersister.class)
	private Fraction[] dosesMon;

	@DatabaseField(persisterClass = FractionArrayPersister.class)
	private Fraction[] dosesTue;

	@DatabaseField(persisterClass = FractionArrayPersister.class)
	private Fraction[] dosesWed;

	@DatabaseField(persisterClass = FractionArrayPersister.class)
	private Fraction[] dosesThu;

	@DatabaseField(persisterClass = FractionArrayPersister.class)
	private Fraction[] dosesFri;

	@DatabaseField(persisterClass = FractionArrayPersister.class)
	private Fraction[] dosesSat;

	@DatabaseField(persisterClass = FractionArrayPersister.class)
	private Fraction[] dosesSun;

	private transient Fraction[][] doses = null;

	public String getName() {
		return name;
	}

	public Fraction getDose(Date date, int doseTime)
	{
		if(date == null || doseTime >= TIME_INVALID)
			throw new IllegalArgumentException();

		if(!hasDoseOnDate(date))
			return Fraction.ZERO;

		Fraction[] doses = getDoses(date);
		if(doses == null)
			return Fraction.ZERO;

		return doses[doseTime];
	}

	public boolean hasDoseOnDate(Date date)
	{
		if(begin != null && date.before(begin))
			return false;
		else if(end != null && date.after(end))
			return false;

		if(repetition != null)
			return repetition.hasDoseOnDate(date);

		return !sum(getDoses(date)).isZero();
	}

	public boolean hasNoDoses()
	{



		for(Fraction[] doses : weeklySchedule)
		{
			if(doses == null)
				continue;

			for(Fraction dose : doses)
			{
				if(dose != null && !dose.isZero())
					return false;
			}
		}

		return true;
	}

	@Override
	public boolean equals(Object other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	private Fraction[] getDoses(Date date)
	{
		final Fraction[] doses;

		switch(DateTime.get(date, Calendar.DAY_OF_WEEK))
		{
			case Calendar.MONDAY:
				doses = dosesMon;
				break;

			case Calendar.TUESDAY:
				doses = dosesTue;
				break;

			case Calendar.WEDNESDAY:
				doses = dosesWed;
				break;

			case Calendar.THURSDAY:
				doses = dosesThu;
				break;

			case Calendar.FRIDAY:
				doses = dosesFri;
				break;

			case Calendar.SATURDAY:
				doses = dosesSat;
				break;

			case Calendar.SUNDAY:
				doses = dosesSun;
				break;

			default:
				throw new IllegalStateException("Unexpected DAY_OF_WEEK");
		}

		if(doses == null)
			return dosesDefault;

		return doses;
	}

	private static MutableFraction sum(Fraction[] fractions)
	{
		final MutableFraction sum = new MutableFraction();
		for(Fraction f : fractions)
			sum.add(f);

		return sum;
	}
}
