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

package at.jclehner.rxdroid;

import java.io.File;
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

import net.lingala.zip4j.exception.ZipException;

import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;

public class BackupAgent extends BackupAgentHelper
{
	private static final String TAG = BackupAgent.class.getSimpleName();

	private static final String KEY_BACKUP = "backup";

	private File mBackupFile;

	@Override
	public void onCreate()
	{
		RxDroid.setContext(getApplicationContext());

		if(!isBackupEnabled())
			return;

		mBackupFile = new File(getFilesDir(), "cloud.rxdbak");
		addHelper(KEY_BACKUP, new FileBackupHelper(this, mBackupFile.getAbsolutePath()));

		Log.i(TAG, "onCreate");
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

		try
		{
			Backup.createBackup(mBackupFile, null);
		}
		catch(ZipException e)
		{
			Log.w(TAG, e);
			return;
		}

		Log.i(TAG, "Created backup at " + mBackupFile);
		super.onBackup(oldState, data, newState);
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
			throws IOException
	{
		Log.i(TAG, "Restoring...");

		super.onRestore(data, appVersionCode, newState);
		final Backup.BackupFile bf = new Backup.BackupFile(mBackupFile.getAbsolutePath());
		if(bf.isValid() && !bf.isEncrypted())
		{
			bf.restore(null);
			Database.reload(getApplicationContext());
		}
	}

	private static boolean isBackupEnabled()
	{
		try
		{
			return Settings.getBoolean(Keys.USE_BACKUP_FRAMEWORK, false);
		}
		catch(Exception e)
		{
			Log.w(TAG, e);
			return false;
		}
	}
}
