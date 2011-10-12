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

package at.caspase.rxdroid;


import java.io.Serializable;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Hasher;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

/**
 * All DB access goes here.
 * <p>
 * Even though DB access is handled by ORMLite, it should not be neccessary to deal with the library
 * directly outside this class. For this to work, {@link #load(Context)} has to be called before using any
 * other function. If using {@link #load()}, you must ensure that the {@link #GlobalContext} has been
 * initialized. 
 * </p>
 * <p>
 * Note that all ORMLite related classes will have members prefixed without the
 * usual "m" (i.e. "comment" instead of "mComment").
 *
 * @author Joseph Lehner
 *
 */
public final class Database
{
	private static final String TAG = Database.class.getName();

	private static List<Drug> sDrugCache;
	private static List<Intake> sIntakeCache;
	
	private static Helper mHelper;
	private static Dao<Drug, Integer> mDrugDao;
	private static Dao<Intake, Integer> mIntakeDao;
	
	private static boolean sIsLoaded = false;
	
	private static WeakHashMap<OnDatabaseChangedListener, Void> sOnChangedListeners = 
		new WeakHashMap<OnDatabaseChangedListener, Void>();

	/**
	 * Initializes the DB.
	 * <p>
	 * This function uses the Context provided by GlobalContext
	 * 
	 * @throws NullPointerException if GlobalContext was not initialized.
	 */
	public static synchronized void load() {
		load(GlobalContext.get());
	}
	
	/**
	 * Initializes the DB.
	 * 
	 * @param context an android Context for creating the ORMLite database helper.
	 */
	public static synchronized void load(Context context)
	{
		if(context == null)
			throw new IllegalArgumentException("Argument 'context' must not be null. Did you call GlobalContext.set() ?");
		
		if(!sIsLoaded)
		{
			mHelper = new Helper(context);
			
			mDrugDao = mHelper.getDrugDao();
			mIntakeDao = mHelper.getIntakeDao();
			
			getCachedDrugs();
			getCachedIntakes();
			
			sIsLoaded = true;
		}
	}
	
	/**
	 * Add a listener to the registry.
	 *
	 * Whenever the methods create(), update(), or delete() are used, all
	 * objects that were registered using this method will have their
	 * callback functions called accordingly.
	 *
	 * @see #Database.OnDatabaseChangedListener
	 * @param listener The listener to register.
	 */
	public static synchronized void registerOnChangedListener(OnDatabaseChangedListener listener) 
	{
		sOnChangedListeners.put(listener, null);
		//Log.d(TAG, "register: Objects in listener registry: " + sOnChangedListeners.size());
	}

	/**
	 * Removes a listener from the registry.
	 *
	 * @see #Database.OnDatabaseChangedListener
	 * @param listener The listener to remove.
	 */
	public static synchronized void unregisterOnChangedListener(OnDatabaseChangedListener listener) 
	{
		sOnChangedListeners.remove(listener);
		//Log.d(TAG, "unregister: Objects in listener registry: " + sOnChangedListeners.size());
	}

	/**
	 * Creates a new database entry and notifies listeners.
	 */
	public static <T extends Entry, ID> void create(final T t, int flags)
	{
		if(t instanceof Drug)
			create(mDrugDao, (Drug) t, flags);
		else if(t instanceof Intake)
			create(mIntakeDao, (Intake) t, flags);
	}
	
	/**
	 * Creates a new database entry and notifies listeners.
	 */
	public static <T extends Entry, ID> void create(final T t) {
		create(t, 0);
	}
	
	/**
	 * Updates a database entry and notifies listeners.
	 */	
	public static <T extends Entry, ID> void update(final T t, int flags)
	{
		if(t instanceof Drug)
			update(mDrugDao, (Drug) t, flags);
		else if(t instanceof Intake)
			update(mIntakeDao, (Intake) t, flags);
	}
	
	/**
	 * Updates a database entry and notifies listeners.
	 */	
	public static <T extends Entry, ID> void update(final T t) {
		update(t, 0);
	}
	
	/**
	 * Deletes a database entry and notifies listeners.
	 */	
	public static <T extends Entry, ID> void delete(final T t, int flags)
	{
		if(t instanceof Drug)
			delete(mDrugDao, (Drug) t, flags);
		else if(t instanceof Intake)
			delete(mIntakeDao, (Intake) t, flags);
	}
	
	/**
	 * Deletes a database entry and notifies listeners.
	 */	
	public static <T extends Entry, ID> void delete(final T t) {
		delete(t, 0);
	}

	/**
	 * Finds a drug with the specified id.
	 * 
	 * @param drugId the id to search for.
	 * @throws NoSuchElementException if there is no drug with the specified id.
	 */
	public static Drug findDrug(int drugId)
	{
		for(Drug drug : getCachedDrugs())
		{
			if(drug.getId() == drugId)
				return drug;
		}
		
		throw new NoSuchElementException("No drug with id=" + drugId);
	}
	
	/**
	 * Find all intakes meeting the specified criteria.
	 */
	public static synchronized List<Intake> findIntakes(Drug drug, Calendar date, int doseTime)
	{
		final List<Intake> intakes = new LinkedList<Intake>();
				
		for(Intake intake : getCachedIntakes())
		{
			if(intake.getDoseTime() != doseTime)
				continue;
			if(intake.getDrugId() != drug.getId())
				continue;
			if(!intake.getDate().equals(DateTime.toSqlDate(date)))
				continue;
			
			intakes.add(intake);
		}
		
		return intakes;	
	}
	
	/**
	 * Gets the cached drugs.
	 * @return a reference to the <em>actual</em> cache. Use {@link #getDrugs()} to get a copy of the cache.
	 */
	public static synchronized List<Drug> getCachedDrugs() 
	{
		if(sDrugCache == null)
		{
			sDrugCache = Collections.synchronizedList(queryForAll(mDrugDao));
			Log.d(TAG, "Loaded " + sDrugCache.size() + " drugs into cache");
		}
		return sDrugCache;
	}
	
	/**
	 * Gets a copy of the cached drugs.
	 * @return a copy of the current cache.
	 */
	public static List<Drug> getDrugs() {
		return new LinkedList<Drug>(getCachedDrugs());
	}
	
	/**
	 * Gets the cached intakes.
	 * @return a reference to the <em>actual</em> cache. Use {@link #getIntakes()} to get a copy of the cache.
	 */
	public static synchronized List<Intake> getCachedIntakes() 
	{
		if(sIntakeCache == null)
		{
			sIntakeCache = Collections.synchronizedList(queryForAll(mIntakeDao));
			Log.d(TAG, "Loaded " + sIntakeCache.size() + " intakes into cache");
		}
		return sIntakeCache;
	}	

	/**
	 * Gets a copy of the cached intakes.
	 * @return a copy of the current cache.
	 */
	public static List<Intake> getIntakes() {
		return new LinkedList<Intake>(getCachedIntakes());
	}	
	
	private static synchronized <T extends Entry, ID> void create(final Dao<T, ID> dao, final T t, int flags)
	{
		Thread th = new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					dao.create(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}
			}
		});

		th.start();
		
		if(t instanceof Drug)
		{		
			final List<Drug> drugCache = getCachedDrugs();
			drugCache.add((Drug) t);
		}
		else if(t instanceof Intake)
		{
			final List<Intake> intakeCache = getCachedIntakes();
			intakeCache.add((Intake) t);
			
		}
		
		for(OnDatabaseChangedListener l : sOnChangedListeners.keySet())
			l.onEntryCreated(t, flags);
	}	

	private static synchronized <T extends Entry, ID> void update(final Dao<T, ID> dao, final T t, int flags)
	{
		Thread th = new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					dao.update(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}
			}
		});

		th.start();

		Entry newEntry;
		
		if(t instanceof Drug)
		{
			final List<Drug> drugCache = getCachedDrugs();
			final Drug newDrug = (Drug) t;			
			final Drug oldDrug = Entry.findInCollection(drugCache, newDrug.getId());
			int index = drugCache.indexOf(oldDrug);
			
			drugCache.remove(index);
			drugCache.add(index, newDrug);
			
			newEntry = newDrug;			
		}
		else
			throw new UnsupportedOperationException();
		
		for(OnDatabaseChangedListener l : sOnChangedListeners.keySet())
			l.onEntryUpdated(newEntry, flags);
	}
	
	private static synchronized <T extends Entry, ID> void delete(final Dao<T, ID> dao, final T t, int flags)
	{
		Thread th = new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					dao.delete(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}
			}
		});

		th.start();

		if(t instanceof Drug)
		{		
			final List<Drug> drugCache = getCachedDrugs();
			drugCache.remove((Drug) t);				
			// FIXME remove intakes
		}
		else if(t instanceof Intake)
		{			
			final List<Intake> intakeCache = getCachedIntakes();
			intakeCache.remove((Intake) t);
		}
		
		for(OnDatabaseChangedListener l : sOnChangedListeners.keySet())
			l.onEntryDeleted(t, flags);	
	}
	
	private static<T> List<T> queryForAll(Dao<T, Integer> dao)
	{
		try
		{
			return dao.queryForAll();
		}
		catch(SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Database() {}

	/**
	 * Base class for all database entries.
	 *
	 * The main purpose of this class is to provide alleviate child classes from
	 * declaring an ID field and to provide an unimplemented equals() method.
	 *
	 * @author Joseph Lehner
	 *
	 */
	public static abstract class Entry implements Serializable
	{
		private static final long serialVersionUID = 8300191193261799857L;

		public static final String COLUMN_ID = "id";

		@DatabaseField(columnName = COLUMN_ID, generatedId = true)
		protected int id;

		@Override
		public abstract boolean equals(Object other);

		@Override
		public abstract int hashCode();

		public int getId() {
			return id;
		}
		
		public static<T extends Entry> T findInCollection(Collection<T> collection, int id)
		{
			for(T t : collection)
			{
				if(t.getId() == id)
					return t;
			}
			
			return null;
		}
	}

	/**
	 * Class for handling the drug database.
	 *
	 * The word "dose" in the context of this documentation refers to
	 * the smallest available dose of that drug without having to
	 * manually reduce its amount (i.e. no pill-splitting). For example,
	 * a package of Aspirin containing 30 tablets contains 30 doses; of
	 * course, the intake schedule may also contain doses in fractions.
	 *
	 * Another term you'll come across in the docs and the code is the
	 * concept of a 'dose-time'. A dose-time is a user-definable subdivision
	 * of the day, having one of the following predefined names: morning,
	 * noon, evening, night.
	 *
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
	public static class Drug extends Entry
	{
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

		@DatabaseField(dataType = DataType.SERIALIZABLE)
		private Fraction doseWholeDay = new Fraction();

		@DatabaseField(canBeNull = true, useGetSet = true)
		private int frequency = FREQ_DAILY;

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
		private long frequencyArg = 0;

		@DatabaseField(canBeNull = true)
		private String comment;

		public Drug() {}

		public boolean hasDoseOnDate(Calendar cal)
		{
			if(frequency == FREQ_DAILY)
				return true;

			if(frequency == FREQ_EVERY_OTHER_DAY)
			{
				final long diffDays = Math.abs(frequencyArg - cal.getTimeInMillis()) / Constants.MILLIS_PER_DAY;
				return diffDays % 2 == 0;
			}
			else if(frequency == FREQ_WEEKLY)
				return cal.get(Calendar.DAY_OF_WEEK) == frequencyArg;
			
			throw new AssertionError("WTF");
		}

		public String getName()
		{
			if(name == null)
				Log.w(TAG, "getName: name == null");

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
					return R.drawable.med_syringe;

				case FORM_DROP:
					return R.drawable.med_drink;

				case FORM_TABLET:
					// fall through

				default:
					return R.drawable.med_pill;

				// FIXME
			}
		}

		public int getFrequency() {
			return frequency;
		}

		public long getFrequencyArg() {
			return frequencyArg;
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

		public Fraction[] getSchedule() {
			return new Fraction[] { doseMorning, doseNoon, doseEvening, doseNight, doseWholeDay };
		}

		public Fraction getDose(int doseTime)
		{
			final Fraction doses[] = {
					doseMorning,
					doseNoon,
					doseEvening,
					doseNight,
					doseWholeDay
			};

			return doses[doseTime];
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

		public void setFrequency(int frequency)
		{
			if(frequency > FREQ_WEEKLY)
				throw new IllegalArgumentException();
			this.frequency = frequency;
		}

		public void setFrequencyArg(long frequencyArg) {
			this.frequencyArg = frequencyArg;
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
		}

		public void setComment(String comment) {
			this.comment = comment;
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
	public static class Intake extends Entry
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
		
		public int getDrugId() {
			return drug.getId();
		}
		
		public Drug getDrug() {
			return findDrug(getDrugId());			
		}
		
		public Fraction getDose() {
			return dose;
		}

		public Date getDate() {
			return new Date(date.getTime());
		}

		public Timestamp getTimestamp() {
			return new Timestamp(timestamp.getTime());
		}

		public int getDoseTime() {
			return doseTime;
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
			return drug.getName() + ": date=" + date + ", doseTime=" + doseTime + ", dose=" + dose;
		}
	}

	/**
	 * Helper class for ORMLite related voodoo.
	 *
	 * @author Joseph Lehner
	 *
	 */
	public static class Helper extends OrmLiteSqliteOpenHelper
	{
		private static final String DB_NAME = "db.sqlite";
		private static final int DB_VERSION = 42;

		private Dao<Database.Drug, Integer> mDrugDao = null;
		private Dao<Database.Intake, Integer> mIntakeDao = null;

		public Helper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db, ConnectionSource cs)
		{
			try
			{
				TableUtils.createTable(cs, Database.Drug.class);
				TableUtils.createTable(cs, Database.Intake.class);
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Error while creating tables", e);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion)
		{
			try
			{
				TableUtils.dropTable(cs, Database.Drug.class, true);
				TableUtils.dropTable(cs, Database.Intake.class, true);
				onCreate(db, cs);
			}
			catch (SQLException e)
			{
				Log.w(TAG, "onUpgrade", e);
			}
		}

		public void dropTables() {
			onUpgrade(getWritableDatabase(), 0, DB_VERSION);
		}

		public synchronized Dao<Database.Drug, Integer> getDrugDao()
		{
			try
			{
				if(mDrugDao == null)
					mDrugDao = getDao(Database.Drug.class);
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Cannot get DAO", e);
			}
			return mDrugDao;
		}

		public synchronized Dao<Database.Intake, Integer> getIntakeDao()
		{
			try
			{
				if(mIntakeDao == null)
					mIntakeDao = getDao(Database.Intake.class);
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Cannot get DAO", e);
			}
			return mIntakeDao;
		}

		@Override
		public void close()
		{
			super.close();
			mDrugDao = null;
			mIntakeDao = null;
		}
	}

	/**
	 * Notifies objects of database changes.
	 * <p>
	 * Objects implementing this interface and registering themselves using
	 * {@link #Database.registerOnChangedListener()} will be notified of 
	 * any changes to the database, as long as the modifications are performed
	 * using the static functions in {@link #Database}.
	 * 
	 * @see Database#create
	 * @see Database#update
	 * @see Database#delete
	 * @see Database#dropDatabase
	 * 
	 * @author Joseph Lehner
	 *
	 */
	public interface OnDatabaseChangedListener
	{
		/**
		 * Pass this to ignore an event.
		 * <p>
		 * Implementations of this interface may ignore an event if this value
		 * is ORed into the <code>flags</code> argument of the callbacks.
		 */
		public static final int FLAG_IGNORE = 1;
		
		/**
		 * Called after an entry has been added to the database.
		 * 
		 * @param entry the entry that has been created.
		 * @param flags for private implementation details.
		 */
		public void onEntryCreated(Entry entry, int flags);
		
		/**
		 * Called after a database entry has been updated.
		 * 
		 * @param entry the new version of the entry.
		 * @param flags for private implementation details.
		 */
		public void onEntryUpdated(Entry entry, int flags);
		
		/**
		 * Called after a database entry has been deleted.
		 * 
		 * @param entry the entry that was just deleted.
		 * @param flags for private implementation details.
		 */
		public void onEntryDeleted(Entry entry, int flags);
	}
	
	public interface Filter<T extends Entry>
	{
		boolean matches(T t);
	}
}
