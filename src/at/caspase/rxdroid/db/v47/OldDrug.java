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

package at.caspase.rxdroid.db.v47;

import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;

@DatabaseTable(tableName="drugs")
@SuppressWarnings({ "unused", "serial" })
public class OldDrug extends Entry
{
	@DatabaseField(unique = true)
	private String name;

	@DatabaseField
	private int form;

	@DatabaseField(defaultValue = "true")
	private boolean active;

	@DatabaseField
	private int refillSize;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction currentSupply;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseMorning;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNoon;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseEvening;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNight;

	@DatabaseField(canBeNull = true)
	private int repeat;

	@DatabaseField(canBeNull = true)
	private long repeatArg;

	@DatabaseField(canBeNull = true)
	private Date repeatOrigin;

	@DatabaseField(canBeNull = true)
	private String comment;

	@Override
	protected Drug convertToCurrentDatabaseFormat()
	{
		Drug drug = new Drug();
		copy(drug, this);
		drug.setSortRank(0);
		assert drug.getId() != -1;
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
