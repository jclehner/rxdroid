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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import at.caspase.rxdroid.GlobalContext;
import at.caspase.rxdroid.db.DatabaseHelper.DatabaseError;
import at.caspase.rxdroid.util.Reflect;
import at.caspase.rxdroid.util.WrappedCheckedException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

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
	private static final boolean LOGV = false;

	static final Class<?>[] CLASSES = {
		Drug.class,
		Intake.class,
		Schedule.class
	};

	static final int ID_VIRTUAL_ENTRY = 0x7fffffff;

	public static final int FLAG_DONT_NOTIFY_LISTENERS = 1;

	private static final HashMap<Class<?>, List<? extends Entry>> sCache =
			new HashMap<Class<?>, List<? extends Entry>>();

	private static Map<Class<?>, List<? extends Entry>> sCacheCopy = null;

	private static DatabaseHelper sHelper;
	private static boolean sIsLoaded = false;

	private static boolean sPutNewListenersInQueue = false;

	private static Map<OnChangeListener, Void> sOnChangeListeners = new WeakHashMap<OnChangeListener, Void>();
	private static Map<OnChangeListener, Void> sOnChangeListenersQueue = new WeakHashMap<OnChangeListener, Void>();

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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static synchronized void init(Context context)
	{
		if(context == null)
			throw new NullPointerException("Argument 'context' must not be null. Did you call GlobalContext.set() ?");

		if(!sIsLoaded)
		{
			sHelper = new DatabaseHelper(context);

			// precache entries
			for(Class clazz : CLASSES)
				getCached(clazz);

			sIsLoaded = true;
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
	public static synchronized void registerOnChangedListener(OnChangeListener listener)
	{
		if(!sPutNewListenersInQueue)
			sOnChangeListeners.put(listener, null);
		else
		{
			sOnChangeListenersQueue.put(listener, null);
			if(LOGV) Log.v(TAG, "registerOnChangedListener: listener was enqueued");
		}
		//if(LOGV) Log.v(TAG, "register: Objects in registry: " + sOnChangedListeners.size());
	}

	/**
	 * Removes a listener from the registry.
	 *
	 * @see #Database.OnDatabaseChangedListener
	 * @param listener The listener to remove.
	 */
	public static synchronized void unregisterOnChangedListener(OnChangeListener listener)
	{
		sOnChangeListeners.remove(listener);
		if(LOGV) Log.v(TAG, "unregister: Objects in registry: " + sOnChangeListeners.size());
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

	@SuppressWarnings("unchecked")
	public static synchronized <T extends Entry> List<T> getAll(Class<T> clazz)
	{
		if(sCacheCopy == null)
			sCacheCopy = Collections.unmodifiableMap(sCache);

		return (List<T>) sCacheCopy.get(clazz);

		//return new LinkedList<T>(getCached(clazz));
	}

	static synchronized <T extends Entry> List<T> getCached(Class<T> clazz)
	{
		if(!sCache.containsKey(clazz))
		{
			if(!sIsLoaded)
			{
				if(LOGV) Log.v(TAG, "getCached: clazz=" + clazz);

				final List<T> entries = queryForAll(clazz);
				sCache.put(clazz, entries);

				if(LOGV)
				{
					for(T t : entries)
						Log.v(TAG, "  " + t);
				}

				Log.i(TAG, "Cached " + entries.size() + " entries of type " + clazz.getSimpleName());
			}
			else
				throw new NoSuchElementException(clazz.getSimpleName());
		}

		@SuppressWarnings("unchecked")
		List<T> cached = (List<T>) sCache.get(clazz);
		return cached;
	}

	public static void generateJavaSourceForDbUpgrade()
	{
		final File dir = new File(Environment.getExternalStorageDirectory(), "RxDroid");

		Log.i(TAG, "generateJavaSourceForDbUpgrade: dir=" + dir);

		for(Class<?> clazz : CLASSES)
		{
			final File file = new File(dir, "Old" + clazz.getSimpleName() + ".java");

			try
			{
				if(!clazz.isAnnotationPresent(DatabaseTable.class))
					continue;

				String tableName = Reflect.getAnnotationParameter(clazz.getAnnotation(DatabaseTable.class), "tableName");

				FileWriter fs = new FileWriter(file);
				fs.write(
						"package " + Database.class.getPackage().getName() + ".v" + DatabaseHelper.DB_VERSION + ";\n" +
						"\n" +
						"@SuppressWarnings({\"serial\", \"unused\"})\n" +
						"@DatabaseTable(tableName=\"" + tableName + "\")\n" +
						"public class Old" + clazz.getSimpleName() + " extends Entry\n" +
						"{\n" +
						"\tpublic Old" + clazz.getSimpleName() + "() {}\n\n"
				);

				for(Field f : clazz.getDeclaredFields())
				{
					final Class<?> type = f.getType();

					if(f.isAnnotationPresent(DatabaseField.class))
					{
						boolean isSerializable = false;

						if(type == Integer.TYPE)
							;
						else if(type == Long.TYPE)
							;
						else if(type == Boolean.TYPE)
							;
						else if(type == Date.class)
							;
						else if(type == String.class)
							;
						else
							isSerializable = true;

						fs.write("\t@DatabaseField");

						if(isSerializable)
							fs.write("(dataType=DataType.SERIALIZABLE) // TODO verify");

						fs.write("\n\t");

						int m = f.getModifiers();
						if(Modifier.isProtected(m))
							fs.write("protected");
						else if(Modifier.isPublic(m))
							fs.write("public");
						else if(Modifier.isPrivate(m))
							fs.write("private");
						else
							fs.write("/* package */");

						fs.write(" " + type.getSimpleName() + " " + f.getName() + ";\n\n");
					}
				}

				fs.write(
						// convertToCurrentDatabaseFormat()

						"\t@Override\n" +
						"\tprotected Entry convertToCurrentDatabaseFormat()\n" +
						"\t{\n" +
						"\t\t// FIXME stub\n" +
						"\t\treturn null;\n" +
						"\t}\n\n" +

						// equals()

						"\t@Override\n" +
						"\tpublic boolean equals(Object other) {\n" +
						"\t\tthrow new UnsupportedOperationException();\n" +
						"\t}\n\n" +

						// hashCode()

						"\t@Override\n" +
						"\tpublic int hashCode() {\n" +
						"\t\tthrow new UnsupportedOperationException();\n" +
						"\t}\n"
				);

				fs.write("}\n");
				fs.close();
			}
			catch(FileNotFoundException e)
			{
				Log.w(TAG, "generateJavaSourceForDbUpgrade", e);
				break;
			}
			catch(IOException e)
			{
				Log.w(TAG, "generateJavaSourceForDbUpgrade", e);
				break;
			}
		}
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
		if(entry.id == ID_VIRTUAL_ENTRY)
			throw new IllegalArgumentException("Cannot perform database operation on virtual entries");

		@SuppressWarnings("unchecked")
		final Class<E> clazz = (Class<E>) entry.getClass();
		final List<E> cached = getCached(clazz);

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

		final Dao<E, Integer> dao = getDaoChecked(clazz);
		runDaoMethodInThread(dao, methodName, entry);

		final String hookName = "HOOK_" + methodName.toUpperCase(Locale.US);
		final Field hookField = Reflect.getDeclaredField(clazz, hookName);
		if(hookField != null)
		{
			final Runnable hook = (Runnable) Reflect.getFieldValue(hookField, null, null);
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

				throw new WrappedCheckedException("Failed to run DAO method " + methodName, ex);
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
			throw new DatabaseError(DatabaseError.E_GENERAL, e);
		}
	}

	private static synchronized void dispatchEventToListeners(String functionName, Entry entry, int flags)
	{
		if((flags & FLAG_DONT_NOTIFY_LISTENERS) != 0)
			return;

		sPutNewListenersInQueue = true;

		final Set<OnChangeListener> listeners = Collections.synchronizedSet(sOnChangeListeners.keySet());

		synchronized(listeners)
		{
			Iterator<OnChangeListener> i = listeners.iterator();

			while(i.hasNext())
			{
				Exception ex;
				OnChangeListener l = null;

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

		Log.i(TAG, "dispatchEventToListeners: adding " + sOnChangeListenersQueue.size() + " listeners from queue");

		// FIXME we also need to take care of listeners being removed by other listeners

		sPutNewListenersInQueue = false;
		sOnChangeListeners.putAll(sOnChangeListenersQueue);
		sOnChangeListenersQueue.clear();
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

	public interface Filter<T extends Entry>
	{
		boolean matches(T t);
	}

	private Database() {}
}
