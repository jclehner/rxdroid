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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import java.io.File;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

import at.jclehner.androidutils.RefString;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;


public class RxDroid extends Application
{
	private static final String TAG = RxDroid.class.getSimpleName();

	private static WeakHashMap<Activity, Boolean> sActivityVisibility =
			new WeakHashMap<Activity, Boolean>();

	private static long sUnlockedTime = 0;
	private static volatile WeakReference<Context> sContextRef;
	private static volatile Handler sHandler;

	@Override
	public void onCreate()
	{
		setContext(getApplicationContext());

		Thread.setDefaultUncaughtExceptionHandler(sExceptionHandler);

		DoseEventJanitor.registerSelf();
		Database.registerEventListener(sNotificationUpdater);

		// We can't call Settings.init() here, because this overwrites the
		// shared preferences if this class is instantiated by the Android
		// backup framework.
		Components.onCreate(getContext(), Components.NO_DATABASE_INIT | Components.NO_SETTINGS_INIT);

		super.onCreate();
	}

	public static void setContext(Context context)
	{
		sContextRef = new WeakReference<Context>(context);
		sHandler = new Handler(context.getMainLooper());
	}

	/**
	 * Calls {@link GlobalContext#get(boolean)} with <code>allowNullContext=false</code>.
	 */
	public static Context getContext() {
		return getContext(false);
	}

	public static Context getContext(boolean allowNullContext)
	{
		final Context c = sContextRef.get();
		if(c == null && !allowNullContext)
			throw new IllegalStateException("Context is null");
		return c;
	}

	public static PackageInfo getPackageInfo()
	{
		try
		{
			final PackageManager pm = getContext().getPackageManager();
			return pm.getPackageInfo(getContext().getApplicationInfo().packageName, 0);
		}
		catch(PackageManager.NameNotFoundException e)
		{
			throw new WrappedCheckedException(e);
		}
	}

	public static void toastShort(int textResId) {
		toast(textResId, Toast.LENGTH_SHORT);
	}

	public static void toastLong(int textResId) {
		toast(textResId, Toast.LENGTH_LONG);
	}

	public static void runInMainThread(Runnable r) {
		sHandler.post(r);
	}

	public static LocalBroadcastManager getLocalBroadcastManager() {
		return LocalBroadcastManager.getInstance(getContext());
	}

	public static void doStartActivity(Intent intent) {
		getContext().startActivity(intent);
	}

	public static void doSendBroadcast(Intent intent) {
		getContext().sendBroadcast(intent);
	}

	public static String getQuantityString(int id, int quantity, Object... formatArgs)
	{
		final Object[] newArgs = new Object[formatArgs.length + 1];
		newArgs[0] = quantity;
		System.arraycopy(formatArgs, 0, newArgs, 1, formatArgs.length);
		return getContext().getResources().getQuantityString(id, quantity, newArgs);
	}

	public static String getQuantityString(int id, int quantity) {
		return getContext().getResources().getQuantityString(id, quantity, quantity);
	}

	public static boolean isLocked()
	{
		final int timeoutSeconds = Settings.getInt(Settings.Keys.LOCKSCREEN_TIMEOUT, 0);
		if(timeoutSeconds == 0)
			return sUnlockedTime == 0;

		final long now = System.currentTimeMillis();
		if(sUnlockedTime > now)
			return true;

		final long diffSeconds = (now - sUnlockedTime) / 1000;
		return diffSeconds >= timeoutSeconds;
	}

	public static void unlock() {
		sUnlockedTime = System.currentTimeMillis();
	}

	public static void setIsVisible(Activity activity, boolean isVisible)
	{
		if(sActivityVisibility.containsKey(activity))
			sActivityVisibility.put(activity, isVisible);
	}

	public static boolean isUiVisible()
	{
//		for(Activity activity : sActivityVisibility.keySet())
//		{
//			if(sActivityVisibility.get(activity))
//				return true;
//		}

		return false;
	}

	public static void notifyBackupDataChanged()
	{
		new BackupManager(getContext()).dataChanged();
		//Log.i(TAG, "notifyBackupDataChanged");
	}

	public static long getBootTimestamp()
	{
		try
		{
			// Try to get the method SystemProperties.get(String key, String defValue)

			Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
			Method getMethod = systemPropertiesClass.getMethod("get", String.class, String.class);
			String value = (String) getMethod.invoke(null, "ro.runtime.firstboot", null);
			if(value != null && value.length() != 0)
			{
				long timestamp = Long.parseLong(value);
				if(BuildConfig.DEBUG)
					Log.d(TAG, "ro.runtime.firstboot=" + timestamp);

				return timestamp;
			}
		}
		catch(Exception t)
		{
			Log.w(TAG, t);
		}

		if(BuildConfig.DEBUG)
			Log.d(TAG, "getBootTimestamp: falling back to hackish method");

		return System.currentTimeMillis() - SystemClock.elapsedRealtime();
	}

	public static long getLastUpdateTimestamp()
	{
		final Context context = sContextRef.get();
		final PackageManager pm = context.getPackageManager();
		final String pkgName = context.getPackageName();
		try
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
			{
				final PackageInfo info = pm.getPackageInfo(pkgName, 0);
				return info.lastUpdateTime;
			}
			else
			{
				final ApplicationInfo info = pm.getApplicationInfo(pkgName, 0);
				return new File(info.sourceDir).lastModified();
			}
		}
		catch(PackageManager.NameNotFoundException e)
		{
			Log.w(TAG, e);
		}

		return 0;
	}

	public static Intent getErrorEmailIntent(Context context, String tag, Throwable t, boolean includeLogcat)
	{
		final File dir = true ? new File(context.getCacheDir(), "crash")
				: new File(Environment.getExternalStorageDirectory(), "RxDroid");

		dir.mkdirs();

		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "josephclehner+rxdroid@gmail.com" });
		intent.putExtra(Intent.EXTRA_SUBJECT, "[" + tag + "] " + Version.get());

		final StringBuilder text = new StringBuilder();
		final ArrayList<Uri> attachments = new ArrayList<Uri>();

		final String deviceInfo =
				"MANUFACTURER: " + Build.MANUFACTURER + "\n" +
				"PRODUCT     : " + Build.PRODUCT + "\n" +
				"MODEL       : " + Build.MODEL + "\n" +
				"DEVICE      : " + Build.DEVICE + "\n" +
				"DISPLAY     : " + Build.DISPLAY + "\n" +
				"RELEASE     : " + Build.VERSION.RELEASE + "\n" +
				"SDK_INT     : " + Build.VERSION.SDK_INT + "\n" +
				"LOCALE      : " + Locale.getDefault().toString() + "\n" +
				"PID         : " + android.os.Process.myPid() + "\n"
		;

		attachOrAppend(context, "DEVICE INFO", deviceInfo, new File(dir, "info.txt"), attachments, text);

		if(t != null)
		{
			attachOrAppend(context, "STACK TRACE", Util.stackTraceToString(t), new File(dir, "stacktrace.txt"),
					attachments, text);
		}

		if(includeLogcat)
		{
			attachOrAppend(context, "LOGCAT", Util.runCommand("logcat -d -v time"), new File(dir, "logcat.txt"),
					attachments, text);
		}

		intent.putExtra(Intent.EXTRA_TEXT, text.toString());

		if(attachments.size() != 0)
		{
			intent.setAction(Intent.ACTION_SEND_MULTIPLE);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);

			final List<ResolveInfo> rInfos = context.getPackageManager().queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);

			for(ResolveInfo rInfo : rInfos)
			{
				final String pkgName = rInfo.activityInfo.packageName;

				for(Uri uri : attachments)
					context.grantUriPermission(pkgName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
			}
		}
		else
			text.append("================== USER MESSAGE ==================\n");

		return Intent.createChooser(intent, context.getString(R.string._btn_report));
	}

	private static boolean attachOrAppend(Context context, String header, String data, File file, ArrayList<Uri> outAttachments,
			StringBuilder outText)
	{
		if(Util.writeToFile(file, data))
		{
			//file.setReadable(true, false);
			outAttachments.add(FileProvider.getUriForFile(
					context, "at.jclehner.rxdroid.fileprovider", file));
			return true;
		}
		else
		{
			outText.append(
					"===================== " + header + " ====================\n" +
							data + "\n"
			);

			return false;
		}
	}

	private static void toast(final int textResId, final int duration)
	{
		runInMainThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getContext(), textResId, duration).show();
			}
		});
	}

	private static final Database.OnChangeListener sNotificationUpdater = new Database.OnChangeListener() {

		@Override
		public void onEntryUpdated(Entry entry, int flags) {
			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(entry instanceof DoseEvent);
		}

		@Override
		public void onEntryDeleted(Entry entry, int flags) {
			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(false);
		}

		@Override
		public void onEntryCreated(Entry entry, int flags) {
			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(entry instanceof DoseEvent);
		}
	};

	private static final UncaughtExceptionHandler sExceptionHandler = new UncaughtExceptionHandler() {

		@Override
		public void uncaughtException(Thread thread, final Throwable ex)
		{
			ex.printStackTrace();

			final int pid = android.os.Process.myPid();

			final Intent intent = new Intent("at.jclehner.rxdroid.REPORT_EXCEPTION");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("exception", ex);
			intent.putExtra("pid", pid);

			doStartActivity(intent);
			android.os.Process.killProcess(pid);
			System.exit(1);
		}
	};

	public static class ExceptionHandlerActivity extends Activity
	{
		@Override
		protected void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			requestWindowFeature(Window.FEATURE_NO_TITLE);

			final Throwable t = (Throwable) getIntent().getSerializableExtra("exception");
			final int pid = getIntent().getIntExtra("pid", 0);

			final String exClassName = t.getClass().getName();
			final CharSequence msg = Html.fromHtml(
					"<p>" + Util.escapeHtml(RefString.resolve(this, R.string._msg_uncaught_exception)) +
							"</p><p><tt>" + exClassName + "</tt></p>");

			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setTitle(R.string._title_error);
			ab.setIcon(R.drawable.ic_launcher);
			ab.setMessage(msg);
			ab.setCancelable(false);
			ab.setNegativeButton(android.R.string.cancel, null);
			ab.setPositiveButton(R.string._btn_report, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if(which == Dialog.BUTTON_POSITIVE)
						startActivity(getErrorEmailIntent(getApplicationContext(), "BUG", t, true));

					finish();
				}
			});

			ab.show();
		}
	}}
