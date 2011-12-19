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
	
	public static final int DB_VERSION = 46;
	private static final String DB_NAME = "db.sqlite";
	
	private Dao<Drug, Integer> mDrugDao = null;
	private Dao<Intake, Integer> mIntakeDao = null;

	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource cs)
	{
		try
		{
			TableUtils.createTable(cs, Drug.class);
			TableUtils.createTable(cs, Intake.class);
		}
		catch(SQLException e)
		{
			throw new RuntimeException("Error while creating tables", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion)
	{
		Log.d(TAG, "onUpgrade: " + oldVersion + " -> " + newVersion);
		
		try
		{
			if(newVersion == DB_VERSION)
			{
				final String packageName = Database.class.getPackage().getName();
				final String classNames[] = { "Drug", "Intake" };
				
				int updatedDataCount = 0;
				
				for(String className : classNames)
				{
					final String oldDataClassName = packageName + ".v" + oldVersion + ".Old" + className;
					final String newDataClassName = packageName + "." + className;
					
					Log.d(TAG, "  Mapping " + oldDataClassName + " to " + newDataClassName);
					
					final Class<?> oldDataClass;
					final Class<?> newDataClass = Class.forName(newDataClassName);
										
					try
					{
						oldDataClass = Class.forName(oldDataClassName);
						++updatedDataCount;
					}
					catch(Exception e)
					{
						Log.d(TAG, "  Not upgrading " + className);
						continue;
					}					
					
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
					}
					
					if(updatedDataCount == 0)
						resetDatabase(db, cs);
				}
			}
			else if(oldVersion < newVersion)
				resetDatabase(db, cs);
			else
				throw new RuntimeException("Refusing to downgrade database from " + oldVersion + " to " + newVersion);
		}
		catch(Exception e)
		{			
			throw new RuntimeException("Error while attempting database upgrade", e);
		}
	}

	public void dropTables() {
		onUpgrade(getWritableDatabase(), 0, DB_VERSION);
	}

	public synchronized Dao<Drug, Integer> getDrugDao()
	{
		try
		{
			if(mDrugDao == null)
				mDrugDao = getDao(Drug.class);
		}
		catch(SQLException e)
		{
			throw new RuntimeException("Cannot get DAO", e);
		}
		return mDrugDao;
	}

	public synchronized Dao<Intake, Integer> getIntakeDao()
	{
		try
		{
			if(mIntakeDao == null)
				mIntakeDao = getDao(Intake.class);
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
	
	private void resetDatabase(SQLiteDatabase db, ConnectionSource cs)
	{
		Log.d(TAG, "Resetting DB");
		
		try
		{
			TableUtils.dropTable(cs, Drug.class, true);
			TableUtils.dropTable(cs, Intake.class, true);
		}
		catch(SQLException e)
		{
			Log.e(TAG, "resetDatabase", e);
		}
		
		onCreate(db, cs);
	}
}

