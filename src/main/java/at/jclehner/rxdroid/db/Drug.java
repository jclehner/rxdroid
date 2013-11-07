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

package at.jclehner.rxdroid.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import android.util.Log;
import at.jclehner.androidutils.LazyValue;
import at.jclehner.rxdroid.BuildConfig;
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
	@SuppressWarnings("unused")
	private static final boolean LOGV = BuildConfig.DEBUG;

	public static final int ICON_TABLET = 0;
	public static final int ICON_SYRINGE = 1;
	public static final int ICON_GLASS = 2;
	public static final int ICON_TUBE = 3;
	public static final int ICON_RING = 4;
	public static final int ICON_CAPSULE = 5;
	public static final int ICON_INHALER = 6;
	public static final int ICON_AMPOULE = 7;
	public static final int ICON_IV_BAG = 8;
	public static final int ICON_PIPETTE = 9;
	public static final int ICON_SPRAY = 10;
	public static final int ICON_OTHER = 11;

	@DatabaseField(unique = true)
	private String name;

	@DatabaseField(foreign = true)
	private transient Patient patient;

	@DatabaseField
	private int icon;

	@DatabaseField
	private boolean active = true;

	// if mRefillSize == 0, mCurrentSupply should be ignored
	@DatabaseField
	private int refillSize;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction currentSupply = Fraction.ZERO;

	@DatabaseField(columnName = "autoAddIntakes")
	private boolean hasAutoDoseEvents = false;

	@DatabaseField(columnName = "lastAutoIntakeCreationDate")
	private Date lastAutoDoseEventCreationDate;

	@DatabaseField
	private Date lastScheduleUpdateDate;

	@DatabaseField
	private int sortRank = Integer.MAX_VALUE;

	@ForeignCollectionField(eager = true, maxEagerLevel = 2)
	private ForeignCollection<Schedule> foreignSchedules;

	@DatabaseField
	private String comment;

	private transient Fraction[] mSimpleSchedule;

	/**
	 * Default constructor, required by ORMLite.
	 */
	public Drug() {}

	public boolean hasDoseOnDate(Date date) {
		return Schedules.hasDoseOnDate(date, mSchedules.get());
	}

	public String getName() {
		return name;
	}

	public int getIcon() {
		return icon;
	}

	public void setAutoAddIntakesEnabled(boolean autoAddIntakes)
	{
		if(this.hasAutoDoseEvents == autoAddIntakes)
			return;

		this.hasAutoDoseEvents = autoAddIntakes;

		if(autoAddIntakes)
		{
			if(lastAutoDoseEventCreationDate == null)
				lastAutoDoseEventCreationDate = DateTime.yesterday();
		}
		else
			lastAutoDoseEventCreationDate = null;
	}

	public boolean hasAutoDoseEvents() {
		return hasAutoDoseEvents;
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

	public Fraction getDose(int doseTime, Date date) {
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
			throw new IllegalArgumentException(currentSupply.toString());

		this.currentSupply = currentSupply;
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

	/**
	 * Adds the specified schedule to this drug.
	 * <p>
	 * WARNING: After calling this function you *must* pass
	 * the Schedule object to Database#update(Entry), to set
	 * the owner.
	 * </p>
	 *
	 *
	 * @param schedule
	 */
	public void addSchedule(Schedule schedule)
	{
		schedule.owner = this;
		mSchedules.get().add(schedule);
	}

	/**
	 * Set a drug's schedules.
	 * <p>
	 * WARNING: After calling this function you *must* pass
	 * the Schedule object to Database#update(Entry), to set
	 * the owner.
	 * </p>
	 *
	 *
	 * @param schedules
	 */
	public void setSchedules(List<Schedule> schedules)
	{
		for(Schedule schedule : schedules)
			schedule.owner = this;

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

	public Date getLastAutoDoseEventCreationDate() {
		return lastAutoDoseEventCreationDate;
	}

	public void setLastAutoDoseEventCreationDate(Date lastAutoDoseEventCreationDate) {
		this.lastAutoDoseEventCreationDate = lastAutoDoseEventCreationDate;
	}

	public Date getLastScheduleUpdateDate() {
		return lastScheduleUpdateDate;
	}

	public boolean hasNoDoses() {
		return Schedules.hasNoDoses(mSchedules.get());
	}

	public Schedule getSchedule(Date date) {
		return Schedules.getActiveSchedule(mSchedules.get(), date);
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
			this.currentSupply,
			this.refillSize,
			this.hasAutoDoseEvents,
			this.lastAutoDoseEventCreationDate,
			this.comment
		};

		return members;
	}

	private final transient LazyValue<List<Schedule>> mSchedules = new LazyValue<List<Schedule>>() {

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
			for(DoseEvent intake : Database.getAll(DoseEvent.class))
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
