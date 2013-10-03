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

package at.jclehner.rxdroid.db.v56;

import java.util.Date;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.FractionPersister;
import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.db.Schedule;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({ "unused" })
@DatabaseTable(tableName = "drugs")
public class OldDrug extends Entry
{
	@DatabaseField(unique = true)
	private String name;

	@DatabaseField(foreign = true)
	private Patient patient;

	@DatabaseField
	private int icon;

	@DatabaseField
	private boolean active = true;

	@DatabaseField
	private int refillSize;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction currentSupply = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseMorning = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNoon = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseEvening = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNight = Fraction.ZERO;

	@DatabaseField
	private int repeatMode= Drug.REPEAT_DAILY;

	@DatabaseField
	private long repeatArg = 0;

	@DatabaseField
	private Date repeatOrigin;

	@DatabaseField
	private boolean autoAddIntakes = false;

	@DatabaseField
	private Date lastAutoIntakeCreationDate;

	@DatabaseField
	private int sortRank = Integer.MAX_VALUE;

	@DatabaseField(foreign = true)
	private Schedule schedule;

	@DatabaseField
	private String comment;

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		final Drug newDrug = new Drug();
		Entry.copy(newDrug, this);
		// TODO some more magic here?
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
