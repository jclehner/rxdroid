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

package at.caspase.rxdroid.db;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.util.Hasher;

import com.j256.ormlite.field.DataType;
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

@DatabaseTable(tableName = "intake")
public class Intake extends Entry
{
	private static final long serialVersionUID = -9158847314588407608L;

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

	public Intake() {}

	public Intake(Drug drug, Date date, int doseTime, Fraction dose)
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
	 * Empty intakes (i.e. intakes with a dose of zero) are
	 * used for ignoring a dose.
	 */
	public Intake(Drug drug, Date date, int doseTime) {
		this(drug, date, doseTime, new Fraction());
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

	public Date getDate() {
		return new Date(date.getTime());
	}

	public Date getTimestamp() {
		return new Timestamp(timestamp.getTime());
	}

	public int getDoseTime() {
		return doseTime;
	}

	public boolean isEmptyIntake() {
		return dose == null || dose.isZero();
	}

	@Override
	public int hashCode()
	{
		final Hasher hasher = new Hasher();

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
		if(!(o instanceof Intake))
			return false;

		final Intake other = (Intake) o;

		if(this.doseTime != other.doseTime)
			return false;

		if(!this.timestamp.equals(other.timestamp))
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
	public String toString() {
		return drug + ": date=" + date + ", doseTime=" + doseTime + ", dose=" + dose;
	}

	/**
	 * Find all intakes meeting the specified criteria.
	 * <p>
	 * @param drug The drug to search for (based on its database ID).
	 * @param date The Intake's date. Can be <code>null</code>.
	 * @param doseTime The Intake's doseTime. Can be <code>null</code>.
	 */
	public static synchronized List<Intake> findAll(Drug drug, Date date, Integer doseTime)
	{
		final List<Intake> intakes = new LinkedList<Intake>();

		for(Intake intake : Database.getCached(Intake.class))
		{
			if(intake.getDrugId() != drug.getId())
				continue;
			if(date != null && !intake.getDate().equals(date))
				continue;
			if(doseTime != null && intake.getDoseTime() != doseTime)
				continue;

			intakes.add(intake);
		}

		return intakes;
	}
}
