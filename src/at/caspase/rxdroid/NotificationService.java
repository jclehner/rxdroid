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
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;
import at.caspase.rxdroid.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Hasher;
import at.caspase.rxdroid.debug.NotificationServiceInfo;

import com.j256.ormlite.android.apptools.OrmLiteBaseService;
import com.j256.ormlite.dao.Dao;

/**
 * Primary notification service.
 *
 * @author Joseph Lehner
 *
 */
public class NotificationService extends Service implements
		OnDatabaseChangedListener, OnSharedPreferenceChangeListener
{
	public static final String EXTRA_FORCE_RESTART = "force_restart";

	public static final int LISTENER_FLAG_DONT_RESTART = 1;
	
	private static final String TAG = NotificationService.class.getSimpleName();

	private Intent mIntent;
	private SharedPreferences mSharedPreferences;

	private NotificationManager mNotificationManager;
	private String[] mNotificationMessages;
	private int mLastNotificationHash = 0;

	private Thread mThread;
	private static NotificationService sInstance = null;
	
	private static NotificationServiceInfo sSvcInfo = new NotificationServiceInfo();

	@Override
	public void onCreate()
	{
		super.onCreate();
		setInstance(this);
		sSvcInfo.isStarted = true;

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		clearAllNotifications();

		mIntent = new Intent(Intent.ACTION_VIEW);
		mIntent.setClass(getApplicationContext(), DrugListActivity.class);
		mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
		Database.registerOnChangedListener(this);		

		ContextStorage.set(getApplicationContext());
		Database.load();
		
		Thread monitorThread = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				while(true)
				{
					Log.d(TAG + ":MonitorThread", sSvcInfo.toString());
					try
					{						
						Thread.sleep(60000);
					}
					catch (InterruptedException e)
					{
						continue;
					}					
				}				
			}
		}, "Monitor Thread");
		
		monitorThread.start();
	}

	/**
	 * Starts the service if it has not been started yet.
	 *
	 * You can force the service thread to restart by passing the <code>EXTRA_FORCE_RESTART=true</code> extra.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		boolean forceRestart = (intent != null && intent.getBooleanExtra(EXTRA_FORCE_RESTART, false));
		restartThread(forceRestart);

		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		setInstance(null);
		sSvcInfo.isStarted = false;
		
		stopThread();
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		Database.unregisterOnChangedListener(this);

		Log.d(TAG, "onDestroy");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreateEntry(Drug drug, int listenerFlags) {
		restartThread(listenerFlags);
	}

	@Override
	public void onDeleteEntry(Drug drug, int listenerFlags) {
		restartThread(listenerFlags);
	}

	@Override
	public void onUpdateEntry(Drug drug, int listenerFlags) {
		restartThread(listenerFlags);
	}

	@Override
	public void onCreateEntry(Intake intake, int listenerFlags) {
		restartThread(listenerFlags);
	}

	@Override
	public void onDeleteEntry(Intake intake, int listenerFlags) {
		restartThread(listenerFlags);
	}

	@Override
	public void onDatabaseDropped() {
		restartThread(true);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key)
	{
		if(key.startsWith("time_") || key.startsWith("debug_"))
			restartThread(true);
		else
			Log.d(TAG, "Ignoring preference change of " + key);
	}

	/**
	 * Check if the service is currently running.
	 *
	 * Note that this function might return <code>false</code> even if the service is
	 * running (from an Android point of view) when the service thread is not currently
	 * running.
	 *
	 * @return <code>true</code> if the service thread is running.
	 */
	public static boolean isRunning()
	{
		if(sInstance == null)
			return false;

		return sInstance.isThreadRunning();
	}
	
	public static NotificationServiceInfo getNotificationServiceInfo() {
		return sSvcInfo;
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
		return mThread.isAlive() && !mThread.isInterrupted();
	}
	
	private void restartThread(int listenerFlags)
	{
		if((listenerFlags & LISTENER_FLAG_DONT_RESTART) == 0)
			restartThread(true);	
	}

	/**
	 * (Re)starts the worker thread.
	 * <p>
	 * Calling this function will cause the service to consult the DB in order
	 * to determine when the next notification should be posted. Currently, the
	 * worker thread is restarted when <em>any</em> database changes occur (see
	 * DatabaseWatcher) or when the user opens the app.
	 *
	 * @param forceRestart Forces the thread to restart, even if it was running.
	 * @param forceSupplyCheck Force a drug-supply check as soon as the thread is up.
	 *     Supply checks are normally done only when <code>activeDoseTime == Drug.TIME_MORNING</code>,
	 *     i.e. only once per day.
	 */
	private synchronized void restartThread(boolean forceRestart)
	{
		final boolean wasRunning = isThreadRunning();

		if(wasRunning)
		{
			if(!forceRestart)
			{
				Log.d(TAG, "Ignoring service restart request");
				return;
			}

			mThread.interrupt();
		}

		Log.d(TAG, "restartThread(" + forceRestart + "): wasRunning=" + wasRunning);

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

				boolean delayFirstNotification = true;
				final Preferences settings = Preferences.instance();

				try
				{
					while(true)
					{
						final Date date = sSvcInfo.date = DateTime.today();
						mIntent.putExtra(DrugListActivity.EXTRA_DAY, date);

						final int activeDoseTime = sSvcInfo.activeDoseTime = settings.getActiveDoseTime();
						final int nextDoseTime = sSvcInfo.nextDoseTime = settings.getNextDoseTime();
						final int lastDoseTime = (activeDoseTime == -1) ? (nextDoseTime - 1) : (activeDoseTime - 1);

						Log.d(TAG, "times: active=" + activeDoseTime + ", next=" + nextDoseTime + ", last=" + lastDoseTime);

						if(lastDoseTime >= 0)
							checkIntakes(date, lastDoseTime);

						if(activeDoseTime == -1)
						{
							long sleepTime = settings.getMillisFromNowUntilDoseTimeBegin(nextDoseTime);

							sleep(sleepTime);
							delayFirstNotification = false;

							if(settings.getActiveDoseTime() != nextDoseTime)
								Log.e(TAG, "Unexpected dose time, expected " + nextDoseTime);

							continue;
						}
						else if(activeDoseTime == Drug.TIME_MORNING)
						{
							cancelNotification(R.id.notification_intake_forgotten);
							checkSupplies(false);
						}

						long millisUntilDoseTimeEnd = settings.getMillisFromNowUntilDoseTimeEnd(activeDoseTime);

						final Set<Intake> pendingIntakes = getAllOpenIntakes(date, activeDoseTime);

						Log.d(TAG, "Pending intakes: " + pendingIntakes.size());

						if(!pendingIntakes.isEmpty())
						{
							if(delayFirstNotification && wasRunning)
							{
								delayFirstNotification = false;
								Log.d(TAG, "Delaying first notification");
								sleep(Constants.NOTIFICATION_INITIAL_DELAY);
							}

							final String contentText = Integer.toString(sSvcInfo.pendingIntakes = pendingIntakes.size());
							final long snoozeTime = settings.getSnoozeTime();

							postNotification(R.id.notification_intake_pending, Notification.DEFAULT_ALL, contentText);

							while(millisUntilDoseTimeEnd > snoozeTime)
							{
								sleep(snoozeTime);
								millisUntilDoseTimeEnd -= snoozeTime;
								postNotification(R.id.notification_intake_pending, Notification.DEFAULT_ALL, contentText);
							}

							Log.d(TAG, "Finished loop");
						}

						if(millisUntilDoseTimeEnd > 0)
						{
							Log.d(TAG, "Sleeping " + millisUntilDoseTimeEnd + "ms until end of dose time " + activeDoseTime);
							sleep(millisUntilDoseTimeEnd);
						}

						cancelNotification(R.id.notification_intake_pending);
						checkIntakes(date, activeDoseTime);
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
					sSvcInfo.isThreadStarted = false;
				}
			}
		}, "Service Thread");

		mThread.start();
		sSvcInfo.isThreadStarted = true;
	}

	private synchronized void stopThread()
	{
		if(mThread != null)
			mThread.interrupt();

		mThread = null;
	}

	private Set<Intake> getAllOpenIntakes(Date date, int doseTime)
	{
		final Set<Intake> openIntakes = new HashSet<Database.Intake>();
		final List<Drug> drugs = Database.getCachedDrugs();

		for(Drug drug : drugs)
		{
			if(drug.isActive())
			{
				final List<Intake> intakes = Database.findIntakes(drug, date, doseTime);

				if(intakes.isEmpty() && drug.hasDoseOnDate(date) && drug.getDose(doseTime).compareTo(0) != 0)
				{
					Log.d(TAG, "getAllOpenIntakes: adding " + drug);
					openIntakes.add(new Intake(drug, date, doseTime));
				}
			}
		}

		return openIntakes;
	}

	private Set<Intake> getAllForgottenIntakes(Date date, int lastDoseTime)
	{
		final Date today = DateTime.today();

		if(date.after(today))
			return Collections.emptySet();

		if(date.before(today))
			lastDoseTime = -1;

		final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };
		final Set<Intake> forgottenIntakes = new HashSet<Database.Intake>();

		for(int doseTime : doseTimes)
		{
			forgottenIntakes.addAll(getAllOpenIntakes(date, doseTime));

			if(doseTime == lastDoseTime)
				break;
		}

		return forgottenIntakes;
	}

	private void checkIntakes(Date date, int lastDoseTime)
	{
		final Set<Intake> forgottenIntakes = getAllForgottenIntakes(date, lastDoseTime);

		Log.d(TAG, (sSvcInfo.forgottenIntakes = forgottenIntakes.size()) + " forgotten intakes");

		if(!forgottenIntakes.isEmpty())
		{
			final String contentText = Integer.toString(forgottenIntakes.size());
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
		final List<Drug> drugs = Database.getCachedDrugs();
		final List<Drug> drugsWithLowSupply = new ArrayList<Drug>();

		for(Drug drug : drugs)
		{
			// refill size of zero means ignore supply values
			if(drug.getRefillSize() == 0)
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
				final double currentSupply = drug.getCurrentSupply().doubleValue();
				
				Log.d(TAG, "Supplies left for " + drug + ": " + currentSupply / dailyDose);

				if(Double.compare(currentSupply / dailyDose, (double) minDays) == -1)
					drugsWithLowSupply.add(drug);
			}
		}

		return drugsWithLowSupply;
	}

	private void enqueueNotification(int id, String message) {
		mNotificationMessages[notificationIdToIndex(id)] = message;
	}

	private void postNotification(int id, int defaults, String message)
	{
		enqueueNotification(id, message);
		postAllNotifications(defaults);
	}

	private void postAllNotifications(int defaults)
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

		if(mNotificationMessages[2] != null && notificationCount != 1)
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
				// if we appended this line-break in the append() call above, the layout would look
				// messed up in case there was no "low supply" message
				msgBuilder.append("\n");
			}

			msgBuilder.append(bullet + doseMsgLowSupply);
		}

		final RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
		views.setTextViewText(R.id.stat_title, getString(R.string._title_notifications));
		views.setTextViewText(R.id.stat_text, msgBuilder.toString());
		views.setTextViewText(R.id.stat_time, new SimpleDateFormat("HH:mm").format(DateTime.now()));

		final Notification notification = new Notification();
		notification.icon = R.drawable.ic_stat_pill;
		notification.tickerText = getString(R.string._msg_new_notification);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notification.defaults = Preferences.instance().filterNotificationDefaults(defaults);
		notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, mIntent, 0);
		notification.contentView = views;
		if(notificationCount > 1)
			notification.number = notificationCount;

		final int notificationHash = getNotificationHashCode(notification, msgBuilder.toString());

		if(mLastNotificationHash == notificationHash)
			notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		else
			mLastNotificationHash = sSvcInfo.lastNotificationHash = notificationHash;		
		
		mNotificationManager.notify(R.id.notification, notification);
	}

	private void cancelNotification(int id) {
		postNotification(id, Notification.DEFAULT_LIGHTS, null);
	}
	
	private void cancelAllNotifications(boolean resetHash) {
		clearAllNotifications(true);
		mNotificationManager.cancel(R.id.notification);
		if(resetHash)
			mLastNotificationHash = 0;
	}
	
	private void clearAllNotifications() {
		clearAllNotifications(true);
	}

	private void clearAllNotifications(boolean zapMessages)
	{
		//mNotificationManager.cancel(R.id.notification);
		//mLastNotificationHash = 0;
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
			sSvcInfo.sleepingUntil = System.currentTimeMillis() + time;
			sSvcInfo.timeOfSleepBegin = DateTime.now();
			Thread.sleep(time);
		}
		else
			Log.d(TAG, "sleep: ignoring time of " + time);
		
		sSvcInfo.sleepingUntil = -1;
		sSvcInfo.timeOfSleepBegin = null;
	}

	private void writeCrashLog(Exception cause)
	{
		long timestamp = System.currentTimeMillis() / 1000;
		File crashLog = new File(getExternalFilesDir(null), "crash-" + timestamp + ".log");

		OutputStream os = null;
		
		try
		{
			os = new FileOutputStream(crashLog);
			String msg =
					"Time: " + DateTime.now() + "\n\n" +
					cause + "\n\n" +
					"Closing thread...";

			os.write(msg.getBytes());
			os.close();
		}
		catch(IOException e)
		{
			Log.e(TAG, "Error writing crash log", e);
		}
		finally
		{
			try
			{
				if(os != null)
					os.close();
			}
			catch(IOException e)
			{
				Log.e(TAG, "Error writing crash log", e);
			}
		}
	}
}
