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


import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import android.content.Context;
import android.util.Log;
import at.caspase.rxdroid.GlobalContext;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Intake;

import com.j256.ormlite.dao.Dao;

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
	
	private static DatabaseHelper mHelper;
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
			mHelper = new DatabaseHelper(context);
			
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
	 * @see #OnDatabaseChangedListener
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
	
	public static synchronized List<Integer> getOpenIntakeDoseTimes(Drug drug, Calendar date)
	{
		final List<Integer> openIntakeDoseTimes = Arrays.asList(Constants.DOSE_TIMES);
		
		for(int doseTime = Drug.TIME_MORNING; doseTime != Drug.TIME_INVALID; ++doseTime)
		{
			if(!findIntakes(drug, date, doseTime).isEmpty())
				openIntakeDoseTimes.remove(doseTime);			
		}
		
		Log.d(TAG, "openIntakeDoseTimes=" + Arrays.toString(openIntakeDoseTimes.toArray()));
		return openIntakeDoseTimes;		
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
