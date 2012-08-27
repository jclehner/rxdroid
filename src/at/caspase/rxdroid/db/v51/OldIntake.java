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

package at.caspase.rxdroid.db.v51;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({ "unused", "serial" })
@DatabaseTable(tableName = "intake")
public class OldIntake extends Entry
{
	@DatabaseField(foreign = true)
	private Drug drug;

	@DatabaseField
	private java.util.Date date;

	@DatabaseField
	private java.util.Date timestamp;

	@DatabaseField
	private int doseTime;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction dose;

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		Intake newEntry = new Intake();
		Entry.copy(newEntry, this);
		// TODO some more magic here?
		return newEntry;
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
