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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.SplashScreenActivity;
import at.jclehner.rxdroid.util.WrappedCheckedException;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.ReferenceObjectCache;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Helper class for ORMLite related voodoo.
 *
 * @author Joseph Lehner
 *
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper
{
	private static final String TAG = DatabaseHelper.class.getSimpleName();
	private static final boolean LOGV = false;

	public static final int DB_VERSION = 58;
	public static final String DB_NAME = "db.sqlite";

	public static class DatabaseError extends RuntimeException
	{
		private static final long serialVersionUID = 4326067582393937172L;

		public static final int E_GENERAL = 0;
		public static final int E_UPGRADE = 1;
		public static final int E_DOWNGRADE = 2;
		public static final int E_BACKUP = 3;

		public DatabaseError(int type, String string, Throwable e)
		{
			super(string, e);
			mType = type;
		}

		public DatabaseError(String string, Throwable e) {
			this(E_GENERAL, string, e);
		}

		public DatabaseError(int type, Throwable e)
		{
			super(e);
			mType = type;
		}

		public DatabaseError(int type, String string)
		{
			super(string);
			mType = type;
		}

		public DatabaseError(String string) {
			this(E_GENERAL, string);
		}

		public DatabaseError(int type) {
			this(type, getTypeAsString(type));
		}

		public int getType() {
			return mType;
		}

		private static String getTypeAsString(int type)
		{
			final String[] names = {
				"GENERAL",
				"UPGRADE",
				"DOWNGRADE",
				"BACKUP"
			};

			if(type < names.length)
				return names[type];

			return "UNKNOWN";
		}

		private final int mType;
	}

	DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource cs)
	{
		try
		{
			SplashScreenActivity.setStatusMessage(R.string._title_db_status_initializing);
			for(Class<?> clazz : Database.CLASSES)
				TableUtils.createTableIfNotExists(cs, clazz);
		}
		catch(SQLException e)
		{
			throw new DatabaseError("Failed to create tables", e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion)
	{
		if(oldVersion == newVersion)
			return;

		SplashScreenActivity.setStatusMessage(R.string._title_db_status_upgrading);

		db.beginTransaction();

		if(upgrade(cs, oldVersion, newVersion))
		{
			db.setTransactionSuccessful();
			db.endTransaction();
			return; // everything ok
		}

		db.endTransaction();

		db.setVersion(oldVersion);
		throw new DatabaseError(oldVersion < newVersion ? DatabaseError.E_UPGRADE : DatabaseError.E_DOWNGRADE);
	}

	// !!! Do NOT @Override (crashes on API < 11) !!!
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

	@Override
	public void close()
	{
		super.close();


	}

	public void reset()
	{
		ConnectionSource cs = getConnectionSource();

		try
		{
			for(Class<?> clazz : Database.CLASSES)
				TableUtils.dropTable(cs, clazz, true);
		}
		catch(SQLException e)
		{
			Log.e(TAG, "reset", e);
			//throw new DbError(DbError.E_GENERAL, e);
		}

		onCreate(getWritableDatabase(), cs);
	}

	public <D extends Dao<T,?>, T extends Object> D getDao(Class<T> clazz) throws SQLException
	{
		final D dao = super.getDao(clazz);

		if(!Database.USE_CUSTOM_CACHE)
			dao.setObjectCache(WeakObjectCache.getInstance());

		return dao;
	}

	/* package */ <T> Dao<T, Integer> getDaoChecked(Class<T> clazz)
	{
		try
		{
			return getDao(clazz);
		}
		catch(SQLException e)
		{
			throw new WrappedCheckedException("Error getting DAO for " + clazz.getSimpleName(), e);
		}
	}

	private boolean upgrade(ConnectionSource cs, int oldVersion, int newVersion)
	{
		if(oldVersion > newVersion)
			return false;
		else if(oldVersion == newVersion)
			return true;

		Log.i(TAG, "Upgrading DB v" + oldVersion + " -> v" + newVersion);

		final String packageName = Database.class.getPackage().getName();
		final String oldPackageName = packageName + ".v" + oldVersion;

		int updatedDataCount = 0;

		Exception ex;

		try
		{
			if(runUpgradeHelperMethodUpgradeDatabase(oldPackageName, cs))
				++updatedDataCount;

			for(Class<?> clazz : Database.CLASSES)
			{
				if(upgradeTable(cs, oldVersion, newVersion, clazz))
					++updatedDataCount;

				if(updateTableData(oldVersion, newVersion, clazz))
					++updatedDataCount;
			}

			DaoManager.clearCache();
			DaoManager.clearDaoCache();

			return updatedDataCount != 0;
			//return true;
		}
		catch(SQLException e)
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
		catch(NoSuchMethodException e)
		{
			ex = e;
		}
		catch(ClassNotFoundException e)
		{
			ex = e;
		}

		Log.e(TAG, "upgrade", ex);

		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean upgradeTable(ConnectionSource cs, int oldVersion, int newVersion, Class<?> clazz)
			throws ClassNotFoundException, SQLException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException
	{
		final String packageName = Database.class.getPackage().getName();
		final String oldPackageName = packageName + ".v" + oldVersion;

		final String className = clazz.getSimpleName();
		final String oldDataClassName = oldPackageName + ".Old" + className;
		final String newDataClassName = packageName + "." + className;

		final Class<?> oldDataClass = Reflect.classForName(oldDataClassName);
		if(oldDataClass == null)
		{
			if(oldVersion + 1 < newVersion)
			{
				// If the database is older than newVersion - 1, a change
				// might have been introduced somewhere between oldVersion
				// and newVersion.
				return upgradeTable(cs, oldVersion + 1, newVersion, clazz);
			}

			return false;
		}

		if(LOGV) Log.v(TAG, "  Found " + oldDataClassName);

		final Class<?> newDataClass = Class.forName(newDataClassName);

		@SuppressWarnings("rawtypes")
		final Dao newDao = getDao(newDataClass);
		final List<?> oldData = getDao(oldDataClass).queryForAll();

		TableUtils.dropTable(cs, oldDataClass, true);
		TableUtils.createTableIfNotExists(cs, newDataClass);

		final Method convertMethod = oldDataClass.getMethod("convertToCurrentDatabaseFormat");

		for(Object data : oldData)
		{
			final Entry entry = (Entry) convertMethod.invoke(data);
			newDao.create(entry);
		}

		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean updateTableData(int oldVersion, int newVersion, Class<?> clazz)
			throws ClassNotFoundException, SQLException, NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException
	{
		final String packageName = Database.class.getPackage().getName() + ".v" + oldVersion;
		final String methodName = "update" + clazz.getSimpleName() + "TableEntry";
		final Method method = getUpgradeHelperMethod(packageName, methodName, clazz);

		if(method == null)
			return false;

		final Dao dao = getDao(clazz);
		final List<?> entries = dao.queryForAll();

		for(Object entry : entries)
		{
			invokeUpgradeHelperMethod(method, entry);
			dao.update(entry);
		}

		return true;
	}

	private boolean runUpgradeHelperMethodUpgradeDatabase(String packageName, ConnectionSource cs)
	{
		final Method method = getUpgradeHelperMethod(packageName, "upgradeDatabase", ConnectionSource.class);

		if(method == null)
			return false;

		return invokeUpgradeHelperMethod(method, cs);
	}

	private Method getUpgradeHelperMethod(String packageName, String methodName, Class<?>... parameterTypes)
	{
		try
		{
			final Class<?> helper = Class.forName(packageName + ".UpgradeHelper");
			final Method method = helper.getMethod(methodName, ConnectionSource.class);

			return method;
		}
		catch(ClassNotFoundException e)
		{
			// fall through
		}
		catch(NoSuchMethodException e)
		{
			// fall through
		}

		if(LOGV) Log.v(TAG, "getUpgradeHelperMethod: no " + methodName + " in " + packageName);
		return null;
	}

	private boolean invokeUpgradeHelperMethod(Method method, Object... args)
	{
		try
		{
			method.invoke(null, args);
			Log.i(TAG, "Invoked " + method.getName());
			return true;
		}
		catch(IllegalArgumentException e)
		{
			Log.w(TAG, e);
		}
		catch(IllegalAccessException e)
		{
			Log.w(TAG, e);
		}
		catch(InvocationTargetException e)
		{
			Log.w(TAG, e);
		}

		return false;
	}

	/* package */ static class WeakObjectCache extends ReferenceObjectCache
	{
		private static final WeakObjectCache sInstance = new WeakObjectCache();

		@Override
		public <T extends Object, ID extends Object> T get(Class<T> clazz, ID id)
		{
			final T ret = super.get(clazz, id);
			//Log.d("WeakObjectCache", "get(" + clazz.getSimpleName() + ", " + id + ")");
			return ret;
		}

		@Override
		public <T extends Object, ID extends Object> void put(Class<T> clazz, ID id, T data)
		{
			super.put(clazz, id, data);
			//Log.d("WeakObjectCache", "put(" + clazz.getSimpleName() + ", " + id + ", " + data + ")");
		}

		public static WeakObjectCache getInstance() {
			return sInstance;
		}

		private WeakObjectCache() {
			super(true);
		}
	}
}

