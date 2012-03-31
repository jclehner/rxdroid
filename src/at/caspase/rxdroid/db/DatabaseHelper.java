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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
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
	private static final boolean LOGV = true;

	public static class DatabaseError extends RuntimeException
	{
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

		private static final long serialVersionUID = 4326067582393937172L;
	}

	public static final int DB_VERSION = 50;
	public static final String DB_NAME = "db.sqlite";

	private final Context mContext;

	DatabaseHelper(Context context)
	{
		super(context, DB_NAME, null, DB_VERSION);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource cs)
	{
		try
		{
			for(Class<?> clazz : Database.CLASSES)
				TableUtils.createTable(cs, clazz);
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

		final boolean isUpgrade = newVersion > oldVersion;

		if(isUpgrade)
		{
			if(upgrade(cs, oldVersion, newVersion))
				return; // everything ok
		}

		db.setVersion(oldVersion);
		throw new DatabaseError(isUpgrade ? DatabaseError.E_UPGRADE : DatabaseError.E_DOWNGRADE);
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) { // Do NOT @Override (crashes on API < 11)
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

	@SuppressWarnings("unchecked")
	private boolean upgrade(ConnectionSource cs, int oldVersion, int newVersion)
	{
		if(oldVersion >= newVersion)
			return true;

		Log.i(TAG, "upgrade: v" + oldVersion + " -> v" + newVersion);

		final String packageName = Database.class.getPackage().getName();
		final String oldPackageName = packageName + ".v" + oldVersion;

		int updatedDataCount = 0;

		Exception ex;

		try
		{
			try
			{
				final Class<?> hook = Class.forName(oldPackageName + ".Hook");
				Log.i(TAG, "  Found hook in " + oldPackageName);

				final Constructor<?> ctor = hook.getConstructor(cs.getClass());
				Runnable r = (Runnable) ctor.newInstance(cs);
				r.run();

				++updatedDataCount;
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

			for(Class<?> clazz : Database.CLASSES)
			{
				final String className = clazz.getSimpleName();
				final String oldDataClassName = oldPackageName + ".Old" + className;
				final String newDataClassName = packageName + "." + className;

				final Class<?> oldDataClass;
				final Class<?> newDataClass = Class.forName(newDataClassName);

				//TableUtils.createTableIfNotExists(cs, newDataClass);
				//++updatedDataCount;

				try
				{
					oldDataClass = Class.forName(oldDataClassName);
				}
				catch(ClassNotFoundException e)
				{
					continue;
				}

				Log.i(TAG, "  Found " + oldDataClassName);

				@SuppressWarnings("rawtypes")
				final Dao newDao = getDao(newDataClass);
				final List<?> oldData = getDao(oldDataClass).queryForAll();

				TableUtils.dropTable(cs, oldDataClass, true);
				TableUtils.createTable(cs, newDataClass);

				for(Object data : oldData)
				{
					final Method convertMethod = oldDataClass.getMethod("convert");

					Entry entry = (Entry) convertMethod.invoke(data);
					newDao.create(entry);

					++updatedDataCount;
				}
			}

			return updatedDataCount != 0;
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

