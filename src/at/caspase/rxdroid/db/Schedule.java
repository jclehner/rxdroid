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

package at.caspase.rxdroid.db;

import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.schedules.ScheduleBase;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Handles advanced intake schedules.
 * <p>
 * Note that an advanced schedule is implemented by extending
 * {@link ScheduleBase}. This class merely serves as a wrapper to accomodate
 * all schedules in one DB table. To achieve this, the schedule
 * class extending {@link ScheduleBase} is serialized into this DB entry's
 * <code>schedule</code> field. All function calls are thus relayed
 * to the actual implementation in said field.
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

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private ScheduleBase schedule;

	public Schedule() {}

	public Schedule(ScheduleBase scheduleImpl) {
		schedule = scheduleImpl;
	}

	public Fraction getDose(Date date, int doseTime) {
		return schedule.getDose(date, doseTime);
	}

	public boolean hasDoseOnDate(Date date) {
		return schedule.hasDoseOnDate(date);
	}

	public void setDose(Date date, int doseTime, Fraction dose) {
		schedule.setDose(date, doseTime, dose);
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
