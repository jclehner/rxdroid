/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.util.Util;

public class Backup
{
	private static final String TAG = Backup.class.getSimpleName();

	public static final File DIRECTORY =
			new File(Environment.getExternalStorageDirectory(), "RxDroid");

	public static class BackupFile
	{
		private ZipFile mZip = null;
		private String mPath;

		private String[] mInfo;
		private Date mTimestamp;
		private int mVersion;
		private boolean mIsEncrypted;

		public BackupFile(String path)
		{
			mPath = path;

			try
			{
				mZip = new ZipFile(path);
				mIsEncrypted = mZip.isEncrypted();

				if(mZip.getComment() == null)
					return;

				mInfo = mZip.getComment().split(":");
			}
			catch(ZipException e)
			{
				Log.w(TAG, e);
				return;
			}

			if(mInfo.length < 2 || !mInfo[0].startsWith("rxdbak"))
				return;

			mVersion = Integer.parseInt(mInfo[0].substring("rxdbak".length()));
			mTimestamp = new Date(Long.parseLong(mInfo[1]));
		}

		public boolean isValid() {
			return mZip != null && mVersion != 0;
		}

		public boolean isEncrypted() {
			return mIsEncrypted;
		}

		public int version() {
			return mVersion;
		}

		public String getPath() {
			return mPath;
		}

		public Date getTimestamp() {
			return mTimestamp;
		}

		public String getLocation()
		{
			final String file = new File(mPath).getAbsolutePath();
			final String extDir = Environment.getExternalStorageDirectory().getAbsolutePath();
			if(file.startsWith(extDir))
				return file.substring(extDir.length() + 1);
			else
				return file;
		}

		public void restore(String password) throws ZipException
		{
			if(password != null)
				mZip.setPassword(password);

			synchronized(Database.LOCK_DATA)
			{
				mZip.extractAll(RxDroid.getPackageInfo().applicationInfo.dataDir);
				Settings.init(true);
				Database.reload(RxDroid.getContext());
			}

			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(false);
		}
	}

	public static void createBackup(File outFile, String password) throws ZipException
	{
		if(outFile == null)
		{
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			outFile = new File(Environment.getExternalStorageDirectory(),
					"RxDroid/" + sdf.format(new Date()) + ".rxdbak");
		}

		synchronized(Database.LOCK_DATA)
		{
			final ZipFile zip = new ZipFile(outFile);
			final File dataDir = new File(RxDroid.getPackageInfo().applicationInfo.dataDir);

			for(int i = 0; i != FILES.length; ++i)
			{
				final File file = new File(dataDir, FILES[i]);
				if(!file.exists())
					continue;

				final ZipParameters zp = new ZipParameters();
				zp.setRootFolderInZip(new File(FILES[i]).getParent());
				zp.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
				zp.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);

				if(password != null)
				{
					zp.setPassword(password);
					zp.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
					zp.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
					zp.setEncryptFiles(true);
					//zp.setCompressionMethod(Zip4jConstants.COMP_AES_ENC);
				}

				zip.addFile(file, zp);
			}

			zip.setComment("rxdbak1:" + System.currentTimeMillis() + ":DBv" + DatabaseHelper.DB_VERSION);
		}
	}

	private static final String[] FILES = {
			"databases/" + DatabaseHelper.DB_NAME,
			"shared_prefs/at.jclehner.rxdroid_preferences.xml",
			"shared_prefs/showcase_internal.xml"
	};
}
