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
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;

import com.j256.ormlite.android.apptools.OrmLiteBaseService;
import com.j256.ormlite.dao.Dao;

/**
 * Primary notification service for handling 
 * 
 * @author caspase
 *
 */
public class DrugNotificationService extends OrmLiteBaseService<Database.Helper> implements DatabaseWatcher
{
	private static final String TAG = DrugNotificationService.class.getName();
	
	Thread mThread;
	long mSnoozeTime;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		Log.d(TAG, "onCreate");
		restartThread();
		
		mSnoozeTime = Settings.INSTANCE.getSnoozeTime();
		
		Database.addWatcher(this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);
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
								
				while(true)
				{
					if(doseTime == -1)
						doseTime = Settings.INSTANCE.getActiveOrNextDoseTime();
					
					// TODO check supply levels
					final Date today = Util.DateTime.today();
					final long offset = Util.DateTime.now().getTime() - today.getTime();				
					final long millisUntilNextDoseTime = Settings.INSTANCE.getDoseTimeBeginOffset(doseTime) - offset;
					
					Log.d(TAG, "Next dose time (" + doseTime + ") in " + millisUntilNextDoseTime + "ms");
					
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
							if(!drug.getDose(doseTime).equals(0))
							{
								final List<Intake> intakes = Database.getIntakes(intakeDao, drug, today, doseTime);
								if(intakes.isEmpty())
									++count;
								else
									Log.d(TAG, "Not counting " + drug + ": " + intakes.size() + " intakes");
							}
						}
						
						if(count != 0)
						{										
							PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
							
							final CharSequence contentTitle = "RxDroid: Reminder";
							final CharSequence contentText = "You have " + count + " prescriptions to take";
							
							Notification notification = new Notification(R.drawable.med_pill, "RxDroid", System.currentTimeMillis());
							notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
							notification.defaults |= Notification.DEFAULT_ALL;
							
							Log.d(TAG, "Posting notification");
							notificationManager.notify(R.id.notification_intake, notification);							
							
							Thread.sleep(mSnoozeTime);
						}
						else
						{
							Log.d(TAG, "No intakes remaining.");
							doseTime = Settings.INSTANCE.getNextDoseTime();			
						}
							
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
