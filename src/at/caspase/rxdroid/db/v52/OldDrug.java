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

package at.caspase.rxdroid.db.v52;

import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Schedule;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({ "unused", "serial" })
@DatabaseTable(tableName = "drugs")
public class OldDrug extends Entry
{
	@DatabaseField(unique = true)
	private String name;

	@DatabaseField
	private int form;

	@DatabaseField
	private boolean active = true;

	@DatabaseField
	private int refillSize;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction currentSupply = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseMorning = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNoon = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseEvening = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNight = new Fraction();

	@DatabaseField(columnName = "repeat")
	private int repeatMode= Drug.REPEAT_DAILY;

	@DatabaseField
	private long repeatArg = 0;

	@DatabaseField
	private Date repeatOrigin;

	@DatabaseField
	private boolean autoAddIntakes = false;

	@DatabaseField
	private int sortRank = 0;

	@DatabaseField(foreign = true)
	private Schedule schedule;

	@DatabaseField
	private String comment;

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		final Drug newDrug = new Drug();
		Entry.copy(newDrug, this);
		return newDrug;
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
