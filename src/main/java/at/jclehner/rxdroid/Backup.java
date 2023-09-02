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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.util.Log;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import androidx.documentfile.provider.DocumentFile;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class Backup
{
	private static final String TAG = Backup.class.getSimpleName();

	private static final File DIRECTORY =
			new File(Environment.getExternalStorageDirectory(), "RxDroid");

	public static @Nullable DocumentFile getDirectory()
	{
		final String str = Settings.getString(Settings.Keys.BACKUP_DIRECTORY, null);
		if(str == null)
			return null;

		try
		{
			final Context c = RxDroid.getContext();
			final Uri uri = Uri.parse(str);

			final DocumentFile df = DocumentFile.fromTreeUri(c, uri);
			if(!df.isDirectory())
			{
				Log.w(TAG, "Not a directory: " + uri);
				return null;
			}
			else if(!df.canWrite() || !df.canRead())
			{
				Log.w(TAG, "No R/W access: " + uri);
				return null;
			}

			return df;
		}
		catch(Exception e)
		{
			Log.w(TAG, e);
			return null;
		}
	}



	public static abstract class StorageStateListener extends BroadcastReceiver
	{
		private static final IntentFilter INTENT_FILTER = new IntentFilter();

		private boolean mReadable;
		private boolean mWritable;

		public StorageStateListener(Context context) {
			update(context, getStorageState());
		}

		@Override
		public final void onReceive(Context context, Intent intent)
		{
			final String storageState = getStorageState();
			update(context, storageState);
			onStateChanged(storageState, intent);
		}

		public void register(Context context) {
			context.registerReceiver(this, INTENT_FILTER);
		}

		public void unregister(Context context) {
			context.unregisterReceiver(this);
		}

		public abstract void onStateChanged(String storageState, Intent intent);

		public boolean isReadable() {
			return mReadable;
		}

		public boolean isWritable() {
			return mWritable;
		}

		public static boolean isReadable(String storageState)
		{
			return Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)
					|| Environment.MEDIA_MOUNTED.equals(storageState);
		}

		public static boolean isWritable(String storageState) {
			return Environment.MEDIA_MOUNTED.equals(storageState);
		}

		public void update(Context context) {
			update(context, Environment.getExternalStorageState());
		}

		private void update(Context context, String storageState)
		{
			if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED)
			{
				mReadable = mWritable = false;
			}
			else
			{
				mReadable = isReadable(storageState);
				mWritable = isWritable(storageState);
			}
		}

		static
		{
			INTENT_FILTER.addAction(Intent.ACTION_MEDIA_MOUNTED);
			INTENT_FILTER.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
			INTENT_FILTER.addAction(Intent.ACTION_MEDIA_EJECT);
			INTENT_FILTER.addAction(Intent.ACTION_MEDIA_REMOVED);
		}
	}

	public static class BackupFile
	{
		private Uri mUri;
		private String[] mInfo;
		private Date mTimestamp;
		private int mVersion = 0;
		private int mDbVersion;
		private final boolean mIsEncrypted = false;

		private String mLocation;

		public BackupFile(Uri uri)
		{
			mUri = uri;

			final InputStream is = openInputStream();
			if(is == null)
				return;

			final String comment;

			// copy the InputStream to a temporary file, because ZipInputStream doesn't
			// support reading the ZIP file comment
			final File f = new File(RxDroid.getContext().getCacheDir(), "temp.rxdbak");

			try
			{
				Util.copyFile(is, f);
				try(final ZipFile zf = new ZipFile(f))
				{
					comment = zf.getComment();
				}
			}
			catch(IOException e)
			{
				Log.w(TAG, e);
				return;
			}
			finally
			{
				Util.closeQuietly(is);
			}

			if(comment == null)
				return;

			mInfo = comment.split(":");

			if(mInfo.length < 2 || !mInfo[0].startsWith("rxdbak") || mInfo[0].equals("rxdbak"))
				return;

			Log.d(TAG, "mInfo=" + Arrays.toString(mInfo));

			mVersion = Integer.parseInt(mInfo[0].substring("rxdbak".length()));

			mTimestamp = new Date(Long.parseLong(mInfo[1]));

			if(mInfo.length >= 3)
				mDbVersion = Integer.parseInt(mInfo[2].substring("DBv".length()));
			else
				mDbVersion = -1;

			final DocumentFile df = DocumentFile.fromSingleUri(RxDroid.getContext(), mUri);
			mLocation = (df != null) ? df.getName() : "[unknown]";
		}

		public boolean isValid() {
			return mVersion != 0;
		}

		public boolean isEncrypted() {
			return mIsEncrypted;
		}

		public int version() {
			return mVersion;
		}

		public int dbVersion() {
			return mDbVersion;
		}

		public Date getTimestamp() {
			return mTimestamp;
		}

		public String getLocation() {
			return mLocation;
		}

		public boolean restore(String password)
		{
			if(!isValid())
				throw new IllegalStateException("Invalid backup file");

			synchronized(Database.LOCK_DATA)
			{
				final ZipInputStream zis = new ZipInputStream(openInputStream());
				if(zis == null)
					throw new IllegalStateException("Failed to open input stream");

				FileOutputStream fos = null;

				try
				{
					ZipEntry ze;
					while((ze = zis.getNextEntry()) != null)
					{
						if (!Arrays.asList(FILES).contains(ze.getName()))
						{
							Log.d(TAG, "Skipping " + ze.getName());
							continue;
						}

						final File f = new File(RxDroid.getPackageInfo().applicationInfo.dataDir, ze.getName());
						final byte[] bytes = new byte[1024];
						int n;

						fos = new FileOutputStream(f);

						while ((n = zis.read(bytes)) > 0) {
							fos.write(bytes, 0, n);
						}

						fos.close();
					}
				}
				catch(IOException e)
				{
					Log.w(TAG, e);
					throw new WrappedCheckedException(e);
				}
				finally
				{
					Util.closeQuietly(fos);
					Util.closeQuietly(zis);
				}

				Settings.init(true);
			}

			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(false);
			return true;
		}

		private @Nullable InputStream openInputStream()
		{
			try
			{
				return RxDroid.getContext().getContentResolver().openInputStream(mUri);
			}
			catch(FileNotFoundException e)
			{
				return null;
			}
		}
	}

	public static String getStorageState()
	{
		final String state;

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
			state = Environment.getStorageState(DIRECTORY);
		else
			state = Environment.getExternalStorageState();

		if(Environment.MEDIA_MOUNTED.equals(state) && !DIRECTORY.canWrite())
		{
			Log.d(TAG, "Storage state reported as MEDIA_MOUNTED, but " +
					DIRECTORY + " is not writeable");

			return Environment.MEDIA_MOUNTED_READ_ONLY;
		}

		return state;
	}

	public static String makeBackupFilename(String template)
	{
		return template + ".rxdbak";
	}

	@NonNull public static void createBackup(@Nullable String filename)
	{
		if(filename == null)
		{
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			filename = makeBackupFilename(sdf.format(new Date()));
		}

		synchronized(Database.LOCK_DATA)
		{
			try
			{
				DocumentFile dir = Backup.getDirectory().createFile("application/octet-stream",
						filename);
				final OutputStream os = RxDroid.getContext().getContentResolver().openOutputStream(
						dir.getUri());
				final ZipOutputStream zos = new ZipOutputStream(os);

				final File dataDir = new File(RxDroid.getPackageInfo().applicationInfo.dataDir);

				for(int i = 0; i != FILES.length; ++i)
				{
					final File file = new File(dataDir, FILES[i]);
					if(!file.exists())
						continue;

					final ZipEntry ze = new ZipEntry(FILES[i]);
					FileInputStream fis = null;

					try
					{
						fis = new FileInputStream(file);
						zos.putNextEntry(ze);
						Util.copy(fis, zos);
					}
					finally
					{
						Util.closeQuietly(fis);
					}
				}

				zos.setComment(
						"rxdbak1:" + System.currentTimeMillis() + ":DBv" + DatabaseHelper.DB_VERSION);
				zos.close();
			}
			catch(IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	private static final String[] FILES = {
			"databases/" + DatabaseHelper.DB_NAME,
			"shared_prefs/at.jclehner.rxdroid_preferences.xml",
			"shared_prefs/showcase_internal.xml"
	};
}
