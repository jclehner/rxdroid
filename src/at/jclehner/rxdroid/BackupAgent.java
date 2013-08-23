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

package at.jclehner.rxdroid;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;

public class BackupAgent extends BackupAgentHelper
{
	private static final String TAG = BackupAgent.class.getSimpleName();

	private static final String KEY_DATABASE = "database";
	private static final String KEY_PREFS = "preferences";

	@Override
	public void onCreate()
	{
		RxDroid.setContext(getApplicationContext());

		if(!isBackupEnabled())
			return;

		addHelper(KEY_DATABASE, new FileBackupHelper(this, "../databases/" + DatabaseHelper.DB_NAME));
		addHelper(KEY_PREFS, new DefaultSharedPreferencesBackupHelper(this));

		Log.i(TAG, "Created BackupAgent");
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState)
			throws IOException
	{
		if(!isBackupEnabled())
		{
			Log.i(TAG, "Backup requested but disabled");
			return;
		}

		synchronized(Database.LOCK_DATA)
		{
			Log.i(TAG, "Backing up...");
			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
			throws IOException
	{
		Log.i(TAG, "Restoring...");

		synchronized(Database.LOCK_DATA) {
			super.onRestore(data, appVersionCode, newState);
		}
	}

	private static boolean isBackupEnabled()
	{
		try
		{
			return Settings.getBoolean(Keys.USE_BACKUP_FRAMEWORK, true);
		}
		catch(Exception e)
		{
			Log.w(TAG, e);
			return true;
		}
	}

	private static class DefaultSharedPreferencesBackupHelper extends SharedPreferencesBackupHelper
	{
		public DefaultSharedPreferencesBackupHelper(Context context) {
			super(context, getDefaultPreferencesName(context));
		}

		private static String getDefaultPreferencesName(Context context) {
			return context.getPackageName() + "_preferences";
		}
	}
}
