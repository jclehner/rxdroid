package at.caspase.rxdroid;

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
import com.j256.ormlite.stmt.query.Not;

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
			mThread.interrupt();
		
		mThread = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
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
				
				int count = 0;
				
				while(true)
				{
					final int doseTime = Settings.INSTANCE.getNextDoseTime();
					
					// TODO check supply levels
					
					final long day = Util.getMidnightMillisFromNow();
					final long offset = Util.getDayOffsetInMillis();
					
					final long millisUntilNextDoseTime = Settings.INSTANCE.getDoseTimeBegin(doseTime) - offset;
					
					Log.d(TAG, "Next dose time (" + doseTime + ") in " + millisUntilNextDoseTime + "ms");
					
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setClass(getApplicationContext(), DrugListActivity.class);
					intent.putExtra(DrugListActivity.EXTRA_DAY, Util.getMidnightMillisFromNow());
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					
					try
					{
						Thread.sleep(millisUntilNextDoseTime);
																		
						while(Settings.INSTANCE.getActiveDoseTime() == doseTime)
						{
							count = 0;
							// FIXME this does not need to be determined every time							
							for(Drug drug : drugs)
							{
								if(drug.isActive() && !drug.getDose(doseTime).equals(0))
								{
									final List<Intake> intakes = Database.getIntakes(intakeDao, drug, day, doseTime);
									if(intakes.isEmpty())
										++count;
								}
							}
							
							if(count != 0)
							{													
								NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);					
								PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
								
								final CharSequence contentTitle = "RxDroid: Reminder";
								final CharSequence contentText = "You have " + count + " prescriptions to take";
								
								Notification notification = new Notification(R.drawable.med_pill, "RxDroid", System.currentTimeMillis());
								notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
								notification.defaults |= Notification.DEFAULT_ALL;
								
								manager.cancel(R.id.notification_intake);
								manager.notify(R.id.notification_intake, notification);
								
								if(Settings.INSTANCE.getDoseTimeBegin(doseTime) - offset > mSnoozeTime)
									Thread.sleep(mSnoozeTime);
							}
							else
								break;
						}						
					}
					catch (InterruptedException e)
					{
						Log.d(TAG, "Interrupted. Exiting.");
						stopSelf();
						break;
					}
				}
			}
		});
		
		mThread.start();
	}


}
