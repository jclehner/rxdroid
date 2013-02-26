/**
 * RxDroid - A Medication Reminder
 * Copyright 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import at.jclehner.androidutils.LazyValue;
import at.jclehner.androidutils.LazyValue.Mutator;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Hasher;
import at.jclehner.rxdroid.util.Keep;
import at.jclehner.rxdroid.util.Util;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
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
	@SuppressWarnings("unused")
	private static final String TAG = Drug.class.getSimpleName();

	public static final int ICON_TABLET = 0;
	public static final int ICON_SYRINGE = 1;
	public static final int ICON_GLASS = 2;
	public static final int ICON_TUBE = 3;
	public static final int ICON_RING = 4;
	public static final int ICON_SPRAY = 5;
	public static final int ICON_OTHER = 6;

	public static final int TIME_MORNING = 0;
	public static final int TIME_NOON = 1;
	public static final int TIME_EVENING = 2;
	public static final int TIME_NIGHT = 3;
	/**
	 * All dose-times &gt;= this value are considered invalid.
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
	public static final int REPEAT_AS_NEEDED = 3;
	public static final int REPEAT_21_7 = 4; // for oral contraceptives, 21 days on, 7 off
	public static final int REPEAT_CUSTOM = 5;
	public static final int REPEAT_INVALID = 6;

	// TODO valid arguments: 6, 8, 12, with automapping to doseTimes
	public static final int REPEAT_EVERY_N_HOURS = REPEAT_INVALID;

	public static final int REPEATARG_DAY_MON = 1;
	public static final int REPEATARG_DAY_TUE = 1 << 1;
	public static final int REPEATARG_DAY_WED = 1 << 2;
	public static final int REPEATARG_DAY_THU = 1 << 3;
	public static final int REPEATARG_DAY_FRI = 1 << 4;
	public static final int REPEATARG_DAY_SAT = 1 << 5;
	public static final int REPEATARG_DAY_SUN = 1 << 6;

	@DatabaseField(unique = true)
	private String name;

	// XXX
	@DatabaseField(foreign = true)
	private Patient patient;
	// XXX

	@DatabaseField
	private int icon;

	@DatabaseField
	private boolean active = true;

	// if mRefillSize == 0, mCurrentSupply should be ignored
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
	private int repeatMode= REPEAT_DAILY;


	@DatabaseField
	private long repeatArg = 0;

	/**
	 * Defines the repeat origin.
	 *
	 *
	 *
	 * For every repeat other than {@link #REPEAT_DAILY}, this field holds a specific value,
	 * allowing {@link #hasDoseOnDate(Date)} to determine whether a dose is scheduled on a specific date.
	 *
	 * <ul>
	 *     <li><code>FREQ_EVERY_OTHER_DAY</code>: field is set to a date (in milliseconds) where this drug's
	 *         intake should be set, i.e. if the date corresponds to 2011-09-07, there's an intake on that day,
	 *         another one on 2011-09-09, and so forth.</li>
	 *     <li><code>FREQ_WEEKLY</code>: field is set to a week day value from {@link java.util.Calendar}.</li>
	 * </ul>
	 */
	@DatabaseField
	private Date repeatOrigin;

	@DatabaseField
	private boolean autoAddIntakes = false;

	@DatabaseField
	private Date lastAutoIntakeCreationDate;

	@DatabaseField
	private Date lastScheduleUpdateDate;

	@DatabaseField
	private int sortRank = Integer.MAX_VALUE;

	@ForeignCollectionField(eager = true, maxEagerLevel = 2)
	private ForeignCollection<Schedule> foreignSchedules;

	@DatabaseField
	private String comment;

	private Fraction[] mSimpleSchedule;

	/**
	 * Default constructor, required by ORMLite.
	 */
	public Drug() {}

	public boolean hasDoseOnDate(Date date)
	{
		if(repeatOrigin != null)
		{
			switch(repeatMode)
			{
				case REPEAT_EVERY_N_DAYS:
				case REPEAT_EVERY_N_HOURS:
				case REPEAT_21_7:
				{
					if(date.before(repeatOrigin))
						return false;
				}

				default:
					;
			}
		}

		switch(repeatMode)
		{
			case REPEAT_DAILY:
			case REPEAT_AS_NEEDED:
				return true;

			case REPEAT_EVERY_N_DAYS:
				return (DateTime.diffDays(date, repeatOrigin) % repeatArg) == 0;

			case REPEAT_WEEKDAYS:
				final Calendar cal = DateTime.calendarFromDate(date);
				return hasDoseOnWeekday(cal.get(Calendar.DAY_OF_WEEK));

			case REPEAT_21_7:
				return (DateTime.diffDays(date, repeatOrigin) % 28) < 21;

			case REPEAT_CUSTOM:
				return Schedules.hasDoseOnDate(date, mSchedules.get());

			default:
				throw new IllegalStateException("Unknown repeat mode");
		}
	}

	public String getName() {
		return name;
	}

	public int getIcon() {
		return icon;
	}

	/*public int getIconResourceId()
	{
		final boolean isDarkTheme = Theme.isDark();

		switch(icon)
		{
			case ICON_SYRINGE:
				return isDarkTheme ? R.drawable.ic_drug_syringe_light : R.drawable.ic_drug_syringe_dark;

			case ICON_GLASS:
				return isDarkTheme ? R.drawable.ic_drug_glass_light : R.drawable.ic_drug_glass_dark;

			case ICON_TUBE:
				return isDarkTheme ? R.drawable.ic_drug_tube_light : R.drawable.ic_drug_tube_dark;

			case ICON_TABLET:
				// fall through

			default:
				//return R.drawable.ic_drug_pill2;
				return isDarkTheme ? R.drawable.ic_drug_tablet_light : R.drawable.ic_drug_tablet_dark;

			// FIXME
		}
	}*/

	public int getRepeatMode() {
		return repeatMode;
	}

	public long getRepeatArg() {
		return repeatArg;
	}

	public Date getRepeatOrigin() {
		return repeatOrigin;
	}

	public void setAutoAddIntakesEnabled(boolean autoAddIntakes)
	{
		if(this.autoAddIntakes == autoAddIntakes)
			return;

		this.autoAddIntakes = autoAddIntakes;

		if(autoAddIntakes)
		{
			if(lastAutoIntakeCreationDate == null)
				lastAutoIntakeCreationDate = DateTime.yesterday();
		}
		else
			lastAutoIntakeCreationDate = null;
	}

	public boolean isAutoAddIntakesEnabled() {
		return autoAddIntakes;
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

	public Fraction[] getSimpleSchedule()
	{
		if(mSimpleSchedule == null)
			mSimpleSchedule = new Fraction[] { doseMorning, doseNoon, doseEvening, doseNight };

		return mSimpleSchedule;
	}

	public Fraction getDose(int doseTime)
	{
		if(repeatMode == REPEAT_CUSTOM)
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
		if(repeatMode != REPEAT_CUSTOM)
		{
			if(!hasDoseOnDate(date))
				return Fraction.ZERO;

			return getDose(doseTime);
		}

		return Schedules.getDose(date, doseTime, mSchedules.get());
	}

	public String getComment() {
		return comment;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setIcon(int icon)
	{
		if(icon > ICON_OTHER)
			throw new IllegalArgumentException();
		this.icon = icon;
	}

	public void setRepeatMode(int repeatMode)
	{
		if(repeatMode >= REPEAT_INVALID)
			throw new IllegalArgumentException();

		if(repeatMode == this.repeatMode)
			return;

		// the preference was changed, so reset all repeat-related settings
		this.repeatMode = repeatMode;
		this.repeatArg = 0;
		this.repeatOrigin = null;
		onScheduleUpdated();
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
		if(repeatMode == REPEAT_EVERY_N_DAYS)
		{
			if(repeatArg <= 1)
				throw new IllegalArgumentException();
		}
		else if(repeatMode == REPEAT_WEEKDAYS)
		{
			// binary(01111111) = hex(0x7f) (all weekdays)
			if(repeatArg <= 0 || repeatArg > 0x7f)
				throw new IllegalArgumentException();
		}
		else if(repeatMode == REPEAT_EVERY_N_HOURS)
		{
			if(repeatArg != 6 && repeatArg != 8 && repeatArg != 12)
				throw new IllegalArgumentException();
		}
		else
		{
			//throw new UnsupportedOperationException();
			return;
		}

		this.repeatArg = repeatArg;
		onScheduleUpdated();
	}

	/**
	 * Sets the repeat origin.
	 * @param repeatOrigin
	 * @throws UnsupportedOperationException if this instance's repeat mode does not allow a repeat origin to be set.
	 * @throws IllegalArgumentException if the setting is out of bounds for this instance's repeat mode.
	 */
	public void setRepeatOrigin(Date repeatOrigin)
	{
		if(repeatMode != REPEAT_EVERY_N_DAYS && repeatMode != REPEAT_EVERY_N_HOURS && repeatMode != REPEAT_21_7)
			return;

		if(repeatMode != REPEAT_EVERY_N_HOURS && DateTime.getOffsetFromMidnight(repeatOrigin) != 0)
			throw new IllegalArgumentException();

		this.repeatOrigin = repeatOrigin;
		onScheduleUpdated();
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
		else if(currentSupply.isNegative())
			throw new IllegalArgumentException();

		this.currentSupply = currentSupply;
	}

	public void setDose(int doseTime, Fraction value)
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

		onScheduleUpdated();
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

	public void addSchedule(Schedule schedule) {
		mSchedules.get().add(schedule);
	}

	public void setSchedules(List<Schedule> schedules) {
		mSchedules.set(schedules);
	}

	public List<Schedule> getSchedules() {
		return mSchedules.get();
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public Patient getPatient() {
		return Database.find(Patient.class, getPatientId());
	}

	public int getPatientId() {
		return patient != null ? patient.id : Patient.DEFAULT_PATIENT_ID;
	}

	public Date getLastAutoIntakeCreationDate() {
		return lastAutoIntakeCreationDate;
	}

	public void setLastAutoIntakeCreationDate(Date lastAutoIntakeCreationDate) {
		this.lastAutoIntakeCreationDate = lastAutoIntakeCreationDate;
	}

	public Date getLastScheduleUpdateDate() {
		return lastScheduleUpdateDate;
	}

//	public void setLastScheduleUpdateDate(Date lastScheduleUpdateDate) {
//		this.lastScheduleUpdateDate = lastScheduleUpdateDate;
//	}

	public boolean hasNoDoses()
	{
		if(repeatMode == REPEAT_CUSTOM)
			return Schedules.hasNoDoses(mSchedules.get());

		for(Fraction dose : getSimpleSchedule())
		{
			if(!dose.isZero())
				return false;
		}

		return true;
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
			if(!Util.equalsIgnoresNull(thisMembers[i], otherMembers[i]))
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
		return "drug " + name;
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

	private void onScheduleUpdated() {
		lastScheduleUpdateDate = DateTime.today();
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
			this.icon,
			this.active,
			this.sortRank,
			//this.patient,
			this.doseMorning,
			this.doseNoon,
			this.doseEvening,
			this.doseNight,
			this.currentSupply,
			this.refillSize,
			this.autoAddIntakes,
			this.lastAutoIntakeCreationDate,
			//this.lastScheduleUpdateDate,
			this.repeatMode,
			this.repeatArg,
			this.repeatOrigin,
			this.comment
		};

		return members;
	}

	/* package */ boolean hasDoseOnWeekday(int calWeekday)
	{
		if(repeatMode != REPEAT_WEEKDAYS)
			throw new IllegalStateException("repeatMode != FREQ_WEEKDAYS");

		// first, translate Calendar's weekday representation to our own
		final int weekday = CollectionUtils.indexOf(calWeekday, Constants.WEEK_DAYS);
		if(weekday == -1)
			throw new IllegalArgumentException("Argument " + calWeekday + " does not map to a valid weekday");

		return (repeatArg & 1 << weekday) != 0;
	}

	private final LazyValue<List<Schedule>> mSchedules = new LazyValue<List<Schedule>>() {

		@Override
		public List<Schedule> value()
		{
			if(foreignSchedules == null)
				return new ArrayList<Schedule>();

			final Schedule[] array = new Schedule[foreignSchedules.size()];
			return Arrays.asList(foreignSchedules.toArray(array));
		}

	};

	@Keep
	/* package */ static final Callback<Drug> CALLBACK_DELETED = new Callback<Drug>() {

		@Override
		public void call(Drug drug)
		{
			for(Intake intake : Database.getAll(Intake.class))
			{
				if(intake.getDrug() == null || intake.getDrugId() == drug.id)
					Database.delete(intake, Database.FLAG_DONT_NOTIFY_LISTENERS);
			}

			for(Schedule schedule : drug.mSchedules.get())
				Database.delete(schedule, Database.FLAG_DONT_NOTIFY_LISTENERS);

		}
	};

	@Keep
	/* package */ static final Callback<Drug> CALLBACK_ALL_CACHED = new Callback<Drug>() {

		@Override
		public void call(Drug entry)
		{

		}
	};
}
