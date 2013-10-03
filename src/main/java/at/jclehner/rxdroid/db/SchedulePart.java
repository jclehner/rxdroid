/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
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
	private Schedule owner;

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
