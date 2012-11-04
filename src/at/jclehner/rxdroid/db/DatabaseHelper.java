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

package at.jclehner.rxdroid.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.SplashScreenActivity;
import at.jclehner.rxdroid.util.WrappedCheckedException;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
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
	private static final String TAG = DatabaseHelper.class.getName();
	private static final boolean LOGV = false;

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

	public static final int DB_VERSION = 53;
	public static final String DB_NAME = "db.sqlite";

	DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource cs)
	{
		try
		{
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

	@SuppressWarnings("unchecked")
	private boolean upgrade(ConnectionSource cs, int oldVersion, int newVersion)
	{
		if(oldVersion >= newVersion)
			return true;

		Log.i(TAG, "Upgrading DB v" + oldVersion + " -> v" + newVersion);

		final String packageName = Database.class.getPackage().getName();
		final String oldPackageName = packageName + ".v" + oldVersion;

		int updatedDataCount = 0;

		Exception ex;

		try
		{
			final String callbackName;

			if(oldVersion < 54)
				callbackName = "BeforeDatabaseUpgradeHook";
			else
				callbackName = "BeforeDatabaseUpgradeCallback";

			if(runCallback(oldPackageName, callbackName, cs))
				++updatedDataCount;

			for(Class<?> clazz : Database.CLASSES)
			{
				if(upgradeTable(cs, oldVersion, newVersion, clazz))
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

		for(Object data : oldData)
		{
			final Method convertMethod = oldDataClass.getMethod("convertToCurrentDatabaseFormat");
			final Entry entry = (Entry) convertMethod.invoke(data);
			newDao.create(entry);
		}

		return true;
		//return !oldData.isEmpty();
	}

	private boolean runCallback(String packageName, String hookName, ConnectionSource cs)
	{
		try
		{
			final Class<?> hook = Class.forName(packageName + "." + hookName);
			if(LOGV) Log.v(TAG, "  Found " + hookName + " in " + packageName);

			final Constructor<?> ctor = hook.getConstructor(cs.getClass());
			Runnable r = (Runnable) ctor.newInstance(cs);
			r.run();

			return true;
		}
		catch(ClassNotFoundException e)
		{
			// ignore
		}
		catch(InstantiationException e)
		{
			Log.w(TAG, e);
		}
		catch(NoSuchMethodException e)
		{
			Log.w(TAG, e);
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

	/*
	private void backup()
	{
		String packageName = mContext.getApplicationInfo().packageName;

		File dbDir = new File(Environment.getDataDirectory(), packageName + "/databases");
		File currentDb = new File(dbDir, DB_NAME);
	    File backupDb = new File(dbDir, "backup-" + System.currentTimeMillis() / 1000 + ".sqlite");

	    if(!dbDir.exists() || !currentDb.exists() || !dbDir.canWrite() || !backupDb.canWrite())
	    	throw new DbError(DbError.E_BACKUP);

		try
		{
			FileChannel src = new FileInputStream(currentDb).getChannel();
			FileChannel dst = new FileOutputStream(backupDb).getChannel();

			dst.transferFrom(src, 0, src.size());

			src.close();
			dst.close();
		}
		catch (FileNotFoundException e)
		{
			throw new DbError(DbError.E_BACKUP, e);
		}
		catch (IOException e)
		{
			throw new DbError(DbError.E_BACKUP, e);
		}
	}
	*/
}

