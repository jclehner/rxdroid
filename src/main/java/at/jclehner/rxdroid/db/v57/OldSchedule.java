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

package at.jclehner.rxdroid.db.v57;

import java.util.Date;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.FractionPersister;
import at.jclehner.rxdroid.db.Schedule;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({ "unused" })
@DatabaseTable(tableName="schedules")
public class OldSchedule extends Entry
{
	public static final int REPEAT_DAILY = 0;
	public static final int REPEAT_AS_NEEDED = 1;
	public static final int REPEAT_EVERY_N_DAYS = 2;
	public static final int REPEAT_EVERY_6_8_12_OR_24_HOURS = 3;
	public static final int REPEAT_WEEKDAYS = 4;
	public static final int REPEAT_DAILY_WITH_PAUSE = 5;

	@DatabaseField
	private String name;

	@DatabaseField(canBeNull = false)
	/* package */ Date begin;

	@DatabaseField
	/* package */ Date end;

	@DatabaseField
	private int repeatMode;

	@DatabaseField
	private long repeatArg;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseMorning;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNoon;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseEvening;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNight;

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		final Schedule newSchedule = new Schedule();
		Entry.copy(newSchedule, this);

		if(repeatMode == REPEAT_AS_NEEDED)
		{
			newSchedule.setAsNeeded(true);
			newSchedule.setRepeatMode(Schedule.REPEAT_DAILY);
		}
		else
			newSchedule.setRepeatMode(toCurrentScheduleRepeatMode(repeatMode));

		return newSchedule;
	}

	@Override
	public boolean equals(Object other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	public static int toCurrentScheduleRepeatMode(int oldRepeatMode)
	{
		switch(oldRepeatMode)
		{
			case REPEAT_DAILY:
				return Schedule.REPEAT_DAILY;

			case REPEAT_EVERY_N_DAYS:
				return Schedule.REPEAT_EVERY_N_DAYS;

			case REPEAT_EVERY_6_8_12_OR_24_HOURS:
				return Schedule.REPEAT_EVERY_6_8_12_OR_24_HOURS;

			case REPEAT_WEEKDAYS:
				return Schedule.REPEAT_WEEKDAYS;

			case REPEAT_DAILY_WITH_PAUSE:
				return Schedule.REPEAT_DAILY_WITH_PAUSE;

			default:
				throw new UnsupportedOperationException();
		}
	}

}
