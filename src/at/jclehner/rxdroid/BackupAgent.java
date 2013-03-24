package at.jclehner.rxdroid;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
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
