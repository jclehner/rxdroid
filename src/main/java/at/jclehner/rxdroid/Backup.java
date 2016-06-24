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
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import at.jclehner.androidutils.StorageHelper;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class Backup
{
	private static final String TAG = Backup.class.getSimpleName();
	private static final String DIRECTORY_NAME = "RxDroid" + (BuildConfig.DEBUG ? "Dbg" : "");

	public static final File DIRECTORY =
			new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);

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
		private ZipFile mZip = null;
		private String mPath;

		private String[] mInfo;
		private Date mTimestamp;
		private int mVersion;
		private int mDbVersion;
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

			if(mInfo.length < 2 || !mInfo[0].startsWith("rxdbak") || mInfo[0].equals("rxdbak"))
				return;

			mVersion = Integer.parseInt(mInfo[0].substring("rxdbak".length()));

			mTimestamp = new Date(Long.parseLong(mInfo[1]));

			if(mInfo.length >= 3)
				mDbVersion = Integer.parseInt(mInfo[2].substring("DBv".length()));
			else
				mDbVersion = -1;
		}

		public boolean isValid() {
			return mZip != null && mVersion == 1;
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

		public String getPath() {
			return mPath;
		}

		public Date getTimestamp() {
			return mTimestamp;
		}

		public String getLocation()
		{
			final String file = new File(mPath).getAbsolutePath();

			final String filesDir = RxDroid.getContext().getFilesDir().getAbsolutePath();
			if(file.startsWith(filesDir))
				return file.replace(filesDir, "[files]");

			return StorageHelper.getPrettyName(file, RxDroid.getContext(), null);
		}

		public ZipFile getZip() {
			return mZip;
		}

		public boolean restore(String password)
		{
			if(!isValid())
				throw new IllegalStateException("Invalid backup file");

			synchronized(Database.LOCK_DATA)
			{
				final String key = Settings.getString(Settings.Keys.BACKUP_KEY, "");

				try
				{
					if(password != null)
						mZip.setPassword(passwordToKey(password));

					mZip.extractAll(RxDroid.getPackageInfo().applicationInfo.dataDir);
				}
				catch(ZipException e)
				{
					final String msg = e.getMessage();
					if(password != null && msg.toLowerCase(Locale.US).contains("password"))
						return false;

					throw new WrappedCheckedException(e);
				}

				Settings.init(true);
				Settings.putString(Settings.Keys.BACKUP_KEY, key);
			}

			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(false);
			return true;
		}
	}

	public static String getStorageState()
	{
		final String state;

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			state = Environment.getExternalStorageState(DIRECTORY);
		else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
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

	public static File makeBackupFilename(String template)
	{
		return new File(DIRECTORY + "/" + template + ".rxdbak");
	}

	public static File createBackup(File outFile, String password) throws ZipException
	{
		return createBackup(outFile, password, RxDroid.getPackageInfo().applicationInfo.dataDir, -1);
	}

	public static File createBackup(File outFile, String password, String dataDir, long time) throws ZipException
	{
		if(outFile == null)
		{
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			outFile = makeBackupFilename(sdf.format(new Date()));
		}

		synchronized(Database.LOCK_DATA)
		{
			final ZipFile zip = new ZipFile(outFile);

			for(int i = 0; i != FILES.length; ++i)
			{
				final File file = new File(dataDir, FILES[i]);
				if(!file.exists())
					continue;

				final ZipParameters zp = new ZipParameters();
				zp.setRootFolderInZip(new File(FILES[i]).getParent());
				zp.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
				zp.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);

				if(!TextUtils.isEmpty(password))
				{
					zp.setPassword(password);
					zp.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
					zp.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
					zp.setEncryptFiles(true);
					//zp.setCompressionMethod(Zip4jConstants.COMP_AES_ENC);
				}

				zip.addFile(file, zp);
			}

			if(time == -1)
				time = System.currentTimeMillis();

			zip.setComment("rxdbak1:" + time + ":DBv" + DatabaseHelper.DB_VERSION);
		}

		return outFile;
	}

	public static List<File> getBackupDirectories(Context context)
	{
		final List<File> dirs = new ArrayList<>();
		dirs.add(context.getFilesDir());
		for(StorageHelper.PathInfo si: StorageHelper.getDirectories(context))
			dirs.add(new File(si.path, DIRECTORY_NAME));

		return dirs;
	}

	public static List<File> getBackupFiles(Context context)
	{
		final List<File> files = new ArrayList<>();

		for(File dir : getBackupDirectories(context))
		{
			if(!dir.exists() || !dir.isDirectory())
				continue;

			final File[] dirFiles = dir.listFiles(FILTER);
			if(dirFiles != null)
			{
				for(File file : dirFiles)
				{
					if(file.isFile())
						files.add(file);
				}
			}
		}

		return files;
	}

	private static void encrypt(Context context, File backup, String password) throws ZipException, IOException
	{
		final BackupFile bf = new BackupFile(backup.getAbsolutePath());
		if(!bf.isValid() || bf.isEncrypted())
			return;

		Log.i(TAG, "Encrypting " + backup);

		final File tmpDir = new File(context.getCacheDir(), "tmp");
		tmpDir.mkdirs();

		final File tmpFile = new File(tmpDir, "tmp.rxdbak");

		bf.getZip().extractAll(tmpDir.getAbsolutePath());
		tmpFile.delete();
		createBackup(tmpFile, password, tmpDir.getAbsolutePath(), bf.getTimestamp().getTime());
		Util.copyFile(tmpFile, backup);
	}

	private static void encryptAll(final Context context, final String key, final Runnable callback)
	{
		new AsyncTask<Void, String, Exception>() {
			private ProgressDialog mDialog;
			private List<File> mFiles;

			@Override
			protected void onPreExecute()
			{
				mFiles = Backup.getBackupFiles(context);

				mDialog = new ProgressDialog(context);
				mDialog.setTitle(R.string._msg_encrypting);
				mDialog.setCancelable(false);
				mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mDialog.setIndeterminate(false);
				mDialog.setMax(mFiles.size());
				mDialog.setProgress(0);
				mDialog.show();
			}

			@Override
			protected Exception doInBackground(Void... params)
			{
				int progress = 0;

				for(File file : mFiles)
				{
					try
					{
						Backup.encrypt(context, file, key);
					}
					catch(IOException | ZipException e)
					{
						Log.w(TAG, e);
						return e;
					}

					mDialog.setProgress(progress++);
				}

				return null;
			}

			@Override
			protected void onCancelled()
			{
				super.onCancelled();
			}

			@Override
			protected void onPostExecute(Exception e)
			{
				if(mDialog != null)
				{
					mDialog.dismiss();
					mDialog = null;
				}

				if(e != null)
					Util.showExceptionDialog(mDialog.getContext(), e);
				else if(callback != null)
					callback.run();

			}
		}.execute();
	}

	private static String passwordToKey(String password)
	{
		if(password.length() == 0)
			return null;

		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update("RxDroid".getBytes());
			return Base64.encodeToString(md.digest(password.getBytes("UTF-8")), Base64.NO_WRAP);
		}
		catch(NoSuchAlgorithmException|UnsupportedEncodingException e)
		{
			throw new WrappedCheckedException(e);
		}
	}

	public static class PasswordDialog extends AlertDialog implements
			DialogInterface.OnShowListener, TextWatcher, View.OnClickListener
	{
		private final Context mContext;
		private final boolean mCreateBackup;

		private Button mPosBtn;
		private TextView mMessage;
		private EditText mPw;
		private EditText mPwRepeat;
		private CheckBox mUseForAll;
		private String mOldKey;

		private Runnable mBackupCallback;

		public PasswordDialog(Context context) {
			this(context, false);
		}

		public PasswordDialog(Context context, boolean createBackup)
		{
			super(context);
			mContext = context;
			mCreateBackup = createBackup;

			setView(getLayoutInflater().inflate(R.layout.dialog_pw, null));
			setButton(BUTTON_NEGATIVE, mContext.getString(android.R.string.cancel), (Message) null);
			setButton(BUTTON_POSITIVE, mContext.getString(android.R.string.ok), (Message) null);
			setOnShowListener(this);
		}

		public void setBackupSuccessCallback(Runnable callback) {
			mBackupCallback = callback;
		}

		@Override
		public void onClick(View v)
		{
			if(v != mPosBtn)
				return;

			final String pw = mPw.getText().toString();
			if(pw.equals(mPwRepeat.getText().toString()))
			{
				final String key = !pw.equals(mOldKey) ? passwordToKey(pw) : mOldKey;
				if(key != null && mUseForAll.isChecked())
					Settings.putString(Settings.Keys.BACKUP_KEY, key);
				else
					Settings.remove(Settings.Keys.BACKUP_KEY);

				if(mCreateBackup)
				{
					try
					{
						Backup.createBackup(null, key);
						if(mBackupCallback != null)
							mBackupCallback.run();
					}
					catch(ZipException e)
					{
						Util.showExceptionDialog(mContext, e);
					}
				}
				else if(key != null)
				{
					encryptAll(mContext, key, mBackupCallback);
				}

				dismiss();
			}
			else
			{
				mPwRepeat.setText("");
				mPwRepeat.setError(mContext.getText(R.string._msg_pw_error));
			}
		}

		@Override
		public void onShow(DialogInterface dialog)
		{
			if(dialog != this)
				return;

			final boolean isInitialSetting = !Settings.contains(Settings.Keys.BACKUP_KEY);
			mOldKey = Settings.getString(Settings.Keys.BACKUP_KEY, "");

			mPosBtn = getButton(BUTTON_POSITIVE);
			mPosBtn.setOnClickListener(this);
			mPosBtn.setEnabled(!isInitialSetting || mCreateBackup);

			mPw = (EditText) findViewById(R.id.pw_new);
			// It doesn't really matter what we put here. The idea is that if a password is
			// already set, we allow the user to remove the password protection by setting
			// a zero-length password
			mPw.setText(mOldKey);
			mPw.addTextChangedListener(this);

			mPwRepeat = (EditText) findViewById(R.id.pw_repeat);
			mPwRepeat.setText(mOldKey);
			mPwRepeat.addTextChangedListener(this);

			mUseForAll = (CheckBox) findViewById(R.id.checkbox);
			mMessage = (TextView) findViewById(R.id.message);

			if(!mCreateBackup)
			{
				mMessage.setText(R.string._msg_change_backup_pw);
				mUseForAll.setChecked(true);
				mUseForAll.setEnabled(false);
			}
			else
				mUseForAll.setChecked(true);
		}

		@Override
		public void afterTextChanged(Editable s)
		{
			mPwRepeat.setError(null);
			mPosBtn.setEnabled(mPw.length() == mPwRepeat.length());
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	}

	private static final FilenameFilter FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String filename)
		{
			return filename.endsWith(".rxdbak");
		}
	};

	private static final String[] FILES = {
			"databases/" + DatabaseHelper.DB_NAME,
			"shared_prefs/at.jclehner.rxdroid" + (BuildConfig.DEBUG ? ".debug" : "") + "_preferences.xml",
			"shared_prefs/showcase_internal.xml"
	};
}
