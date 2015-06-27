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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.util.Util;
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

	public static final int DB_VERSION = 60;
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
		else if(oldVersion > newVersion)
			throw new DatabaseError(DatabaseError.E_DOWNGRADE);

		try
		{
			new DatabaseUpgrader(db, cs).onUpgrade(oldVersion, newVersion);
		}
		catch(SQLException e)
		{
			throw new DatabaseError(DatabaseError.E_UPGRADE, e);
		}
	}

	// !!! Do NOT @Override (crashes on API < 11) !!!
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

	public <D extends Dao<T,?>, T extends Object> D getDao(Class<T> clazz) throws SQLException
	{
		final D dao = super.getDao(clazz);

		if(!Database.USE_CUSTOM_CACHE)
			dao.setObjectCache(new ReferenceObjectCache(true));

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
}

