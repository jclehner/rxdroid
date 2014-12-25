/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

import java.sql.Timestamp;
import java.util.Date;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Hasher;
import at.jclehner.rxdroid.util.Util;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a dose intake event.
 * <p>
 * An intake event is created whenever the user takes a dose. Each
 * entry will contain the following information:
 * <ul>
 * <li>A reference to the Drug that was taken</li>
 * <li>The date at which the intake <em>should</em> have happened<sup>*</sup></li>
 * <li>The exact time at which the dose was <em>actually</em> taken</li>
 * <li>The dose-time (see Drug docs)</li>
 * <li>The actual dose that was taken, as it might differ from the Drug's intake schedule</li>
 * </ul>
 *
 * <sup>*</sup>) Consider a dose that should be taken at night. If the user takes that dose
 * after midnight, the {@link #date} field will contain the date <em>before</em> midnight,
 * while the {@link #timestamp} will be set to the actual time.
 *
 * @see Drug
 *
 * @author Joseph Lehner
 */

@DatabaseTable(tableName = "dose_events")
public class DoseEvent extends Entry
{
	@DatabaseField(foreign = true)
	private Drug drug;

	@DatabaseField
	private java.util.Date date;

	@DatabaseField
	private java.util.Date timestamp;

	@DatabaseField
	private int doseTime;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction dose;

	@DatabaseField
	private boolean wasAutoCreated = false;

	public DoseEvent() {}

	public DoseEvent(Drug drug, Date date, int doseTime, Fraction dose)
	{
		this.drug = drug;
		this.date = date;
		this.timestamp = new Timestamp(System.currentTimeMillis());
		this.doseTime = doseTime;
		this.dose = dose;
	}

	/**
	 * Constructs an empty intake.
	 * <p>
	 * Empty events (i.e. events with a dose of zero) are
	 * used for ignoring a dose.
	 */
	public DoseEvent(Drug drug, Date date, int doseTime) {
		this(drug, date, doseTime, Fraction.ZERO);
	}

	public int getDrugId() {
		return drug.id;
	}

	public Drug getDrug() {
		return Drug.find(drug.id);
	}

	public Fraction getDose() {
		return dose;
	}

	public void setDose(Fraction dose) {
		this.dose = dose;
	}

	public Date getDate() {
		return date;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public int getDoseTime() {
		return doseTime;
	}

	public boolean wasAutoCreated() {
		return wasAutoCreated;
	}

	public void setWasAutoCreated(boolean wasAutoCreated) {
		this.wasAutoCreated = wasAutoCreated;
	}

	@Override
	public int hashCode()
	{
		final Hasher hasher = Hasher.getInstance();

		hasher.hash(drug);
		hasher.hash(date);
		hasher.hash(timestamp);
		hasher.hash(doseTime);
		hasher.hash(dose);

		return hasher.getHashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof DoseEvent))
			return false;

		final DoseEvent other = (DoseEvent) o;

		if(this.doseTime != other.doseTime)
			return false;

		if(!Util.equalsIgnoresNull(this.timestamp, other.timestamp))
			return false;

		if(!this.date.equals(other.date))
			return false;

		/*if(this.getDrugId() != other.getDrugId())
			return false;*/

		if(!this.dose.equals(other.dose))
			return false;

		return true;
	}

	@Override
	public String toString()
	{
		final Drug drug = getDrug();
		final String drugName = drug == null ? "<deleted>" : drug.getName();

		return drugName + ": " + date + " " + Entries.getDoseTimeString(doseTime) + ", " + dose;
	}

	/* package */ static boolean has(DoseEvent intake, Drug drug, Date date, Integer doseTime)
	{
		if(drug.id != intake.drug.id)
			return false;

		if(doseTime != null && doseTime != intake.doseTime)
			return false;

		if(date != null && !DateTime.equalsDate(date, intake.date))
			return false;

		return true;
	}
}
