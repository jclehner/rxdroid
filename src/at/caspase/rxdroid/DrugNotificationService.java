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

import com.j256.ormlite.android.apptools.OrmLiteBaseService;
import com.j256.ormlite.dao.Dao;

/**
 * Primary notification service.
 * 
 * @author Joseph Lehner
 *
 */
public class DrugNotificationService extends OrmLiteBaseService<Database.Helper> implements OnDatabaseChangedListener, OnSharedPreferenceChangeListener
{
	private static final String TAG = DrugNotificationService.class.getName();
	private static final String TICKER_TEXT = "RxDroid";
		
	private Dao<Drug, Integer> mDrugDao;
	private Dao<Intake, Integer> mIntakeDao;
	private NotificationManager mNotificationManager;
	private Date mDate;
	private Intent mIntent;
	
	private Set<Intake> mForgottenIntakes = Collections.emptySet();
	
	private SharedPreferences mSharedPreferences;
	
	Thread mThread;
	// FIXME
	final long mSnoozeTime = Settings.INSTANCE.getSnoozeTime();
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		mDrugDao = getHelper().getDrugDao();
		mIntakeDao = getHelper().getIntakeDao();
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				
		mDate = Util.DateTime.today();
		
		mIntent = new Intent(Intent.ACTION_VIEW);
		mIntent.setClass(getApplicationContext(), DrugListActivity.class);
		mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		Settings.INSTANCE.setApplicationContext(getApplicationContext());
				
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
		Database.addWatcher(this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);
		restartThread();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() 
	{
		super.onDestroy();
		mThread.interrupt();
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		Database.removeWatcher(this);
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
		//checkForgottenIntakes(drug, null, false);
		restartThread();		
	}

	@Override
	public void onDeleteEntry(Drug drug)
	{
		//checkForgottenIntakes(drug, null, true);
		restartThread();
	}

	@Override
	public void onUpdateEntry(Drug drug)
	{
		//checkForgottenIntakes(drug, null, false);
		restartThread();
	}

	@Override
	public void onCreateEntry(Intake intake)
	{
		//checkForgottenIntakes(null, intake, false);
		restartThread();
	}

	@Override
	public void onDeleteEntry(Intake intake)
	{
		//checkForgottenIntakes(null, intake, true);
		restartThread();
	}

	@Override
	public void onDatabaseDropped()
	{
		restartThread();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key)
	{
		restartThread();
	}
	
	/**
	 * (Re)starts the worker thread.
	 * 
	 * Calling this function will cause the service to consult the DB in order to 
	 * determine when the next notification should be posted. Currently, the 
	 * worker thread is restarted when <em>any</em> database changes occur (see
	 * DatabaseWatcher) or when the user opens the app. 
	 */	
	private synchronized void restartThread()
	{	
		mDate = Util.DateTime.today();
		
		if(mThread != null)
			mThread.interrupt();
				
		mThread = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				Log.d(TAG, "Thread is up and running");
								
				final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, mIntent, 0);
												
				/* TODO
				 * 
				 * 1. query settings to determine begin of the next doseTime period & sleep 
				 * 2. determine drugs with a dose other than "0" for the current doseTime
				 * 3. set Intent options for DrugListActivity: mDay=day of notification
				 * 4. post notification
				 * 5. if doseTime == TIME_MORNING, check if supply levels are low and
				 *    post notification
				 * 6. determine remaining time in the current doseTime period & sleep
				 * 7. clear intake reminder notification & replace with "forgotten" notification
				 * 8. goto 1
				 * 
				 * also install a BroadcastReceiver to monitor changes to the system time
				 */
				
				int doseTime = Settings.INSTANCE.getActiveOrNextDoseTime();
				boolean firstRun = true;
				
				mForgottenIntakes = getAllForgottenIntakes(mDate);
				displayOrClearForgottenIntakesNotification();
				
				while(true)
				{
					// TODO check supply levels
					long offset = Util.DateTime.nowOffsetFromMidnight();
					long millisUntilNextDoseTime = Settings.INSTANCE.getDoseTimeBeginOffset(doseTime) - offset;
					boolean dateChangeImminent = false;
					
					
					Log.d(TAG, "offset=" + offset);
					Log.d(TAG, "millisUntilNextDoseTime=" + millisUntilNextDoseTime);
					
					if(!firstRun && millisUntilNextDoseTime < 0)
					{
						assert doseTime == Drug.TIME_MORNING;
						
						dateChangeImminent = true;
						millisUntilNextDoseTime += Util.Constants.MILLIS_PER_DAY;
						
						Log.d(TAG, "Date change is imminent. Adjusting sleep time to next day.");
					}					
													
					try
					{						
						if(millisUntilNextDoseTime > 0)
						{					
							Log.d(TAG, "Will sleep " + millisUntilNextDoseTime + "ms");
							Thread.sleep(millisUntilNextDoseTime);
						}
						else if(firstRun)
						{
							// if the user marks a drug as taken, this thread will be restarted. in order to prevent a new
							// notification from flashing up instantly, we'll snooze a little
							Log.d(TAG, "Will snooze");
							Thread.sleep(mSnoozeTime);
							firstRun = false;
						}
												
						if(dateChangeImminent)
						{
							Log.d(TAG, "Setting date to next day.");
							mDate = Util.DateTime.today();
							mIntent.putExtra(DrugListActivity.EXTRA_DAY, mDate);
							mForgottenIntakes.clear();
						}
						
												
						final Set<Intake> pendingIntakes = getAllPendingIntakes(mDate, doseTime);
											
						if(!pendingIntakes.isEmpty())
						{
							final int count = pendingIntakes.size();							
														
							final CharSequence contentText = count + " doses pending. Click to snooze.";
							
							final Notification notification = new Notification(R.drawable.ic_stat_pill, "RxDroid", Util.DateTime.currentTimeMillis());
							notification.setLatestEventInfo(getApplicationContext(), "Dose reminder", contentText, contentIntent);
							notification.defaults |= Notification.DEFAULT_ALL;
							notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
							//notification.number = count;											
							
							offset = Util.DateTime.nowOffsetFromMidnight();
							
							long millisUntilDoseTimeEnd = Settings.INSTANCE.getDoseTimeEndOffset(doseTime) - offset;
							
							if(millisUntilDoseTimeEnd > 0)
							{
								Log.d(TAG, "Time left until dose time end: " + millisUntilDoseTimeEnd + "ms");
								
								while(millisUntilDoseTimeEnd >= mSnoozeTime)
								{
									mNotificationManager.notify(R.id.notification_intake, notification);									
									Thread.sleep(mSnoozeTime);
									millisUntilDoseTimeEnd -= mSnoozeTime;
								}								
							}
							
							mNotificationManager.cancel(R.id.notification_intake);
							mForgottenIntakes.addAll(pendingIntakes);

							displayOrClearForgottenIntakesNotification();
						}
						
						doseTime = Settings.INSTANCE.getNextDoseTime();						
					}
					catch (InterruptedException e)
					{
						Log.d(TAG, "Thread was interrupted. Exiting...");
						break;
					}
				}
			}
		});
		
		synchronized(Locker.INSTANCE) {
			mThread.start();
		}
	}
		
	private Set<Intake> getAllForgottenIntakes(Date date) {
		return getAllForgottenOrPendingIntakes(date, -1);
	}
	
	private Set<Intake> getAllPendingIntakes(Date date, int activeDoseTime) {
		return getAllForgottenOrPendingIntakes(date, activeDoseTime);
	}
	
	private Set<Intake> getAllForgottenOrPendingIntakes(Date date, int activeDoseTime)
	{
		final boolean onlyPendingIntakes = activeDoseTime != -1;
		
		final List<Drug> drugs;
		try
		{
			drugs = mDrugDao.queryForAll();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
		
		int activeOrNextDoseTime = (activeDoseTime == -1) ? Settings.INSTANCE.getActiveOrNextDoseTime() : activeDoseTime;
		
		if(onlyPendingIntakes && activeDoseTime == -1)
			return Collections.emptySet();
		
		if(activeDoseTime == -1 && activeOrNextDoseTime == 0)
		{
			// FIXME this is hackish.
			activeOrNextDoseTime = Drug.TIME_NIGHT + 1;
			Log.d(TAG, "HACK");
		}
		
		Log.d(TAG, "activeOrNextDoseTime: " + activeOrNextDoseTime);
		
		Set<Intake> intakes = new HashSet<Intake>();
		
		for(Drug drug : drugs)
		{
			if(!drug.isActive())
				continue;
			
			for(int doseTime = 0;; ++doseTime)
			{
				if(!onlyPendingIntakes && doseTime == activeOrNextDoseTime)
					break;
				
				if(!onlyPendingIntakes || doseTime == activeDoseTime)
				{					
					if(!drug.getDose(doseTime).equals(0))
					{
						final List<Intake> intakesInDb = Database.getIntakes(mIntakeDao, drug, date, doseTime);
						if(intakesInDb.isEmpty())
						{				
							
							final Intake intake = new Intake(drug, mDate, doseTime);
							intakes.add(intake);
							Log.d(TAG, "getAllForgottenOrPendingIntakes: adding " + intake);
						}
					}
					
					if(onlyPendingIntakes)
						break;
				}			
			}
		}
		
		return intakes;
	}
	
	private void displayOrClearForgottenIntakesNotification()
	{
		Log.d(TAG, mForgottenIntakes.size() + " forgotten intakes");
		
		if(!mForgottenIntakes.isEmpty())
		{
			final CharSequence contentText = mForgottenIntakes.size() + " forgotten doses";
			final Notification notification = new Notification(R.drawable.ic_stat_pill, TICKER_TEXT, Util.DateTime.currentTimeMillis());
			final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, mIntent, 0);
			
			notification.setLatestEventInfo(getApplicationContext(), "Forgotten doses", contentText, contentIntent);
			notification.defaults |= Notification.DEFAULT_LIGHTS;
			notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
			
			mNotificationManager.notify(R.id.notification_intake_forgotten, notification);
		}
		else
			mNotificationManager.cancel(R.id.notification_intake_forgotten);
	}	

	private enum Locker { INSTANCE; }
}
