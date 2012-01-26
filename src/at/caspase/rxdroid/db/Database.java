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


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import at.caspase.rxdroid.GlobalContext;
import at.caspase.rxdroid.db.DatabaseHelper.DatabaseError;
import at.caspase.rxdroid.util.Reflect;

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
	private static final String TAG = Database.class.getName();
	private static final boolean LOGV = true;

	public static final int FLAG_DONT_NOTIFY_LISTENERS = 1;

	private static final HashMap<Class<?>, List<? extends Entry>> sCache =
			new HashMap<Class<?>, List<? extends Entry>>();

	private static DatabaseHelper sHelper;
	private static boolean sIsLoaded = false;

	private static Map<OnChangedListener, Void> sOnChangedListeners =
			new WeakHashMap<OnChangedListener, Void>();

	/**
	 * Initializes the DB.
	 * <p>
	 * This function uses the Context provided by GlobalContext
	 *
	 * @throws IllegalArgumentException if GlobalContext was not initialized.
	 */
	public static void init() {
		init(GlobalContext.get());
	}

	/**
	 * Initializes the DB.
	 *
	 * @param context an android Context for creating the ORMLite database helper.
	 */
	public static synchronized void init(Context context)
	{
		if(context == null)
			throw new NullPointerException("Argument 'context' must not be null. Did you call GlobalContext.set() ?");

		if(!sIsLoaded)
		{
			try
			{
				sHelper = new DatabaseHelper(context);

				// precache entries
				getCached(Drug.class);
				getCached(Intake.class);

				sIsLoaded = true;
			}
			catch(SQLiteException e)
			{
				throw new DatabaseError(DatabaseError.E_GENERAL, e);
			}
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
	public static synchronized void registerOnChangedListener(OnChangedListener listener)
	{
		sOnChangedListeners.put(listener, null);
		//if(LOGV) Log.v(TAG, "register: Objects in registry: " + sOnChangedListeners.size());
	}

	/**
	 * Removes a listener from the registry.
	 *
	 * @see #Database.OnDatabaseChangedListener
	 * @param listener The listener to remove.
	 */
	public static synchronized void unregisterOnChangedListener(OnChangedListener listener)
	{
		sOnChangedListeners.remove(listener);
		if(LOGV) Log.v(TAG, "unregister: Objects in registry: " + sOnChangedListeners.size());
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

	public static <T extends Entry> List<T> getAll(Class<T> clazz) {
		return new LinkedList<T>(getCached(clazz));
	}

	static synchronized <T extends Entry> List<T> getCached(Class<T> clazz)
	{
		if(!sCache.containsKey(clazz))
		{
			if(!sIsLoaded)
			{
				final List<T> entries = queryForAll(clazz);
				sCache.put(clazz, entries);
				if(LOGV) Log.v(TAG, "Cached " + entries.size() + " entries of type " + clazz.getSimpleName());
			}
			else
				throw new NoSuchElementException(clazz.getSimpleName());
		}

		@SuppressWarnings("unchecked")
		List<T> cached = (List<T>) sCache.get(clazz);
		return cached;
	}


	private static synchronized <T> Dao<T, Integer> getDaoChecked(Class<T> clazz)
	{
		try
		{
			return sHelper.getDao(clazz);
		}
		catch(SQLException e)
		{
			throw new RuntimeException("Error getting DAO for " + clazz.getSimpleName());
		}
	}

	private static <E extends Entry> void performDbOperation(String methodName, E entry, int flags)
	{
		@SuppressWarnings("unchecked")
		final Class<E> clazz = (Class<E>) entry.getClass();
		final List<E> cached = getCached(clazz);

		if("create".equals(methodName))
			cached.add(entry);
		else if("delete".equals(methodName))
			cached.remove(entry);
		else if("update".equals(methodName))
		{
			final Entry oldEntry = Entry.findInCollection(cached, entry.getId());
			int index = cached.indexOf(oldEntry);

			cached.remove(index);
			cached.add(index, entry);
		}
		else
			throw new IllegalArgumentException("methodName=" + methodName);

		final Dao<E, Integer> dao = getDaoChecked(clazz);
		runDaoMethodInThread(dao, methodName, entry);

		final String hookName = "HOOK_" + methodName.toUpperCase(Locale.US);
		final Field hookField = Reflect.getDeclaredField(clazz, hookName);
		if(hookField != null)
		{
			Runnable hook = null;

			try
			{
				hook = (Runnable) hookField.get(null);
			}
			catch(IllegalArgumentException e)
			{
				Log.w(TAG, hookName, e);
			}
			catch(IllegalAccessException e)
			{
				Log.w(TAG, hookName, e);
			}
			catch(ClassCastException e)
			{
				Log.w(TAG, hookName, e);
			}

			if(hook != null)
			{
				// don't run this in a thread as we want a clean state when events are
				// dispatched to listeners
				hook.run();
				if(LOGV) Log.v(TAG, "Ran hook " + hookField.getName());
			}
		}

		if((flags & FLAG_DONT_NOTIFY_LISTENERS) == 0)
		{
			final char first = Character.toUpperCase(methodName.charAt(0));
			final String eventName = "onEntry" + first + methodName.substring(1) + "d";
			dispatchEventToListeners(eventName, entry, 0);
		}
	}

	private static <E extends Entry, ID> void runDaoMethodInThread(final Dao<E, ID> dao, final String methodName, final E entry)
	{
		final Thread th = new Thread(new Runnable() {

			@Override
			public void run()
			{
				Exception ex;

				try
				{
					final Method m = dao.getClass().getMethod(methodName, Object.class);
					m.invoke(dao, entry);
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

				throw new RuntimeException("Failed to run DAO method " + methodName, ex);
			}
		});

		th.start();
	}

	private static<T> List<T> queryForAll(Class<T> clazz)
	{
		try
		{
			return getDaoChecked(clazz).queryForAll();
		}
		catch(SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static synchronized void dispatchEventToListeners(String functionName, Entry entry, int flags)
	{
		if((flags & FLAG_DONT_NOTIFY_LISTENERS) != 0)
			return;

		final Set<OnChangedListener> listeners =
				Collections.synchronizedSet(sOnChangedListeners.keySet());

		synchronized(listeners)
		{
			Iterator<OnChangedListener> i = listeners.iterator();

			while(i.hasNext())
			{
				Exception ex;
				OnChangedListener l = null;

				try
				{
					l = i.next();
					Method m = l.getClass().getMethod(functionName, Entry.class, Integer.TYPE);
					m.invoke(l, entry, flags);

					if(LOGV) Log.v(TAG, "dispatchEventToListeners: success for " + l);

					continue;
				}
				catch(SecurityException e)
				{
					ex = e;
				}
				catch(NoSuchMethodException e)
				{
					ex = e;
				}
				catch(IllegalArgumentException e)
				{
					ex = e;
				}
				catch(IllegalAccessException e)
				{
					ex = e;
				}
				catch(InvocationTargetException e)
				{
					ex = e;
				}
				catch(ConcurrentModificationException e)
				{
					ex = e;
				}

				Log.e(TAG, "Failed to dispatch event " + functionName + " to listener " + l, ex);
				break;
			}
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
	 * @see Database#create(Entry)
	 * @see Database#update(Entry)
	 * @see Database#delete(Entry)
	 *
	 * @author Joseph Lehner
	 *
	 */
	public interface OnChangedListener
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

	private Database() {}
}
