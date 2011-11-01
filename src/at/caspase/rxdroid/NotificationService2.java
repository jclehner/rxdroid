package at.caspase.rxdroid;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Hasher;

public class NotificationService2 extends Service implements OnDatabaseChangedListener, OnSharedPreferenceChangeListener
{
	private static final String TAG = NotificationService2.class.getName();
	
	private AlarmManager mAlarmMgr;
	private Preferences mSettings;
	private SharedPreferences mSharedPrefs;
	private NotificationManager mNotificationMgr;
	
	private PendingIntent mOperation;
	
	private int mLastMessageHash = 0;
	
	public static final String EXTRA_DOSE_TIME = "dose_time";
	public static final String EXTRA_DATE = "date";
	
	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		rescheduleAlarms();
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		rescheduleAlarms();
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags)
	{
		rescheduleAlarms();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		// TODO filter
		rescheduleAlarms();		
	}
	
	@Override
	public void onCreate()
	{
		Log.d(TAG, "onCreate");
		super.onCreate();
		
		GlobalContext.set(getApplicationContext());
		
		mAlarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		mSettings = Preferences.instance();
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mNotificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		Intent intent = new Intent(this, NotificationService2.class);
		mOperation = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
		
		Database.registerOnChangedListener(this);
	}
	
	@Override
	public void onDestroy()
	{
		Log.d(TAG, "onDestroy");
		super.onDestroy();
		
		cancelAllAlarms();
		
		Database.unregisterOnChangedListener(this);
		mLastMessageHash = 0;
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.d(TAG, "onStartCommand: intent=" + intent);
		rescheduleAlarms();
		
		return START_REDELIVER_INTENT;
	}
	
	private void rescheduleAlarms()
	{
		Log.d(TAG, "rescheduleAlarms");
		
		cancelAllAlarms();
		
		final Calendar now = DateTime.now();
		final Calendar activeDate = mSettings.getActiveDate(now);
		int activeDoseTime = mSettings.getActiveDoseTime(now);
				
		if(activeDoseTime != -1)
			updateNotifications(activeDate, activeDoseTime);
		
		scheduleNextAlarm(now, activeDate);	
	}
	
	private void updateNotifications(Calendar date, int doseTime)
	{
		int pendingIntakes = countOpenIntakes(date, doseTime);
		int forgottenIntakes = countForgottenIntakes(date, doseTime);
		String lowSupplyMessage = getLowSupplyMessage(date, doseTime);
		
		if((pendingIntakes + forgottenIntakes) == 0 && lowSupplyMessage == null)
			mNotificationMgr.cancel(R.id.notification);
		else
		{
			String doseMessage = null;
			int notificationCount = 1;
			
			if(pendingIntakes != 0 && forgottenIntakes != 0)
			{
				doseMessage = getString(R.string._msg_doses_fp, forgottenIntakes, pendingIntakes);
				notificationCount = 2;
			}
			else if(pendingIntakes != 0)
				doseMessage = getString(R.string._msg_doses_p, pendingIntakes);
			else if(forgottenIntakes != 0)
				doseMessage = getString(R.string._msg_doses_f, forgottenIntakes);
						
			/*
			Hasher hasher = new Hasher();
			hasher.hash(lowSupplyMessage);
			hasher.hash(doseMessage);
			
			int notificationHash = hasher.getHashCode();
			*/
			
			final String bullet;
			
			if(doseMessage == null || lowSupplyMessage == null)
				bullet = "";
			else
				bullet = Constants.NOTIFICATION_BULLET;
			
			final StringBuilder sb = new StringBuilder();
			
			if(doseMessage != null)
				sb.append(bullet + doseMessage);
			
			if(lowSupplyMessage != null)
			{
				if(doseMessage != null)
					sb.append("\n");
				
				sb.append(bullet + lowSupplyMessage);
				++notificationCount;
			}
			
			final String message = sb.toString();
			
			final RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
			views.setTextViewText(R.id.stat_title, getString(R.string._title_notifications));
			views.setTextViewText(R.id.stat_text, message);
			views.setTextViewText(R.id.stat_time, new SimpleDateFormat("HH:mm").format(DateTime.now().getTime()));

			final Intent intent = new Intent(this, DrugListActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.putExtra(DrugListActivity.EXTRA_DAY, date);
			
			final Notification notification = new Notification();
			notification.icon = R.drawable.ic_stat_pill;
			notification.tickerText = getString(R.string._msg_new_notification);
			notification.flags |= Notification.FLAG_NO_CLEAR;
			notification.defaults = Notification.DEFAULT_ALL;
			notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			notification.contentView = views;
			if(notificationCount > 1)
				notification.number = notificationCount;
			
			
			int messageHash = message.hashCode();
			
			if(messageHash == mLastMessageHash)
				notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
			else
				mLastMessageHash = messageHash;
			
			mNotificationMgr.notify(R.id.notification, notification);			
		}		
	}	

	private void scheduleNextAlarm(Calendar time, Calendar activeDate)
	{
		final int nextDoseTime = mSettings.getNextDoseTime(time);
		final long offset = mSettings.getMillisUntilDoseTimeBegin(time, nextDoseTime);
		
		//final long atTime = SystemClock.elapsedRealtime() + ;
				
		/*Log.d(TAG, "scheduleNextAlarm: atTime=" + new DumbTime(atTime % Constants.MILLIS_PER_DAY));
		Log.d(TAG, "  time=" + DateTime.toString(time));
		Log.d(TAG, "  activeDate=" + DateTime.toString(activeDate));
		Log.d(TAG, "  offset=")*/
		
		Log.d(TAG,"scheduleNextAlarm: next alarm in " + new DumbTime(offset).toString(true));
		
		mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + offset, mOperation);
	}
	
	private void cancelAllAlarms()
	{
		mAlarmMgr.cancel(mOperation);
	}
	
	private static int countOpenIntakes(Calendar date, int doseTime)
	{
		int count = 0;
				
		for(Drug drug : Database.getDrugs())
		{
			final Fraction dose = drug.getDose(doseTime);
			
			if(!drug.isActive() || dose.equals(0) || !drug.hasDoseOnDate(date))
				continue;			
			
			if(Database.findIntakes(drug, date, doseTime).isEmpty())
				++count;								
		}
		
		return count;
		
	}
	
	private static int countForgottenIntakes(Calendar date, int activeDoseTime)
	{
		int count = 0;
		
		for(int doseTime = 0; doseTime != activeDoseTime; ++doseTime)
			count += countOpenIntakes(date, doseTime);
		
		return count;
	}
	
	private String getLowSupplyMessage(Calendar date, int activeDoseTime)
	{
		final List<Drug> drugsWithLowSupply = new ArrayList<Drug>();
		final int minDays = Integer.parseInt(mSharedPrefs.getString("num_min_supply_days", "7"), 10);
		
		for(Drug drug : Database.getDrugs())
		{
			// refill size of zero means ignore supply values
			if (drug.getRefillSize() == 0)
				continue;

			double dailyDose = 0;

			for(int doseTime = 0; doseTime != Drug.TIME_INVALID; ++doseTime)
			{
				final Fraction dose = drug.getDose(doseTime);
				if(dose.compareTo(0) != 0)
					dailyDose += dose.doubleValue();
			}

			if(dailyDose != 0)
			{
				if(Double.compare(drug.getCurrentSupplyDays(), (double) minDays) == -1)
					drugsWithLowSupply.add(drug);
			}
		}
		
		String message = null;
		
		if(!drugsWithLowSupply.isEmpty())
		{
			final String firstDrugName = drugsWithLowSupply.get(0).getName();
			
			if(drugsWithLowSupply.size() == 1)
				message = getString(R.string._msg_low_supply_single, firstDrugName);
			else
				message = getString(R.string._msg_low_supply_multiple, firstDrugName, drugsWithLowSupply.size() - 1);
		}
		
		return message;
	}
}
