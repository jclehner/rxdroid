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
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Hasher;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a dose intake by the user.
 *
 * Each database entry will consist of an id of the drug that was taken, a timestamp
 * representing the time the user marked the dose as 'taken' in the app, the dose-time, the <em>scheduled</em>
 * date (note that this may differ from the date represented by the timestamp. Assume for
 * example that the user takes a drug scheduled for the night at 1 minute past midnight.),
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
		return drug.getId();
	}

	public Drug getDrug() {
		return Database.getDrug(getDrugId());
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

		if(this.getDrugId() != other.getDrugId())
			return false;

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
	public static synchronized List<Intake> find(Drug drug, Date date, Integer doseTime)
	{
		final List<Intake> intakes = new LinkedList<Intake>();
	
		for(Intake intake : Database.getCachedIntakes())
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
