/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

import at.jclehner.androidutils.LazyValue;
import at.jclehner.rxdroid.Fraction;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a drug's schedule on specific weekdays.
 *
 * @author Joseph Lehner
 *
 */
@DatabaseTable
public class SchedulePart extends Entry
{
	public static final int MONDAY    = 1;
	public static final int TUESDAY   = 1 << 1;
	public static final int WEDNESDAY = 1 << 2;
	public static final int THURSDAY  = 1 << 3;
	public static final int FRIDAY    = 1 << 4;
	public static final int SATURDAY  = 1 << 5;
	public static final int SUNDAY    = 1 << 6;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseMorning;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNoon;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseEvening;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNight;

	@DatabaseField
	/* package */ int weekdays;

	@SuppressWarnings("unused")
	@DatabaseField(foreign = true)
	/* package */ transient Schedule owner;

	SchedulePart() {}

	public SchedulePart(int weekdays, Fraction[] doses)
	{
		setWeekdays(weekdays);
		doseMorning = doses[0];
		doseNoon = doses[1];
		doseEvening = doses[2];
		doseNight = doses[3];
	}

	transient private LazyValue<Fraction[]> mDoses = new LazyValue<Fraction[]>() {

		@Override
		public Fraction[] value()
		{
			return new Fraction[] {
					doseMorning, doseNoon, doseEvening, doseNight
			};
		}
	};

	public boolean hasDoses()
	{
		return !doseMorning.isZero() ||
				!doseNoon.isZero() ||
				!doseEvening.isZero() ||
				!doseNight.isZero();
	}

	public Fraction[] getDoses() {
		return mDoses.get();
	}

	public void setDose(int doseTime, Fraction dose)
	{
		switch(doseTime)
		{
			case Schedule.TIME_MORNING:
				doseMorning = dose;
				break;

			case Schedule.TIME_NOON:
				doseNoon = dose;
				break;

			case Schedule.TIME_EVENING:
				doseEvening = dose;
				break;

			case Schedule.TIME_NIGHT:
				doseNight = dose;
				break;

			default:
				throw new IllegalArgumentException("doseTime=" + doseTime);
		}

		mDoses.reset();
	}

	public Fraction getDose(int doseTime)
	{
		switch(doseTime)
		{
			case Schedule.TIME_MORNING:
				return doseMorning;

			case Schedule.TIME_NOON:
				return doseNoon;

			case Schedule.TIME_EVENING:
				return doseEvening;

			case Schedule.TIME_NIGHT:
				return doseNight;

			default:
				throw new IllegalArgumentException("doseTime=" + doseTime);
		}
	}

	public void setWeekdays(int weekdays)
	{
		if(weekdays < 0 || weekdays > 0x7f)
			throw new IllegalArgumentException("weekdays=" + Integer.toHexString(weekdays));

		this.weekdays = weekdays;
	}

	public int getWeekdays() {
		return weekdays;
	}

	@Override
	public boolean equals(Object other)
	{
		if(other == null || !(other instanceof SchedulePart))
			return false;

		if(weekdays != ((SchedulePart) other).weekdays)
			return false;

		if(!doseMorning.equals(((SchedulePart) other).doseMorning))
			return false;

		if(!doseNoon.equals(((SchedulePart) other).doseNoon))
			return false;

		if(!doseEvening.equals(((SchedulePart) other).doseEvening))
			return false;

		if(!doseNight.equals(((SchedulePart) other).doseNight))
			return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return 0;
	}
}
