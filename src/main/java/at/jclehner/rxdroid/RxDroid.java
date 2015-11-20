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

import android.support.v7.app.AppCompatActivity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.*;
import android.support.v4.content.IntentCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/*import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;*/

import java.io.File;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.WeakHashMap;

import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.util.Components;
//import at.jclehner.rxdroid.util.EmailIntentSender;
import at.jclehner.rxdroid.util.WrappedCheckedException;

/*@ReportsCrashes(
		formKey = "",
		mailTo = "josephclehner+rxdbug@gmail.com",
		mode = ReportingInteractionMode.DIALOG,
		//resDialogTitle = R.string._title_error,
		//resDialogIcon = android.R.drawable.ic_dialog_alert,
		resDialogText = R.string._msg_crash_dialog_text,
		resDialogReportButtonText = R.string._btn_report,
		resDialogCancelButtonText = android.R.string.ok,
		switchDialogButtonPositions = true
		//resDialogCommentPrompt = R.string._msg_comment
)*/
public class RxDroid extends Application
{
	private static final String TAG = RxDroid.class.getSimpleName();

	private static WeakHashMap<AppCompatActivity, Boolean> sActivityVisibility =
			new WeakHashMap<AppCompatActivity, Boolean>();

	private static long sUnlockedTime = 0;
	private static volatile WeakReference<Context> sContextRef;
	private static volatile Handler sHandler;

	@Override
	public void onCreate()
	{
		setContext(getApplicationContext());

		DoseEventJanitor.registerSelf();
		Database.registerEventListener(sNotificationUpdater);

		// We can't call Settings.init() here, because this overwrites the
		// shared preferences if this class is instantiated by the Android
		// backup framework.
		Components.onCreate(getContext(), Components.NO_DATABASE_INIT | Components.NO_SETTINGS_INIT);

		super.onCreate();

		//ACRA.init(this);
		//ACRA.getErrorReporter().setReportSender(new EmailIntentSender(this));
	}

	public static void setContext(Context context)
	{
		sContextRef = new WeakReference<Context>(context);
		sHandler = new Handler(context.getMainLooper());
	}

	/**
	 * Calls {@link  GlobalContext#get(boolean)} with <code>allowNullContext=false</code>.
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

	public static void setIsVisible(AppCompatActivity activity, boolean isVisible)
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

	public static void forceRestart()
	{
		toastLong(R.string._toast_theme_changed);

		final Context c = getContext();
		final Intent intent = c.getPackageManager().getLaunchIntentForPackage(
				c.getPackageName());

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		final AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000,
				PendingIntent.getActivity(c, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
		System.exit(0);
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

	public static Intent getLaunchIntent()
	{
		final Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(
				getPackageInfo().packageName);

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		return intent;
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
}
