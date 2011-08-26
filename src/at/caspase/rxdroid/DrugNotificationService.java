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

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;
import at.caspase.rxdroid.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.L10N;

import com.j256.ormlite.android.apptools.OrmLiteBaseService;
import com.j256.ormlite.dao.Dao;

/**
 * Primary notification service.
 * 
 * @author Joseph Lehner
 * 
 */
public class DrugNotificationService extends OrmLiteBaseService<Database.Helper> implements 
		OnDatabaseChangedListener, OnSharedPreferenceChangeListener
{
	public static final String EXTRA_FORCE_RESTART = "force_restart";

	private static final String TAG = DrugNotificationService.class.getName();
	private static final String TICKER_TEXT = "RxDroid";

	private Dao<Drug, Integer> mDrugDao;
	private Dao<Intake, Integer> mIntakeDao;
	private NotificationManager mNotificationManager;
	private Intent mIntent;

	private Set<Intake> mForgottenIntakes = Collections.emptySet();

	private SharedPreferences mSharedPreferences;

	Thread mThread;

	@Override
	public void onCreate()
	{
		super.onCreate();
		mDrugDao = getHelper().getDrugDao();
		mIntakeDao = getHelper().getIntakeDao();

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mIntent = new Intent(Intent.ACTION_VIEW);
		mIntent.setClass(getApplicationContext(), DrugListActivity.class);
		// mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Settings.INSTANCE.setApplicationContext(getApplicationContext());

		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
		Database.registerOnChangedListener(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);
		restartThread(intent.getBooleanExtra(EXTRA_FORCE_RESTART, false));
		return START_STICKY;
	}

	@Override
	public synchronized void onDestroy()
	{
		super.onDestroy();
		mThread.interrupt();
		mThread = null;
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		Database.unregisterOnChangedListener(this);
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreateEntry(Drug drug)
	{
		// checkForgottenIntakes(drug, null, false);
		restartThread(true);
	}

	@Override
	public void onDeleteEntry(Drug drug)
	{
		// checkForgottenIntakes(drug, null, true);
		restartThread(true);
	}

	@Override
	public void onUpdateEntry(Drug drug)
	{
		// checkForgottenIntakes(drug, null, false);
		restartThread(true);
	}

	@Override
	public void onCreateEntry(Intake intake)
	{
		// checkForgottenIntakes(null, intake, false);
		restartThread(true);
	}

	@Override
	public void onDeleteEntry(Intake intake)
	{
		// checkForgottenIntakes(null, intake, true);
		restartThread(true);
	}

	@Override
	public void onDatabaseDropped()
	{
		restartThread(true);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
			String key)
	{
		restartThread(true);
	}

	/**
	 * (Re)starts the worker thread.
	 * 
	 * Calling this function will cause the service to consult the DB in order
	 * to determine when the next notification should be posted. Currently, the
	 * worker thread is restarted when <em>any</em> database changes occur (see
	 * DatabaseWatcher) or when the user opens the app.
	 */

	private synchronized void restartThread(final boolean forceRestart)
	{
		Log.d(TAG, "restartThread(" + forceRestart + ")");

		if (mThread != null)
		{
			if (!forceRestart)
			{
				Log.d(TAG, "Ignoring service restart request");
				return;
			}

			mThread.interrupt();
		}

		mThread = new Thread(new Runnable() {

			@Override
			public void run()
			{
				final PendingIntent contentIntent = PendingIntent.getActivity(
						getApplicationContext(), 0, mIntent, 0);

				/**
				 * TODO:
				 * 
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
				 * 
				 * 
				 * 
				 * 
				 * 
				 * 
				 * 
				 */

				mNotificationManager.cancel(TAG, R.id.notification_intake);
				// mNotificationManager.cancel(R.id.notification_intake_forgotten);
				mNotificationManager.cancel(TAG, R.id.notification_low_supplies);

				try
				{
					boolean delayFirstNotification = true;
					
					checkSupplies();
					
					while (true)
					{
						final Date date = DateTime.today();
						mIntent.putExtra(DrugListActivity.EXTRA_DAY, date);

						final int activeDoseTime = Settings.INSTANCE
								.getActiveDoseTime();
						final int nextDoseTime = Settings.INSTANCE
								.getNextDoseTime();
						final int lastDoseTime = (activeDoseTime == -1) ? (nextDoseTime - 1)
								: (activeDoseTime - 1);

						Log.d(TAG, "times: active=" + activeDoseTime
								+ ", next=" + nextDoseTime + ", last="
								+ lastDoseTime);

						if (lastDoseTime >= 0)
						{
							mForgottenIntakes = getAllForgottenIntakes(date,
									lastDoseTime);
							displayOrClearForgottenIntakesNotification();
						}

						if (activeDoseTime == -1)
						{
							long sleepTime = Settings.INSTANCE
									.getMillisFromNowUntilDoseTimeBegin(nextDoseTime);

							Log.d(TAG, "Time until next dose time ("
									+ nextDoseTime + "): " + sleepTime + "ms");

							Thread.sleep(sleepTime);
							delayFirstNotification = false;

							if (Settings.INSTANCE.getActiveDoseTime() != nextDoseTime)
								throw new RuntimeException(
										"Unexpected dose time, expected "
												+ nextDoseTime);

							continue;
						}
						else if (activeDoseTime == Drug.TIME_MORNING)
						{
							mForgottenIntakes.clear();

							mNotificationManager.cancel(TAG, R.id.notification_intake_forgotten);
							checkSupplies();
							// mNotificationManager.cancel(R.id.notification_low_supplies);
						}

						long millisUntilDoseTimeEnd = Settings.INSTANCE
								.getMillisFromNowUntilDoseTimeEnd(activeDoseTime);

						final Set<Intake> pendingIntakes = getAllOpenIntakes(
								date, activeDoseTime);

						if (!pendingIntakes.isEmpty())
						{
							final int count = pendingIntakes.size();

							final CharSequence contentText = count
									+ " doses pending. Click to snooze.";
							final CharSequence contentTitle = "Dose reminder";

							final Notification notification = new Notification(R.drawable.ic_stat_pill, contentTitle, DateTime.currentTimeMillis());
							notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
							notification.defaults |= Notification.DEFAULT_ALL;
							notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_AUTO_CANCEL;

							final long snoozeTime = Settings.INSTANCE.getSnoozeTime();

							if (delayFirstNotification)
							{
								delayFirstNotification = false;
								Log.d(TAG, "Delaying first notification");
								// FIXME export this
								Thread.sleep(10000);
							}

							Log.d(TAG, "Will post " + millisUntilDoseTimeEnd
									/ snoozeTime + " notifications");

							while (millisUntilDoseTimeEnd > snoozeTime)
							{
								notification.when = DateTime.currentTimeMillis();
								mNotificationManager.notify(TAG, R.id.notification_intake, notification);
								Thread.sleep(snoozeTime);
								millisUntilDoseTimeEnd -= snoozeTime;
							}
						}

						if (millisUntilDoseTimeEnd > 0)
						{
							Log.d(TAG, "Sleeping " + millisUntilDoseTimeEnd + "ms until end of dose time " + activeDoseTime);
							Thread.sleep(millisUntilDoseTimeEnd);
						}

						Log.d(TAG, "Finished iteration");
					}
				}
				catch (InterruptedException e)
				{
					Log.d(TAG, "Thread interrupted, exiting...");
				}
			}
		});

		mThread.start();
	}

	private Set<Intake> getAllOpenIntakes(Date date, int doseTime)
	{
		final Set<Intake> openIntakes = new HashSet<Database.Intake>();
		final List<Drug> drugs;

		try
		{
			drugs = mDrugDao.queryForAll();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}

		for (Drug drug : drugs)
		{
			if (drug.isActive())
			{
				final List<Intake> intakes = Database.findIntakes(mIntakeDao, drug, date, doseTime);

				if (drug.getDose(doseTime).compareTo(0) != 0 && intakes.size() == 0)
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

		if (date.after(today))
			return Collections.emptySet();

		if (date.before(today))
			lastDoseTime = -1;

		final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };
		final Set<Intake> forgottenIntakes = new HashSet<Database.Intake>();

		for (int doseTime : doseTimes)
		{
			forgottenIntakes.addAll(getAllOpenIntakes(date, doseTime));

			if (doseTime == lastDoseTime)
				break;
		}

		return forgottenIntakes;
	}

	private void displayOrClearForgottenIntakesNotification()
	{
		Log.d(TAG, mForgottenIntakes.size() + " forgotten intakes");

		if (!mForgottenIntakes.isEmpty())
		{
			final CharSequence contentText = mForgottenIntakes.size() + " forgotten doses";
			final CharSequence title = "Forgotten doses";

			final Notification notification = new Notification(R.drawable.ic_stat_pill, title, DateTime.currentTimeMillis());
			final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, mIntent, 0);

			notification.setLatestEventInfo(getApplicationContext(), title, contentText, contentIntent);
			notification.defaults |= Notification.DEFAULT_LIGHTS;
			notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;

			mNotificationManager.notify(TAG, R.id.notification_intake_forgotten, notification);
		}
		else
			mNotificationManager.cancel(TAG, R.id.notification_intake_forgotten);
	}

	private void checkSupplies()
	{
		// FIXME
		final int MIN_DAYS = 7;
		
		final List<Drug> drugsWithLowSupply = getAllDrugsWithLowSupply(MIN_DAYS);
		
		if(!drugsWithLowSupply.isEmpty())
		{
			//final CharSequence contentText = mForgottenIntakes.size() + " forgotten doses";
			final CharSequence title = "Low supplies";
			
			final Notification notification = new Notification(R.drawable.ic_stat_pill, title, 0);
			final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, mIntent, 0);
						
			final String firstDrugName = drugsWithLowSupply.get(0).getName();
			final CharSequence contentText;
			
			if(drugsWithLowSupply.size() == 1)
				contentText = L10N.getText("Low supplies for %1.", firstDrugName);
			else
				contentText = L10N.getText("Low suppies for %1 and %2 others.", firstDrugName, drugsWithLowSupply.size() - 1);
			
			notification.setLatestEventInfo(getApplicationContext(), title, contentText, contentIntent);
			notification.defaults = Notification.DEFAULT_LIGHTS;
			notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_ONGOING_EVENT;
			
			mNotificationManager.notify(TAG, R.id.notification_low_supplies, notification);			
		}
		else
			mNotificationManager.cancel(TAG, R.id.notification_low_supplies);
	}

	private List<Drug> getAllDrugsWithLowSupply(int minDays)
	{
		final List<Drug> drugs;

		try
		{
			drugs = mDrugDao.queryForAll();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}

		ArrayList<Drug> drugsWithLowSupply = new ArrayList<Drug>();

		for (Drug drug : drugs)
		{
			// refill size of zero means ignore supply values
			if (drug.getRefillSize() == 0)
				continue;

			final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON,
					Drug.TIME_EVENING, Drug.TIME_NIGHT };
			double dailyDose = 0;

			for (int doseTime : doseTimes)
			{
				final Fraction dose = drug.getDose(doseTime);
				if (dose.compareTo(0) != 0)
					dailyDose += dose.doubleValue();
			}

			if (dailyDose != 0)
			{
				final double currentSupply = drug.getCurrentSupply().doubleValue();

				if (Double.compare(currentSupply / dailyDose, (double) minDays) == -1)
					drugsWithLowSupply.add(drug);
			}
		}

		return drugsWithLowSupply;
	}
}
