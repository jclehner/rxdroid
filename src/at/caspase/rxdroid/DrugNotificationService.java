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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path.FillType;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;

import com.j256.ormlite.android.apptools.OrmLiteBaseService;
import com.j256.ormlite.dao.Dao;

/**
 * Primary notification service.
 * 
 * @author Joseph Lehner
 *
 */
public class DrugNotificationService extends OrmLiteBaseService<Database.Helper> implements DatabaseWatcher
{
	private static final String TAG = DrugNotificationService.class.getName();
	
	private static final String TICKER_TEXT = "RxDroid";
	private static final String CONTENT_TITLE = "RxDroid: Notification";
	
	
	private Dao<Drug, Integer> mDrugDao;
	private Dao<Intake, Integer> mIntakeDao;
	private NotificationManager mNotificationManager;
	private Date mDate;
	
	private Set<Intake> mForgottenIntakes = Collections.emptySet();
	
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
		Log.d(TAG, "onDestroy");
		// FIXME
		mThread.interrupt();		
		Database.removeWatcher(this);
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDrugCreate(Drug drug)
	{
		//checkForgottenIntakes(drug, null, false);
		restartThread();		
	}

	@Override
	public void onDrugDelete(Drug drug)
	{
		//checkForgottenIntakes(drug, null, true);
		restartThread();
	}

	@Override
	public void onDrugUpdate(Drug drug)
	{
		//checkForgottenIntakes(drug, null, false);
		restartThread();
	}

	@Override
	public void onIntakeCreate(Intake intake)
	{
		//checkForgottenIntakes(null, intake, false);
		restartThread();
	}

	@Override
	public void onIntakeDelete(Intake intake)
	{
		//checkForgottenIntakes(null, intake, true);
		restartThread();
	}

	@Override
	public void onDatabaseDropped()
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
		if(mThread != null)
			mThread.interrupt();
				
		mThread = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				Log.d(TAG, "Started thread...");
				
				final Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setClass(getApplicationContext(), DrugListActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				
				final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
				final Notification notification = new Notification(R.drawable.ic_stat_pill, "RxDroid", 0);
				
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
				
				int doseTime = -1;
				boolean firstRun = true;
				
				while(true)
				{
					if(doseTime == -1)
						doseTime = Settings.INSTANCE.getActiveOrNextDoseTime();
					else
						firstRun = false;
					
					// TODO check supply levels
					final Date today = Util.DateTime.today();
					
					if(!mDate.equals(today))
					{
						mForgottenIntakes = getAllForgottenIntakes(mDate);
						mDate = today;
						intent.putExtra(DrugListActivity.EXTRA_DAY, today);
						
						// TODO check current supplies
					}				
					
					long offset = Util.DateTime.nowOffsetFromMidnight();		
					
					long millisUntilNextDoseTime = Settings.INSTANCE.getDoseTimeBeginOffset(doseTime) - offset;
					if(!firstRun)
					{
						millisUntilNextDoseTime += Util.Constants.MILLIS_PER_DAY;
						Log.d(TAG, "Adjusting sleep time to next day");
					}
															
					maybeDisplayForgottenIntakesNotification(intent);
					
					try
					{						
						if(millisUntilNextDoseTime > 0)
						{					
							Log.d(TAG, "Will sleep " + millisUntilNextDoseTime + "ms");
							Thread.sleep(millisUntilNextDoseTime);
						}
						else
						{
							// if the user marks a drug as taken, this thread will be restarted. in order to prevent a new
							// notification from flashing up instantly, we'll snooze a little
							Log.d(TAG, "Will snooze");
							Thread.sleep(mSnoozeTime);
						}
						
						final Set<Intake> pendingIntakes = getAllPendingIntakes(mDate, doseTime);
											
						if(!pendingIntakes.isEmpty())
						{
							final int count = pendingIntakes.size();							
														
							final CharSequence contentText = "You have " + count + " prescriptions to take";
							notification.setLatestEventInfo(getApplicationContext(), CONTENT_TITLE, contentText, contentIntent);
							notification.defaults |= Notification.DEFAULT_ALL;
							notification.when = Util.DateTime.currentTimeMillis();
							//notification.number = count;
							
							Log.d(TAG, "Posting notification");
							mNotificationManager.notify(R.id.notification_intake, notification);							
							
							offset = Util.DateTime.nowOffsetFromMidnight();
							final long millisUntilDoseTimeEnd = Settings.INSTANCE.getDoseTimeEndOffset(doseTime) - offset;
							
							if(millisUntilDoseTimeEnd > 0)
							{
								Log.d(TAG, "Time left until dose time end: " + millisUntilDoseTimeEnd + "ms");
								Thread.sleep(millisUntilDoseTimeEnd);
							}
							
							mNotificationManager.cancel(R.id.notification_intake);
							mForgottenIntakes.addAll(pendingIntakes);		
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
		
		Log.d(TAG, "Starting thread");
		mThread.start();
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
		
		final int activeOrNextDoseTime = (activeDoseTime == -1) ? Settings.INSTANCE.getActiveOrNextDoseTime() : activeDoseTime;
		
		if(onlyPendingIntakes && activeDoseTime == -1)
			return Collections.emptySet();
		
		Set<Intake> intakes = new HashSet<Database.Intake>();
		
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
						final Intake intake = new Intake(drug, mDate, doseTime);
						intakes.add(intake);
					}
					
					if(onlyPendingIntakes)
						break;
				}			
			}
		}
		
		return intakes;
	}
	
	
	private void checkForgottenIntakes(Drug drug, Intake intake, boolean wasRemoved)
	{
		assert drug == null ^ intake == null;
		
		synchronized(this)
		{
			for(Intake forgottenIntake : mForgottenIntakes)
			{
				if(drug != null)
				{				
					if(forgottenIntake.getDrug().getId() == drug.getId())
					{
						boolean removeIntake = wasRemoved;
						
						if(!removeIntake)
						{
							// check if the drug was updated in such a way that the
							// forgotten intake is not applicable any more, due to the
							// dose having been changed to zero						
							final int doseTime = forgottenIntake.getDoseTime();
							if(drug.getDose(doseTime).equals(0))
								removeIntake = true;						
						}
						
						if(removeIntake)
						{
							Log.d(TAG, "Removing intake: " + forgottenIntake);
							mForgottenIntakes.remove(forgottenIntake);
						}										
					}
				}
				else if(intake != null)
				{
					if(forgottenIntake.getDrug().equals(intake.getDrug()))
					{
						// FIXME
						assert !wasRemoved;
						
						// FIXME for now, assume that if an intake was added/updated, we can clear the forgotten intake
						mForgottenIntakes.remove(intake);						
					}					
				}
			}		
		}
	}
	
	private synchronized void maybeDisplayForgottenIntakesNotification(Intent intent)
	{
		if(!mForgottenIntakes.isEmpty())
		{	
			final CharSequence contentText = mForgottenIntakes.size() + " prescriptions were not taken on time";
			final Notification notification = new Notification(R.drawable.ic_stat_pill, TICKER_TEXT, Util.DateTime.currentTimeMillis());
			final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			
			notification.setLatestEventInfo(getApplicationContext(), CONTENT_TITLE, contentText, contentIntent);						
			notification.defaults |= Notification.DEFAULT_ALL;
			mNotificationManager.notify(R.id.notification_intake_forgotten, notification);
		}
		else
			mNotificationManager.cancel(R.id.notification_intake_forgotten);
	}
}
