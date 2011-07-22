package at.caspase.rxdroid;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
 * @author caspase
 *
 */
public class DrugNotificationService extends OrmLiteBaseService<Database.Helper> implements DatabaseWatcher
{
	private static final String TAG = DrugNotificationService.class.getName();
	
	Thread mThread;
	// FIXME
	final long mSnoozeTime = Settings.INSTANCE.getSnoozeTime();
	
	@Override
	public void onCreate()
	{
		super.onCreate();
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
		Log.d(TAG, "onDrugCreate");
		restartThread();		
	}

	@Override
	public void onDrugDelete(Drug drug)
	{
		restartThread();
	}

	@Override
	public void onDrugUpdate(Drug drug)
	{
		restartThread();
	}

	@Override
	public void onIntakeCreate(Intake intake)
	{
		restartThread();
	}

	@Override
	public void onIntakeDelete(Intake intake)
	{
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
	private void restartThread()
	{
		final Dao<Drug, Integer> drugDao = getHelper().getDrugDao();
		final Dao<Intake, Integer> intakeDao = getHelper().getIntakeDao();
		final List<Drug> drugs;
		
		try
		{
			drugs = drugDao.queryForAll();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
		
		if(mThread != null && mThread.isAlive())
		{
			mThread.interrupt();
			Log.d(TAG, "Interrupting thread");
		}
		
		mThread = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.cancel(R.id.notification_intake);
				
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
					long offset = Util.DateTime.nowOffsetFromMidnight();		
					
					long millisUntilNextDoseTime = Settings.INSTANCE.getDoseTimeBeginOffset(doseTime) - offset;
					if(!firstRun)
					{
						millisUntilNextDoseTime += Util.Constants.MILLIS_PER_DAY;
						Log.d(TAG, "Adjusting sleep time to next day");
					}
					
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setClass(getApplicationContext(), DrugListActivity.class);
					intent.putExtra(DrugListActivity.EXTRA_DAY, today);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					
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
						
						int count = 0;
						// FIXME this does not need to be determined every time							
						for(Drug drug : drugs)
						{
							if(drug.isActive() && !drug.getDose(doseTime).equals(0))
							{
								final List<Intake> intakes = Database.getIntakes(intakeDao, drug, today, doseTime);
								if(intakes.isEmpty())
									++count;
								else
									Log.d(TAG, "Not counting " + drug + ": " + intakes.size() + " intakes");
							}
							else
								Log.d(TAG, "Not considering " + drug);
						}
						
						if(count != 0)
						{										
							PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
							
							final CharSequence contentTitle = "RxDroid: Reminder";
							CharSequence contentText = "You have " + count + " prescriptions to take";
							
							Notification notification = new Notification(R.drawable.ic_stat_pill, "RxDroid", Util.DateTime.currentTimeMillis());
							notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
							notification.defaults |= Notification.DEFAULT_ALL;
							notification.number = count;
							
							Log.d(TAG, "Posting notification");
							notificationManager.notify(R.id.notification_intake, notification);							
							
							offset = Util.DateTime.nowOffsetFromMidnight();
							final long millisUntilDoseTimeEnd = Settings.INSTANCE.getDoseTimeEndOffset(doseTime) - offset;
							
							if(millisUntilDoseTimeEnd > 0)
							{
								Log.d(TAG, "Time left until dose time end: " + millisUntilDoseTimeEnd + "ms");
								Thread.sleep(millisUntilDoseTimeEnd);
							}
							
							notificationManager.cancel(R.id.notification_intake);
							
							contentText = count + " prescriptions were not taken on time";
							notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
							
							notificationManager.notify(R.id.notification_intake_forgotten, notification);
						}						
						
						// TODO cancel old notification, display "forgotten" notification
						
						doseTime = Settings.INSTANCE.getNextDoseTime();						
					}
					catch (InterruptedException e)
					{
						break;
					}
				}
			}
		});
		
		Log.d(TAG, "Starting thread");
		mThread.start();
	}
}
