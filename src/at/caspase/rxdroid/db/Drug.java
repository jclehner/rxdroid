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

package at.caspase.rxdroid.db;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import android.util.Log;
import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Hasher;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Class for handling the drug database.
 * <p>
 * The word "dose" in the context of this text (and this text ONLY) refers to
 * the smallest available dose of that drug without having to
 * manually reduce its amount (i.e. no pill-splitting). For example,
 * a package of Aspirin containing 30 tablets contains 30 doses; of
 * course, the intake schedule may also contain doses in fractions.
 * <p>
 * Another term you'll come across in the docs and the code is the
 * concept of a 'dose-time'. A dose-time is a user-definable subdivision
 * of the day, having one of the following predefined names: morning,
 * noon, evening, night.
 * <p>
 * Any drug in the database will have the following attributes:
 * <ul>
 *  <li>A unique name</li>
 *  <li>The form of the medication. This will be reflected in the UI by
 *      displaying a corresponding icon next to the drug's name.</li>
 *  <li>The size of one refill. This corresponds to the amount of doses
 *      per prescription, package, etc. Note that due to the definition of
 *      the word "dose" mentioned above, this size must not be a fraction.</li>
 *  <li>The current supply. This contains the number of doses left for this particular drug.</li>
 *  <li>An optional comment for that drug (e.g. "Take with food").</li>
 *  <li>A field indicating whether the drug should be considered active. A drug marked
 *      as inactive will be ignored by the DrugNotificationService.</li>
 * </ul>
 *
 * @author Joseph Lehner
 *
 */
@DatabaseTable(tableName = "drugs")
public class Drug extends Entry implements Comparable<Drug>
{
	private static final String TAG = Drug.class.getName();
	private static final long serialVersionUID = -2569745648137404894L;

	public static final int FORM_PILL = 0;
	public static final int FORM_INJECTION = 1;
	public static final int FORM_SPRAY = 2;
	public static final int FORM_DROP = 3;
	public static final int FORM_GEL = 4;
	public static final int FORM_OTHER = 5;

	public static final int TIME_MORNING = 0;
	public static final int TIME_NOON = 1;
	public static final int TIME_EVENING = 2;
	public static final int TIME_NIGHT = 3;
	/**
	 * All dose-times &lt;= this value are considered invalid.
	 * <p>
	 * You can also use this value in a <code>for</code> loop, which
	 * will guarantee that the value of <code>doseTime</code> will always
	 * be a valid dose-time. Example:
	 * <pre>
	 * {@code
	 * for(int doseTime = TIME_MORNING; doseTime != TIME_INVALD; ++doseTime) {
	 * 	 // do something
	 * }
	 * }
	 * </pre>
	 */
	public static final int TIME_INVALID = 4;

	public static final int REPEAT_DAILY = 0;
	public static final int REPEAT_EVERY_N_DAYS = 1;
	public static final int REPEAT_WEEKDAYS = 2;
	public static final int REPEAT_ON_DEMAND = 3;
	public static final int REPEAT_CUSTOM = 4;
	public static final int REPEAT_INVALID = 5;

	// TODO valid arguments: 6, 8, 12, with automapping to doseTimes
	public static final int REPEAT_EVERY_N_HOURS = -1;

	public static final int REPEATARG_DAY_MON = 1;
	public static final int REPEATARG_DAY_TUE = 1 << 1;
	public static final int REPEATARG_DAY_WED = 1 << 2;
	public static final int REPEATARG_DAY_THU = 1 << 3;
	public static final int REPEATARG_DAY_FRI = 1 << 4;
	public static final int REPEATARG_DAY_SAT = 1 << 5;
	public static final int REPEATARG_DAY_SUN = 1 << 6;

	@DatabaseField(unique = true)
	private String name;

	@DatabaseField(useGetSet = true)
	private int form;

	@DatabaseField(defaultValue = "true")
	private boolean active = true;

	// if mRefillSize == 0, mCurrentSupply should be ignored
	@DatabaseField(useGetSet = true)
	private int refillSize;

	@DatabaseField(dataType = DataType.SERIALIZABLE, useGetSet = true)
	private Fraction currentSupply = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseMorning = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNoon = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseEvening = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNight = new Fraction();

	// TODO change column name
	@DatabaseField(canBeNull = true)
	private int repeat = REPEAT_DAILY;

	/**
	 * Defines the repeat origin.
	 *
	 * For every repeat other than {@link #REPEAT_DAILY}, this field holds a specific value,
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
	private long repeatArg = 0;

	@DatabaseField(canBeNull = true)
	private Date repeatOrigin;

	@DatabaseField
	private int sortRank = 0;

	@DatabaseField(foreign = true, canBeNull = true)
	private Schedule schedule;

	@DatabaseField(canBeNull = true)
	private String comment;

	private Fraction[] mSimpleSchedule;

	/**
	 * Default constructor, required by ORMLite.
	 */
	public Drug() {}

	@Deprecated
	public boolean hasDoseOnDate(Calendar cal) {
		return hasDoseOnDate(cal.getTime());
	}

	public boolean hasDoseOnDate(Date date)
	{
		switch(repeat)
		{
			case REPEAT_DAILY:
			case REPEAT_ON_DEMAND:
				return true;

			case REPEAT_EVERY_N_DAYS:
				if(date.before(repeatOrigin))
					return false;

				final long diffDays = (repeatOrigin.getTime() - date.getTime()) / Constants.MILLIS_PER_DAY;
				return diffDays % repeatArg == 0;

			case REPEAT_WEEKDAYS:
				final Calendar cal = DateTime.calendarFromDate(date);
				return hasDoseOnWeekday(cal.get(Calendar.DAY_OF_WEEK));

			case REPEAT_CUSTOM:
				return schedule.hasDoseOnDate(date);

			default:
				throw new RuntimeException("Unknown repeat mode");
		}
	}

	public String getName() {
		return name;
	}

	public int getForm() {
		return form;
	}

	public int getFormResourceId()
	{
		switch(form)
		{
			case FORM_INJECTION:
				return R.drawable.ic_drug_syringe;

			case FORM_DROP:
				return R.drawable.ic_drug_drink;

			case FORM_GEL:
				return R.drawable.ic_drug_tube;

			case FORM_PILL:
				// fall through

			default:
				return R.drawable.ic_drug_pill;

			// FIXME
		}
	}

	public int getRepeat() {
		return repeat;
	}

	public long getRepeatArg() {
		return repeatArg;
	}

	public Date getRepeatOrigin() {
		return repeatOrigin;
	}

	public boolean isActive() {
		return active;
	}

	public int getRefillSize() {
		return refillSize;
	}

	public Fraction getCurrentSupply() {
		return currentSupply;
	}

	public int getCurrentSupplyDays()
	{
		final Fraction dailyDose = getDailyDose();
		if(dailyDose.isZero())
			return 0;

		final Date today = DateTime.todayDate();

		// determine the sum of the doses still to be taken today and
		// subtract that from the current supply to get the true
		// number of days remaining
		double doseRemainingToday = 0.0;

		for(int doseTime : Constants.DOSE_TIMES)
		{
			Fraction cumulativeDose = new Fraction();

			final List<Intake> intakes = Intake.findAll(this, today, doseTime);
			for(Intake intake : intakes)
				cumulativeDose.add(intake.getDose());

			if(cumulativeDose.isZero())
			{
				final Fraction dose = getDose(doseTime, today);
				doseRemainingToday += dose.doubleValue();
			}

			//
			//Fraction expectedDose = getDose(doseTime, today);
			//if(cumulativeDose.compareTo(expectedDose) < 0)
			//	doseRemainingToday += expectedDose.minus(cumulativeDose).doubleValue();
			//
		}

		final double supply = this.currentSupply.doubleValue() - doseRemainingToday;
		final double correctionFactor = getSupplyCorrectionFactor();

		return (int) Math.floor(supply / dailyDose.doubleValue() * correctionFactor);
	}

	public double getSupplyCorrectionFactor()
	{
		switch(repeat)
		{
			case REPEAT_EVERY_N_DAYS:
				return repeatArg / 1.0;

			case REPEAT_WEEKDAYS:
				return 7.0 / Long.bitCount(repeatArg);

			default:
				return 1.0;
		}
	}

	public Fraction[] getSimpleSchedule()
	{
		if(mSimpleSchedule == null)
			mSimpleSchedule = new Fraction[] { doseMorning, doseNoon, doseEvening, doseNight };

		return mSimpleSchedule;
	}

	public Fraction getDose(int doseTime)
	{
		if(repeat == REPEAT_CUSTOM)
			throw new UnsupportedOperationException("This function cannot be used in conjunction with a custom schedule");

		switch(doseTime)
		{
			case TIME_MORNING:
				return doseMorning;
			case TIME_NOON:
				return doseNoon;
			case TIME_EVENING:
				return doseEvening;
			case TIME_NIGHT:
				return doseNight;
			default:
				throw new IllegalArgumentException();
		}
	}

	public Fraction getDose(int doseTime, Date date)
	{
		if(repeat != REPEAT_CUSTOM)
		{
			if(!hasDoseOnDate(date))
				return new Fraction(0);
			return getDose(doseTime);
		}

		return schedule.getDose(date, doseTime);
	}

	private Fraction getDailyDose()
	{
		final Fraction dailyDose = new Fraction();

		for(Fraction dose : getSimpleSchedule())
			dailyDose.add(dose);

		return dailyDose;
	}

	public String getComment() {
		return comment;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setForm(int form)
	{
		if(form > FORM_OTHER)
			throw new IllegalArgumentException();
		this.form = form;
	}

	public void setRepeat(int repeat)
	{
		if(repeat >= REPEAT_INVALID)
			throw new IllegalArgumentException();

		if(repeat == this.repeat)
			return;

		Log.d(TAG, "setRepeat(" + repeat + ") on " + toString());

		// the preference was changed, so reset all repeat-related settings
		this.repeat = repeat;
		this.repeatArg = 0;
		this.repeatOrigin = DateTime.todayDate();
	}

	/**
	 * Sets the repeat mode.
	 *
	 * @param repeatArg the exact interpretation of this value depends on the repeat mode currently set.
	 * @throws IllegalArgumentException if the setting is out of bounds for this instance's repeat mode.
	 * @throws UnsupportedOperationException if this instance's repeat mode does not expect any arguments.
	 */
	public void setRepeatArg(long repeatArg)
	{
		if(repeat == REPEAT_EVERY_N_DAYS)
		{
			if(repeatArg <= 1)
				throw new IllegalArgumentException();
		}
		else if(repeat == REPEAT_WEEKDAYS)
		{
			// binary(01111111) = hex(0x7f) (all weekdays)
			if(repeatArg <= 0 || repeatArg > 0x7f)
				throw new IllegalArgumentException();
		}
		else if(repeat == REPEAT_EVERY_N_HOURS)
		{
			if(repeatArg != 6 && repeatArg != 8 && repeatArg != 12)
				throw new IllegalArgumentException();
		}
		else
			throw new UnsupportedOperationException();

		this.repeatArg = repeatArg;
	}

	/**
	 * Sets the repeat origin.
	 * @param repeatOrigin
	 * @throws UnsupportedOperationException if this instance's repeat mode does not allow a repeat origin to be set.
	 * @throws IllegalArgumentException if the setting is out of bounds for this instance's repeat mode.
	 */
	public void setRepeatOrigin(Date repeatOrigin)
	{
		if(repeat != REPEAT_EVERY_N_DAYS && repeat != REPEAT_EVERY_N_HOURS)
			throw new UnsupportedOperationException();

		if(repeat == REPEAT_EVERY_N_DAYS && DateTime.getOffsetFromMidnight(repeatOrigin) != 0)
			throw new IllegalArgumentException();

		this.repeatOrigin = repeatOrigin;
	}

	public void setActive(boolean active) {
		this.active = active;
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

	public synchronized void setDose(int doseTime, Fraction value)
	{
		switch(doseTime)
		{
			case TIME_MORNING:
				doseMorning = value;
				break;
			case TIME_NOON:
				doseNoon = value;
				break;
			case TIME_EVENING:
				doseEvening = value;
				break;
			case TIME_NIGHT:
				doseNight = value;
				break;
			default:
				throw new IllegalArgumentException();
		}

		if(mSimpleSchedule != null)
			mSimpleSchedule[doseTime] = value;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getSortRank() {
		return sortRank;
	}

	public void setSortRank(int sortRank) {
		this.sortRank = sortRank;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}


	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Drug))
			return false;

		final Drug other = (Drug) o;

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
		return id + ":\"" + name + "\"=" + Arrays.toString(getSimpleSchedule());
	}

	@Override
	public int compareTo(Drug other)
	{
		int thisRank = sortRank;
		int otherRank = other.sortRank;

		if(thisRank == otherRank)
		{
			thisRank = id;
			otherRank = other.id;
		}

		if(thisRank < otherRank)
			return -1;
		else if(thisRank > otherRank)
			return 1;

		return 0;
	}

	/**
	 * Returns the drug with the specified id (unchecked).
	 *
	 * @param drugId the id to search for.
	 * @return The drug or <code>null</code> if it doesn't exist.
	 */
	public static Drug find(int drugId)
	{
		for(Drug drug : Database.getCached(Drug.class))
		{
			if(drug.getId() == drugId)
				return drug;
		}

		return null;
	}

	/**
	 * Returns the drug with the specified id (checked).
	 *
	 * @param drugId the id to search for.
	 * @throws NoSuchElementException if there is no drug with the specified id.
	 */
	public static Drug get(int drugId)
	{
		Drug drug = find(drugId);
		if(drug == null)
			throw new NoSuchElementException("No drug with id=" + drugId);
		return drug;
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
			this.repeat,
			this.repeatArg,
			this.repeatOrigin,
			this.comment
		};

		return members;
	}

	private boolean hasDoseOnWeekday(int calWeekday)
	{
		if(repeat != REPEAT_WEEKDAYS)
			throw new IllegalStateException("repeat != FREQ_WEEKDAYS");

		// first, translate Calendar's weekday representation to our
		// own.

		int weekday = CollectionUtils.indexOf(calWeekday, Constants.WEEK_DAYS);
		if(weekday == -1)
			throw new IllegalArgumentException("Argument " + calWeekday + " does not map to a valid weekday");

		return (repeatArg & 1 << weekday) != 0;
	}

	static final Runnable HOOK_DELETE = new Runnable() {

		@Override
		public void run()
		{
			for(Intake intake : Database.getAll(Intake.class))
			{
				if(intake.getDrug() == null)
				{
					Log.d(TAG, "Deleting intake for drug id " + intake.getDrugId());
					Database.delete(intake, Database.FLAG_DONT_NOTIFY_LISTENERS);
				}
			}
		}
	};
}
