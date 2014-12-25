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


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import android.content.Context;
import android.util.Log;
import at.jclehner.androidutils.EventDispatcher;
import at.jclehner.androidutils.Extras;
import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.db.DatabaseHelper.DatabaseError;
import at.jclehner.rxdroid.util.Timer;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

import com.j256.ormlite.dao.Dao;

/**
 * All DB access goes here.
 * <p>
 * Even though DB access is handled by ORMLite, it should not be neccessary to deal with the library
 * directly outside this class. For this to work, {@link #init(Context)} has to be called before using any
 * other function. If using {@link #init()}, you must ensure that the {@link #GlobalContext} has been
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
	private static final String TAG = Database.class.getSimpleName();
	private static final boolean LOGV = true;

	public static Object LOCK_DATA = new Object();

	/* package */ static final boolean USE_CUSTOM_CACHE = true;

	static final Class<?>[] CLASSES = {
		Drug.class,
		DoseEvent.class,
		Patient.class
	};

	static final int ID_VIRTUAL_ENTRY = 0x7fffffff;

	public static final int FLAG_DONT_NOTIFY_LISTENERS = 1;

	public static final int TABLE_COUNT = CLASSES.length;

	private static final HashMap<Class<?>, List<? extends Entry>> sCache =
			new HashMap<Class<?>, List<? extends Entry>>();

	private static final Object LOCK_INIT = new Object();

	//private static Map<Class<?>, List<? extends Entry>> sCacheCopy = null;

	private static DatabaseHelper sHelper;
	private static boolean sIsLoaded = false;

	private static boolean sInMemoryOnly = false;

	private static long sDbLoadingTimeMillis = 0;

	private static volatile int sPendingDaoOperations = 0;

	private static EventDispatcher<Object> sEventMgr =
			new EventDispatcher<Object>();

	/**
	 * Initializes the DB.
	 * <p>
	 * This function uses the Context provided by GlobalContext
	 *
	 * @throws IllegalArgumentException if GlobalContext was not initialized.
	 */
	public static void init() {
		init(RxDroid.getContext());
	}

	/**
	 * Initializes the DB.
	 *
	 * @param context an android Context for creating the ORMLite database helper.
	 */
	public static synchronized void init(Context context)
	{
		synchronized(LOCK_INIT)
		{
			if(!sIsLoaded)
				reload(context);
		}
	}

	public static void setInMemoryOnly(boolean inMemoryOnly)
	{
		if(BuildConfig.DEBUG)
			sInMemoryOnly = inMemoryOnly;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static synchronized void reload(Context context)
	{
		if(context == null)
			throw new NullPointerException();

		synchronized(LOCK_INIT)
		{
			sIsLoaded = false;
			sCache.clear();

			if(sHelper != null)
			{
				sHelper.close();
				sHelper = null;
			}

			sHelper = new DatabaseHelper(context);
			sDbLoadingTimeMillis = 0;

			if(USE_CUSTOM_CACHE)
			{
				// precache entries
				for(Class clazz : CLASSES)
					getCached(clazz);
			}

			sIsLoaded = true;
			sEventMgr.post("onDatabaseInitialized");
		}
	}

	public static DatabaseHelper getHelper()
	{
		if(!sIsLoaded)
			throw new RuntimeException("Database is not yet initialized");

		return sHelper;
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
	public static synchronized void registerEventListener(OnChangeListener listener) {
		sEventMgr.register(listener);
	}

	/**
	 * Removes a listener from the registry.
	 *
	 * @see #Database.OnDatabaseChangedListener
	 * @param listener The listener to remove.
	 */
	public static synchronized void unregisterEventListener(OnChangeListener listener) {
		sEventMgr.unregister(listener);
	}

	public static synchronized void registerOnInitializedListener(OnInitializedListener l) {
		sEventMgr.register(l);
	}

	/**
	 * Creates a new database entry and notifies listeners.
	 */
	public static <E extends Entry> void create(E entry, int flags) {
		performDbOperation("create", entry, flags);
	}

	/**
	 * Creates a new database entry and notifies listeners.
	 */
	public static <E extends Entry> void create(E entry) {
		create(entry, 0);
	}

	/**
	 * Updates an existing database entry and notifies listeners.
	 */
	public static <E extends Entry> void update(E entry, int flags) {
		performDbOperation("update", entry, flags);
	}

	/**
	 * Updates an existing database entry and notifies listeners.
	 */
	public static <E extends Entry> void update(E entry) {
		update(entry, 0);
	}

	/**
	 * Deletes an existing database entry and notifies listeners.
	 */
	public static <E extends Entry> void delete(E entry, int flags) {
		performDbOperation("delete", entry, flags);
	}

	/**
	 * Deletes an existing database entry and notifies listeners.
	 */
	public static <E extends Entry> void delete(E entry) {
		delete(entry, 0);
	}

	public static <E extends Entry> void deleteByIds(Class<? extends Entry> clazz, Collection<Integer> ids)
	{
		final Dao<? extends Entry, Integer> dao = getDaoChecked(clazz);

		try
		{
			dao.deleteIds(ids);
		}
		catch(SQLException e)
		{
			throw new WrappedCheckedException(e);
		}
	}

	public static <T extends Entry> T find(Class<T> clazz, int id)
	{
		if(USE_CUSTOM_CACHE)
			return Entries.findInCollectionById(getCached(clazz), id);
		else
		{
			try
			{
				return getDaoChecked(clazz).queryForId(id);
			}
			catch(SQLException e)
			{
				throw new WrappedCheckedException(e);
			}
		}
	}

	public static <T extends Entry> T get(Class<T> clazz, int id)
	{
		final T t = find(clazz, id);
		if(t == null)
			throw new NoSuchElementException();

		return t;
	}

	public static <T extends Entry> boolean exists(Class<T> clazz, int id) {
		return find(clazz, id) != null;
	}


	public static <T extends Entry> List<T> getAll(Class<T> clazz)
	{
		if(USE_CUSTOM_CACHE)
			return new LinkedList<T>(getCached(clazz));
		else
			return queryForAll(clazz);
	}

	public static <T extends Entry> int countAll(Class<T> clazz)
	{
		if(USE_CUSTOM_CACHE)
			return getCached(clazz).size();
		else
			return getDaoChecked(clazz).getObjectCache().size(clazz);
	}

	public static boolean hasPendingOperations() {
		return sPendingDaoOperations > 0;
	}

	public static long getLoadingTimeMillis() {
		return sDbLoadingTimeMillis;
	}

	static synchronized <T extends Entry> List<T> getCached(Class<T> clazz)
	{
		if(!USE_CUSTOM_CACHE)
		{
			//Log.w(TAG, "getCached called", new )
			//throw new UnsupportedOperationException();
			return queryForAll(clazz);
		}

		if(!sCache.containsKey(clazz))
		{
			if(!sIsLoaded)
			{
				final Timer timer = new Timer();
				final List<T> entries = queryForAll(clazz);
				sCache.put(clazz, entries);

//				if(LOGV)
//				{
//					for(T t : entries)
//						Log.v(TAG, "  " + t);
//				}

				sDbLoadingTimeMillis += timer.elapsed();

				Log.i(TAG, "Cached " + entries.size() + " entries of type " + clazz.getSimpleName() + ": " + timer);
			}
			else
				throw new NoSuchElementException(clazz.getSimpleName());
		}

		@SuppressWarnings("unchecked")
		List<T> cached = (List<T>) sCache.get(clazz);
		return cached;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static <E extends Entry> void createWithoutMagic(E entry) throws SQLException
	{
		final Dao<E, Integer> dao = (Dao<E, Integer>) getDaoChecked(entry.getClass());
		dao.create(entry);
	}

	private static <T> Dao<T, Integer> getDaoChecked(Class<T> clazz) {
		return sHelper.getDaoChecked(clazz);
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static void createOrUpdateWithoutMagic(Object entry)
	{
		try
		{
			@SuppressWarnings("rawtypes")
			final Dao dao = sHelper.getDao(entry.getClass());
			dao.createOrUpdate(entry);
		}
		catch (SQLException e)
		{
			throw new WrappedCheckedException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Entry> void performDbOperation(String methodName, E entry, int flags)
	{
		if(entry.id == ID_VIRTUAL_ENTRY)
			throw new IllegalArgumentException("Cannot perform database operation on virtual entries");

		// Extras might be invalid after the entry has changed
		Extras.remove(entry);

		final Class<E> clazz = (Class<E>) entry.getClass();
		final List<E> cached = getCached(clazz);

		if(USE_CUSTOM_CACHE)
		{
			if("create".equals(methodName))
				cached.add(entry);
			else if("delete".equals(methodName))
				cached.remove(entry);
			else if("update".equals(methodName))
			{
				final Entry oldEntry = Entries.findInCollectionById(cached, entry.getId());
				int index = cached.indexOf(oldEntry);

				cached.remove(index);
				cached.add(index, entry);
			}
			else
				throw new IllegalArgumentException("methodName=" + methodName);
		}

		final Dao<E, Integer> dao = getDaoChecked(clazz);
		runDaoMethodInThread(dao, methodName, entry);

		final String callbackName = "on" + Util.capitalize(methodName);
		final Method m = Reflect.getMethod(clazz, callbackName, clazz);
		if(m != null)
		{
			Reflect.invokeMethod(m, null, entry);
			if(LOGV) Log.v(TAG, "Ran callback " + callbackName);
		}
		else if(LOGV)
			Log.v(TAG, "No callback " + callbackName + " for " + clazz.getSimpleName());

		if((flags & FLAG_DONT_NOTIFY_LISTENERS) == 0)
		{
			final char first = Character.toUpperCase(methodName.charAt(0));
			final String eventName = "onEntry" + first + methodName.substring(1) + "d";
			dispatchEventToListeners(eventName, entry, 0);
		}
	}

	private static <E extends Entry, ID> void runDaoMethodInThread(final Dao<E, ID> dao, final String methodName, final E entry)
	{
		if(sInMemoryOnly)
			return;

		++sPendingDaoOperations;

		final Thread th = new Thread() {

			@Override
			public void run()
			{
				try
				{
					runDaoMethod(dao, methodName, entry);
				}
				finally
				{
					--sPendingDaoOperations;
				}
			}
		};

		th.start();
	}

	private static <E extends Entry, ID> void runDaoMethod(final Dao<E, ID> dao, final String methodName, final E entry)
	{
		Exception ex;

		try
		{
			final Method m = dao.getClass().getMethod(methodName, Object.class);
			final Timer t = LOGV ? new Timer() : null;

			synchronized(LOCK_DATA)
			{
				if(!sHelper.isOpen())
				{
					Log.w(TAG, "Database was not open; reopening!");
					reload(RxDroid.getContext());
				}

				m.invoke(dao, entry);
			}

			RxDroid.notifyBackupDataChanged();

			if(LOGV) Log.v(TAG, "runDaoMethod: " + methodName + ": " + t);

			//dao.deleteBuilder().where().idEq(id)

			return;
		}
		catch(IllegalArgumentException e)
		{
			ex = e;
			// handled at end of function
		}
		catch(IllegalAccessException e)
		{
			ex = e;
			// handled at end of function
		}
		catch(InvocationTargetException e)
		{
			ex = e;
			// handled at end of function
		}
		catch(SecurityException e)
		{
			ex = e;
			// handled at end of function
		}
		catch(NoSuchMethodException e)
		{
			ex = e;
			// handled at end of function
		}
		/*finally
		{
			--sPendingDaoOperations;
		}*/

		throw new WrappedCheckedException("Failed to run DAO method " + methodName, ex);
	}

	private static<T> List<T> queryForAll(Class<T> clazz)
	{
		if(sHelper == null)
		{
			Log.w(TAG, "Database not initialized - initializing it now...", new IllegalStateException("Trace"));
			init();
		}

		try
		{
			return sHelper.getDao(clazz).queryForAll();
		}
		catch(SQLException e)
		{
			throw new DatabaseError(DatabaseError.E_GENERAL, e);
		}
	}

	private static synchronized void dispatchEventToListeners(String functionName, Entry entry, int flags)
	{
		if((flags & FLAG_DONT_NOTIFY_LISTENERS) != 0)
			return;

		sEventMgr.post(functionName, EVENT_HANDLER_ARG_TYPES, entry, flags);
	}

	/**
	 * Notifies objects of database changes.
	 * <p>
	 * Objects implementing this interface and registering themselves using
	 * {@link #Database.registerOnChangedListener()} will be notified of
	 * any changes to the database, as long as the modifications are performed
	 * using the static functions in {@link #Database}.
	 *
	 * @see Database#create(Entry)
	 * @see Database#update(Entry)
	 * @see Database#delete(Entry)
	 *
	 * @author Joseph Lehner
	 *
	 */
	public interface OnChangeListener
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

	/**
	 * {@link OnChangeListener} with method stubs.
	 */
	public static class EmptyOnChangeListener implements OnChangeListener
	{
		@Override
		public void onEntryCreated(Entry entry, int flags) {}

		@Override
		public void onEntryUpdated(Entry entry, int flags) {}

		@Override
		public void onEntryDeleted(Entry entry, int flags) {}
	}

	public interface OnInitializedListener
	{
		void onDatabaseInitialized();
	}

	public interface Filter<T extends Entry>
	{
		boolean matches(T t);
	}

	private Database() {}

	private static final Class<?>[] EVENT_HANDLER_ARG_TYPES = { Entry.class, Integer.TYPE };
}
