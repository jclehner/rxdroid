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

package at.jclehner.rxdroid.db.v48;

import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({"serial", "unused"})
@DatabaseTable(tableName="drugs")
public class OldDrug extends Entry
{
	public OldDrug() {}

	@DatabaseField
	private Date repeatOrigin;

	@DatabaseField
	private String comment;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction currentSupply;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction doseEvening;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction doseMorning;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction doseNight;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction doseNoon;

	@DatabaseField
	private String name;

	@DatabaseField
	private long repeatArg;

	@DatabaseField
	private int refillSize;

	@DatabaseField
	private int repeat;

	@DatabaseField
	private int form;

	@DatabaseField
	private boolean active;

	@DatabaseField
	private int sortRank;

	@Override
	protected Entry convertToCurrentDatabaseFormat()
	{
		Drug drug = new Drug();
		Entry.copy(drug, this);
		drug.setSchedule(null);
		return drug;
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