/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.caspase.rxdroid.db.v42;

import java.sql.Date;
import java.util.Calendar;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.Hasher;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "drugs")
public class OldDrug extends Entry
{
	@Override
	public Drug convert()
	{
		Drug drug = new Drug(name, form, active, refillSize, currentSupply, getSchedule(), 
				frequency, frequencyArg, new java.util.Date(0));
		drug.setId(getId());
		
		return drug;		
	}
	
	private static final long serialVersionUID = -2569745648137404894L;

	public static final int FORM_TABLET = 0;
	public static final int FORM_INJECTION = 1;
	public static final int FORM_SPRAY = 2;
	public static final int FORM_DROP = 3;
	public static final int FORM_GEL = 4;
	public static final int FORM_OTHER = 5;

	public static final int TIME_MORNING = 0;
	public static final int TIME_NOON = 1;
	public static final int TIME_EVENING = 2;
	public static final int TIME_NIGHT = 3;
	public static final int TIME_WHOLE_DAY = 4;

	public static final int FREQ_DAILY = 0;
	public static final int FREQ_EVERY_OTHER_DAY = 1;
	public static final int FREQ_WEEKLY = 2;

	public static final String COLUMN_NAME = "name";

	@DatabaseField(unique = true)
	public String name;

	@DatabaseField(useGetSet = true)
	public int form;

	@DatabaseField(defaultValue = "true")
	public boolean active = true;

	// if mRefillSize == 0, mCurrentSupply should be ignored
	@DatabaseField(useGetSet = true)
	public int refillSize;

	@DatabaseField(dataType = DataType.SERIALIZABLE, useGetSet = true)
	public Fraction currentSupply = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	public Fraction doseMorning = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	public Fraction doseNoon = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	public Fraction doseEvening = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	public Fraction doseNight = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	public Fraction doseWholeDay = new Fraction();

	@DatabaseField(canBeNull = true, useGetSet = true)
	public int frequency = FREQ_DAILY;

	/**
	 * Defines the frequency origin.
	 *
	 * For every frequency other than {@link #FREQ_DAILY}, this field holds a specific value,
	 * allowing {@link #hasDoseOnDate(Date)} to determine whether a dose is pending
	 * on a specific date.
	 *
	 * <ul>
	 *     <li><code>FREQ_EVERY_OTHER_DAY</code>: field is set to a date (in milliseconds) where this drug's
	 *         intake should be set, i.e. if the date corresponds to 2011-09-07, there's an intake on that day,
	 *         another one on 2011-09-09, and so forth.</li>
	 *     <li><code>FREQ_WEEKLY</code>: field is set to a week day value from {@link java.util.Calendar}.</li>
	 * </ul>
	 */
	@DatabaseField(canBeNull = true)
	public long frequencyArg = 0;

	@DatabaseField(canBeNull = true)
	public String comment;

	public OldDrug() {}

	public int getForm() {
		return form;
	}

	public int getFrequency() {
		return frequency;
	}

	public long getFrequencyArg() {
		return frequencyArg;
	}

	public int getRefillSize() {
		return refillSize;
	}

	public Fraction getCurrentSupply() {
		return currentSupply;
	}
	
	public Fraction[] getSchedule() {
		return new Fraction[] { doseMorning, doseNoon, doseEvening, doseNight };
	}

	public void setForm(int form)
	{
		if(form > FORM_OTHER)
			throw new IllegalArgumentException();
		this.form = form;
	}

	public void setFrequency(int frequency)
	{
		if(frequency > FREQ_WEEKLY)
			throw new IllegalArgumentException();
		this.frequency = frequency;
	}

	public void setRefillSize(int refillSize)
	{
		if(refillSize < 0)
			throw new IllegalArgumentException();
		this.refillSize = refillSize;
	}

	public void setCurrentSupply(Fraction currentSupply)
	{
		if(currentSupply == null)
			this.currentSupply = Fraction.ZERO;
		else if(currentSupply.compareTo(0) == -1)
			throw new IllegalArgumentException();

		this.currentSupply = currentSupply;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof OldDrug))
			return false;

		final OldDrug other = (OldDrug) o;

		if(other == this)
			return true;

		final Object[] thisMembers = this.getFieldValues();
		final Object[] otherMembers = other.getFieldValues();

		for(int i = 0; i != thisMembers.length; ++i)
		{
			if(thisMembers[i] == null && otherMembers[i] == null)
				continue;
			else if(thisMembers[i] == null || otherMembers[i] == null)
				return false;
			else if(!thisMembers[i].equals(otherMembers[i]))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		final Hasher hasher = new Hasher();
		final Object[] thisMembers = this.getFieldValues();

		for(Object o : thisMembers)
			hasher.hash(o);

		return hasher.getHashCode();
	}

	@Override
	public String toString() {
		return name + "(" + id + ")={ " + doseMorning + " - " + doseNoon + " - " + doseEvening + " - " + doseNight + "}";
	}

	/**
	 * Get all relevant members for comparison/hashing.
	 *
	 * When comparing for equality or hashing, we ignore a drug's unique ID, as it may be left
	 * uninitialized and automatically determined by the SQLite logic.
	 *
	 * @return An array containing all fields but the ID.
	 */
	private Object[] getFieldValues()
	{
		final Object[] members = {
			this.name,
			this.form,
			this.active,
			this.doseMorning,
			this.doseNoon,
			this.doseEvening,
			this.doseNight,
			this.currentSupply,
			this.refillSize,
			this.frequency,
			this.frequencyArg,
			this.comment
		};

		return members;
	}
}