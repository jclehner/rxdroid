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

package at.jclehner.rxdroid.db.schedules;

import java.io.Serializable;
import java.util.Date;

import at.caspase.rxdroid.Fraction;

/**
 * Abstract base class for all advanced schedules.
 *
 * @author Joseph Lehner
 */
@SuppressWarnings("serial")
public abstract class ScheduleBase implements Serializable
{
	/**
	 * Returns the dose for a given date and dose-time.
	 *
	 * @return the dose. Must not be <code>null</code>.
	 */
	public abstract Fraction getDose(Date date, int doseTime);

	/**
	 * Checks whether there are any doses on a given day.
	 *
	 * @return <code>true</code> if the given drug has at least one non-zero dose on a given date.
	 */
	public abstract boolean hasDoseOnDate(Date date);

	/**
	 * Sets the dose for a given date and dose-time.
	 */
	public abstract void setDose(Date date, int doseTime, Fraction dose);

	/**
	 * Returns the beginning of the specified dose time.
	 * <p>
	 * @return <code>null</code> by default, telling RxDroid to use the
	 * 	values specified in the application-wide settings.
	 */
	public Date getDoseTimeBegin(Date date, int doseTime) {
		return null;
	}

	/**
	 * Returns the end of the specified dose time.
	 * <p>
	 * @return <code>null</code> by default, telling RxDroid to use the
	 * 	values specified in the application-wide settings.
	 */
	public Date getDoseTimeEnd(Date date, int doseTime) {
		return null;
	}

	@Override
	public boolean equals(Object other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}
}
