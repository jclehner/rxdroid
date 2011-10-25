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

package at.caspase.rxdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.db.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.debug.SleepState;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Hasher;

/**
 * Primary notification service.
 * 
 * @author Joseph Lehner
 * 
 */
public class NotificationService extends Service implements
		OnDatabaseChangedListener, OnSharedPreferenceChangeListener
{
	public static final String EXTRA_RESTART_FLAGS = "restart_flags";

	public static final int RESTART_FORCE = 64 << 1;
	public static final int RESTART_DELAYFIRST = 64 << 2;

	private static final String TAG = NotificationService.class.getSimpleName();

	private static final int SNOOZE_DISABLED = 0;
	private static final int SNOOZE_AUTO = 1;
	private static final int SNOOZE_MANUAL = 2;

	private int mSnoozeType = SNOOZE_DISABLED;

	private Intent mNotificationIntent;
	private SharedPreferences mSharedPreferences;

	private NotificationManager mNotificationManager;
	private String[] mNotificationMessages;
	private int mLastNotificationHash = 0;

	private Thread mThread;
	private static NotificationService sInstance = null;

	private Object mSnoozeLock;

	@Override
	public void onCreate()
	{
		super.onCreate();
		setInstance(this);

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		clearAllNotifications();

		mNotificationIntent = new Intent(Intent.ACTION_VIEW);
		mNotificationIntent.setClass(getApplicationContext(), DrugListActivity.class);
		mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mNotificationIntent.putExtra(DrugListActivity.EXTRA_STARTED_BY_NOTIFICATION, true);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

		mSnoozeType = Integer.parseInt(mSharedPreferences.getString("snooze_type", "0"));

		Database.registerOnChangedListener(this);

		GlobalContext.set(getApplicationContext());
		Database.load();
	}

	/**
	 * Starts the service if it has not been started yet.
	 * <p>
	 * You can pass options to {@link #restartThread(int)} by setting flags from
	 * <code>RESTART_*</code> in <code>EXTRA_RESTART_FLAGS</code>.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// super.onStartCommand(intent, flags, startId);

		Log.d(TAG, "onStartCommand: intent=" + intent);

		restartThread(intent != null ? intent.getIntExtra(EXTRA_RESTART_FLAGS, 0) : 0);
		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		setInstance(null);

		stopThread();
		cancelAllNotifications(true);
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		Database.unregisterOnChangedListener(this);

		Log.d(TAG, "onDestroy");
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}

	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		restartThread(flags | RESTART_FORCE);
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		restartThread(flags | RESTART_FORCE);
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags)
	{
		restartThread(flags | RESTART_FORCE);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key)
	{
		if(key.startsWith("time_") || key.startsWith("debug_") || key.startsWith("snooze_"))
		{
			Log.d(TAG, "Preference key " + key + " changed, restarting thread");
			restartThread(RESTART_FORCE);
		}
		else if(key.equals("snooze_type"))
			mSnoozeType = Integer.parseInt(mSharedPreferences.getString("snooze_type", "0"), 10);
		else
			Log.d(TAG, "Ignoring preference change of " + key);
	}

	public static void requestSnooze()
	{
		if(sInstance != null && sInstance.mSnoozeLock != null && sInstance.mSnoozeType == SNOOZE_MANUAL)
		{
			try
			{
				synchronized (sInstance.mSnoozeLock) {
					sInstance.mSnoozeLock.notify();
				}
			}
			catch(IllegalStateException e)
			{
				Log.w(TAG, "requestSnooze: " + e.getMessage());
			}
		}
	}

	/**
	 * Check if the service is currently running.
	 * 
	 * Note that this function might return <code>false</code> even if the
	 * service is running (from an Android point of view) when the service
	 * thread is not currently running.
	 * 
	 * @return <code>true</code> if the service thread is running.
	 */
	public static boolean isRunning()
	{
		if(sInstance == null)
			return false;

		return sInstance.isThreadRunning();
	}

	private synchronized boolean isThreadRunning()
	{
		if(mThread == null)
			return false;

		boolean isAlive = mThread.isAlive();
		boolean isInterrupted = mThread.isInterrupted();

		Log.d(TAG, "isThreadRunning: ");
		Log.d(TAG, "  mThread.isAlive(): " + isAlive);
		Log.d(TAG, "  mThread.isInterrupted(): " + isInterrupted);

		// TODO does isAlive() imply !isInterrupted() ?
		return mThread.isAlive() /* && !mThread.isInterrupted() */;
	}

	/**
	 * (Re)starts the worker thread.
	 * <p>
	 * Calling this function will cause the service to consult the DB in order
	 * to determine when the next notification should be posted. Currently, the
	 * worker thread is restarted when <em>any</em> database changes occur (see
	 * DatabaseWatcher) or when the user opens the app.
	 * 
	 * @param forceRestart
	 *            if set to <code>true</code> forces the thread to restart, even
	 *            if it was running.
	 * @param delayFirstNotification
	 *            if set to <code>true</code> the first notification will be
	 *            somewhat delayed.
	 */
	private synchronized void restartThread(int flags)
	{
		if((flags & FLAG_IGNORE) != 0)
			return;
		
		final boolean forceRestart = (flags & RESTART_FORCE) != 0;
		final boolean delayFirstNotification;
		
		if(forceRestart)
			delayFirstNotification = true;
		else
			delayFirstNotification = (flags & RESTART_DELAYFIRST) != 0;
				
		Log.d(TAG, "restarThread(" + flags + ")");		
		Log.d(TAG, "  forceRestart=" + forceRestart + ", delayFirstNotification=" + delayFirstNotification);
		Log.d(TAG, "  mSnoozeType=" + mSnoozeType);
		
		final boolean wasRunning = isThreadRunning();
		if(wasRunning)
		{
			if(!forceRestart)
			{
				Log.d(TAG, "  ignoring service restart request");			
				return;
			}
						
			mThread.interrupt();
		}

		Log.d(TAG, "  wasRunning=" + wasRunning);
		
		mSnoozeLock = new Object();		
		mThread = new Thread(new Runnable() {

			@Override
			public void run()
			{
				/**
				 * - on start, clear all notifications
				 *
				 * - collect forgotten intakes & display notifications if
				 * necessary - if a dose time is active, collect pending intakes
				 * & display notifications if neccessary. do so every N minutes
				 * (as specified by the snooze time), until the next dose time
				 * becomes active
				 *
				 * - if the active dose time is TIME_MORNING, also check supply
				 * levels & display notifications, if applicable
				 *
				 * - if no dose time is active, sleep until the start of the
				 * next dose time
				 *
				 */

				clearAllNotifications();
				checkSupplies(true);
				
				final Preferences settings = Preferences.instance();
				boolean doDelayFirstNotification = delayFirstNotification;				
				
				try
				{
					while(true)
					{
						final Calendar time = DateTime.now();
						
						final int activeDoseTime = settings.getActiveDoseTime(time);
						final int nextDoseTime = settings.getNextDoseTime(time);
						final int lastDoseTime = (activeDoseTime == -1) ? (nextDoseTime - 1) : (activeDoseTime - 1);
						
						final Calendar date = settings.getActiveDate(time);						
						mNotificationIntent.putExtra(DrugListActivity.EXTRA_DAY, date);
						
						Log.d(TAG, "date: " + DateTime.toString(date));						
						Log.d(TAG, "times: active=" + activeDoseTime + ", next=" + nextDoseTime + ", last=" + lastDoseTime);
						
						if(lastDoseTime >= 0)
							checkForForgottenIntakes(date, lastDoseTime);

						if(activeDoseTime == -1)
						{
							long sleepTime = settings.getMillisUntilDoseTimeBegin(time, nextDoseTime);

							Log.d(TAG, "sleeping " + new DumbTime(sleepTime)  +" until beginning of dose time " + nextDoseTime);
							
							sleep(sleepTime);
							doDelayFirstNotification = false;

							if(settings.getActiveDoseTime() != nextDoseTime)
								Log.e(TAG, "unexpected dose time, expected " + nextDoseTime);

							continue;
						}
						else if(activeDoseTime == Drug.TIME_MORNING)
						{
							cancelNotification(R.id.notification_intake_forgotten);
							checkSupplies(false);
						}
						
						final int pendingIntakeCount = countOpenIntakes(date, activeDoseTime);

						Log.d(TAG, "Pending intakes: " + pendingIntakeCount);

						if(pendingIntakeCount != 0)
						{
							if(doDelayFirstNotification && wasRunning)
							{
								doDelayFirstNotification = false;
								Log.d(TAG, "Delaying first notification");
								sleep(Constants.NOTIFICATION_INITIAL_DELAY);
							}

							final String contentText = Integer.toString(pendingIntakeCount);
							final long snoozeTime = settings.getSnoozeTime();
							
							do
							{								
								postNotification(R.id.notification_intake_pending, Notification.DEFAULT_ALL, 
										contentText, mSnoozeType == SNOOZE_AUTO);
								
								final Calendar now = DateTime.now();
								final long millisUntilDoseTimeEnd = settings.getMillisUntilDoseTimeEnd(now, activeDoseTime);
								
								if(mSnoozeType == SNOOZE_DISABLED || millisUntilDoseTimeEnd < snoozeTime)
									break;
								
								if(mSnoozeType == SNOOZE_MANUAL)
								{
									final long waitMillis = millisUntilDoseTimeEnd - snoozeTime;
									
									SleepState.INSTANCE.onEnterSleep(waitMillis);
									synchronized(mSnoozeLock) {
										mSnoozeLock.wait(waitMillis);
									}
									SleepState.INSTANCE.onFinishedSleep();
									cancelNotification(R.id.notification_intake_pending);
								}
																
								sleep(snoozeTime);
								
							} while(true);
						}						

						final long millisUntilDoseTimeEnd = settings.getMillisUntilDoseTimeEnd(DateTime.now(), activeDoseTime);
						if(millisUntilDoseTimeEnd > 0)
						{
							Log.d(TAG, "Sleeping " + millisUntilDoseTimeEnd + "ms until end of dose time " + activeDoseTime);
							sleep(millisUntilDoseTimeEnd);
						}

						cancelNotification(R.id.notification_intake_pending);
						checkForForgottenIntakes(date, activeDoseTime);
					}
				}
				catch(InterruptedException e)
				{
					Log.d(TAG, "Thread interrupted, exiting...");
				}
				catch(Exception e)
				{
					Log.e(TAG, "Service died due to exception", e);
					stopSelf();
					writeCrashLog(e);
				}
				finally
				{
					//cancelAllNotifications();
				}
			}
		}, "Service Thread");

		mThread.start();
	}

	private synchronized void stopThread()
	{
		if(mThread != null)
			mThread.interrupt();

		mThread = null;
	}

	private int countOpenIntakes(Calendar date, int doseTime)
	{
		int count = 0;

		// Log.d(TAG, "  countOpenIntakes: date=" + DateTime.toString(date) +
		// ", doseTime=" + doseTime);

		for(Drug drug : Database.getDrugs())
		{
			if(drug.isActive())
			{
				final List<Intake> intakes = Database.findIntakes(drug, date, doseTime);
				final Fraction dose = drug.getDose(doseTime);

				if(intakes.isEmpty() && drug.hasDoseOnDate(date) && dose.compareTo(0) != 0)
				{
					Log.d(TAG, "  adding " + drug.getName());
					++count;
				}
			}
		}

		return count;
	}

	private int countForgottenIntakes(Calendar date, int lastDoseTime)
	{
		// Log.d(TAG, "countForgottenIntakes: date=" + DateTime.toString(date) +
		// ", lastDoseTime=" + lastDoseTime);

		final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };

		int count = 0;

		for(int doseTime : doseTimes)
		{
			count += countOpenIntakes(date, doseTime);

			if(doseTime == lastDoseTime)
				break;
		}

		return count;
	}

	private void checkForForgottenIntakes(Calendar date, int lastDoseTime)
	{
		int count = countForgottenIntakes(date, lastDoseTime);

		Log.d(TAG, "Forgotten intakes: " + count);

		if(count != 0)
		{
			final String contentText = Integer.toString(count);
			postNotification(R.id.notification_intake_forgotten, Notification.DEFAULT_LIGHTS, contentText);
		}
		else
			cancelNotification(R.id.notification_intake_forgotten);
	}

	private void checkSupplies(boolean doEnqueueNotification)
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final int minDays = Integer.parseInt(prefs.getString("num_min_supply_days", "7"), 10);
		final List<Drug> drugsWithLowSupply = getAllDrugsWithLowSupply(minDays);

		if(!drugsWithLowSupply.isEmpty())
		{
			final String firstDrugName = drugsWithLowSupply.get(0).getName();
			final String contentText;

			if(drugsWithLowSupply.size() == 1)
				contentText = getString(R.string._msg_low_supply_single, firstDrugName);
			else
				contentText = getString(R.string._msg_low_supply_multiple, firstDrugName, drugsWithLowSupply.size() - 1);

			if(!doEnqueueNotification)
				postNotification(R.id.notification_low_supplies, Notification.DEFAULT_LIGHTS, contentText);
			else
				enqueueNotification(R.id.notification_low_supplies, contentText);
		}
		else
			cancelNotification(R.id.notification_low_supplies);
	}

	private List<Drug> getAllDrugsWithLowSupply(int minDays)
	{
		final List<Drug> drugsWithLowSupply = new ArrayList<Drug>();

		for(Drug drug : Database.getDrugs())
		{
			// refill size of zero means ignore supply values
			if (drug.getRefillSize() == 0)
				continue;

			final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };
			double dailyDose = 0;

			for(int doseTime : doseTimes)
			{
				final Fraction dose = drug.getDose(doseTime);
				if(dose.compareTo(0) != 0)
					dailyDose += dose.doubleValue();
			}

			if(dailyDose != 0)
			{
				final double correctionFactor = drug.getSupplyCorrectionFactor();
				final double currentSupply = drug.getCurrentSupply().doubleValue() * correctionFactor;

				Log.d(TAG, "Supplies left for " + drug + ": " + currentSupply / dailyDose + " with correctionFactor=" + correctionFactor);

				if(Double.compare(currentSupply / dailyDose, (double) minDays) == -1)
					drugsWithLowSupply.add(drug);
			}
		}

		return drugsWithLowSupply;
	}

	private void enqueueNotification(int id, String message) {
		mNotificationMessages[notificationIdToIndex(id)] = message;
	}

	private void postNotification(int id, int defaults, String message) {
		postNotification(id, defaults, message, false);
	}

	private void postNotification(int id, int defaults, String message, boolean forceNotification)
	{
		enqueueNotification(id, message);
		postAllNotifications(defaults, forceNotification);
	}

	private void postAllNotifications(int defaults, boolean forceNotifcation)
	{
		int notificationCount;

		if((notificationCount = getNotificationCount()) == 0)
		{
			cancelAllNotifications(true);
			return;
		}

		Log.d(TAG, "postAllNotifications: notificationCount=" + notificationCount);
		for(String msg : mNotificationMessages)
			Log.d(TAG, "  msg=" + msg);

		final String bullet;

		if (mNotificationMessages[2] != null && notificationCount != 1)
		{
			// we have 2 notifications, use bullets!
			bullet = Constants.NOTIFICATION_BULLET;
		}
		else
			bullet = "";

		StringBuilder msgBuilder = new StringBuilder();

		final String doseMsgPending = mNotificationMessages[0];
		final String doseMsgForgotten = mNotificationMessages[1];

		int stringId = -1;

		if(doseMsgPending != null && doseMsgForgotten != null)
			stringId = R.string._msg_doses_fp;
		else if(doseMsgPending != null)
			stringId = R.string._msg_doses_p;
		else if(doseMsgForgotten != null)
			stringId = R.string._msg_doses_f;

		if(stringId != -1)
			msgBuilder.append(bullet + getString(stringId, doseMsgForgotten, doseMsgPending));

		final String doseMsgLowSupply = mNotificationMessages[2];

		if(doseMsgLowSupply != null)
		{
			if(stringId != -1)
			{
				// if we appended this line-break in the append() call above,
				// the layout would look
				// messed up in case there was no "low supply" message
				msgBuilder.append("\n");
			}

			msgBuilder.append(bullet + doseMsgLowSupply);
		}

		final RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
		views.setTextViewText(R.id.stat_title, getString(R.string._title_notifications));
		views.setTextViewText(R.id.stat_text, msgBuilder.toString());
		// views.setTextViewText(R.id.stat_time, new
		// SimpleDateFormat("HH:mm").format(DateTime.now()));
		views.setTextViewText(R.id.stat_time, "");

		final Notification notification = new Notification();
		notification.icon = R.drawable.ic_stat_pill;
		notification.tickerText = getString(R.string._msg_new_notification);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notification.defaults = Preferences.instance().filterNotificationDefaults(defaults);
		notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, mNotificationIntent, 0);
		notification.contentView = views;
		if(notificationCount > 1)
			notification.number = notificationCount;

		final int notificationHash = getNotificationHashCode(notification, msgBuilder.toString());

		if(mLastNotificationHash == notificationHash && !forceNotifcation)
			notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		else
			mLastNotificationHash = notificationHash;

		mNotificationManager.notify(R.id.notification, notification);
	}

	private void cancelNotification(int id) {
		postNotification(id, Notification.DEFAULT_LIGHTS, null);
	}

	private void cancelAllNotifications(boolean resetHash)
	{
		clearAllNotifications(true);
		mNotificationManager.cancel(R.id.notification);
		if(resetHash)
			mLastNotificationHash = 0;
	}

	private void clearAllNotifications()
	{
		clearAllNotifications(true);
	}

	private void clearAllNotifications(boolean zapMessages)
	{
		// mNotificationManager.cancel(R.id.notification);
		// mLastNotificationHash = 0;
		if(zapMessages)
			mNotificationMessages = new String[3];
	}

	private int getNotificationCount()
	{
		int count = 0;

		for(String msg : mNotificationMessages)
		{
			if(msg != null)
				++count;
		}

		return count;
	}

	private static int notificationIdToIndex(int id)
	{
		switch(id)
		{
			case R.id.notification_intake_pending:
				return 0;

			case R.id.notification_intake_forgotten:
				return 1;

			case R.id.notification_low_supplies:
				return 2;
		}

		throw new IllegalArgumentException();
	}

	private static int getNotificationHashCode(Notification n, String msg)
	{
		final int contentViewLayoutId = (n.contentView != null) ? n.contentView.getLayoutId() : 0;

		final Hasher hasher = new Hasher();

		hasher.hash(n.audioStreamType);
		hasher.hash(n.contentIntent != null);
		hasher.hash(contentViewLayoutId);
		hasher.hash(n.defaults);
		hasher.hash(n.deleteIntent != null);
		hasher.hash(n.fullScreenIntent != null);
		hasher.hash(n.icon);
		hasher.hash(n.iconLevel);
		hasher.hash(n.ledARGB);
		hasher.hash(n.ledOffMS);
		hasher.hash(n.ledOnMS);
		hasher.hash(n.number);
		hasher.hash(n.sound);
		hasher.hash(n.tickerText);
		hasher.hash(n.vibrate);
		hasher.hash(msg);

		return hasher.getHashCode();
	}

	private static void setInstance(NotificationService instance) {
		sInstance = instance;
	}

	private static void sleep(long time) throws InterruptedException
	{
		if(time > 0)
		{
			SleepState.INSTANCE.onEnterSleep(time);
			try
			{
				Thread.sleep(time);
			}
			finally
			{
				SleepState.INSTANCE.onFinishedSleep();
			}
		}
		else
			Log.d(TAG, "sleep: ignoring time of " + time);
	}

	private void writeCrashLog(Exception cause)
	{
		long timestamp = System.currentTimeMillis() / 1000;
		File crashLog = new File(getExternalFilesDir(null), "crash-" + timestamp + ".log");

		OutputStream os = null;

		try
		{
			os = new FileOutputStream(crashLog);
			
			StringBuilder sb = new StringBuilder();
			sb.append("Time: " + DateTime.toString(DateTime.now()) + "\n\n");
			sb.append("Cause: " + cause + "\n");
			
			for(StackTraceElement e : cause.getStackTrace())
				sb.append("  " + e.getFileName() + ":" + e.getLineNumber() + ", in " + e.getMethodName() + "\n");
			
			sb.append("\n\n" + "Closing thread...");
			
			os.write(sb.toString().getBytes());
			os.close();
		}
		catch (IOException e)
		{
			Log.e(TAG, "Error writing crash log", e);
		}
		finally
		{
			try
			{
				if (os != null)
					os.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, "Error writing crash log", e);
			}
		}
	}

	private class PendingNotificationsThreadRunnable implements Runnable
	{
		private int mPendingCount = 0;
		private Object mWaitLock;

		public PendingNotificationsThreadRunnable() {
			mWaitLock = new Object();
		}

		public void setPendingCount(int pendingCount)
		{
			mPendingCount = pendingCount;
		}

		public void snooze()
		{
			if (mSnoozeType == SNOOZE_MANUAL)
			{
				cancelNotification(R.id.notification_intake_pending);
				synchronized (mWaitLock)
				{
					mWaitLock.notify();
				}
			}
		}

		@Override
		public void run()
		{
			Thread.currentThread().setName("PendingNotificationsThread");

			if (mPendingCount == 0)
				return;

			final long snoozeTime = Preferences.instance().getSnoozeTime();
			final boolean doAutoSnooze = mSnoozeType == SNOOZE_AUTO;

			do
			{
				final String contentText = Integer.toString(mPendingCount);
				postNotification(R.id.notification_intake_pending,
						Notification.DEFAULT_ALL, contentText, doAutoSnooze);

				try
				{
					switch (mSnoozeType)
					{
						case SNOOZE_AUTO:
							mWaitLock.wait(snoozeTime);
							break;

						case SNOOZE_MANUAL:
							mWaitLock.wait();
							sleep(snoozeTime);
							break;

						default:
							return;
					}
				}
				catch (InterruptedException e)
				{
					Log.d(TAG, "Exiting pending notifications thread");
				}

			} while (true);
		}
	}
}
