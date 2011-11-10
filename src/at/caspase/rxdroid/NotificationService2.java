package at.caspase.rxdroid;

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
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public class NotificationService2 extends Service implements OnDatabaseChangedListener, OnSharedPreferenceChangeListener
{
	private static final String TAG = NotificationService2.class.getName();
	
	//public static final String EXTRA_DONT_CLEAR_NOTIFICATIONS = "dont_clear_notifications";
	private static final String EXTRA_SCHEDULED_START = "scheduled_start";
	private static final String EXTRA_DOSE_TIME = "dose_time";
	private static final String EXTRA_IS_END = "is_end";
	
	private AlarmManager mAlarmMgr;
	private Preferences mSettings;
	private SharedPreferences mSharedPrefs;
	private NotificationManager mNotificationMgr;
	
	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		updateCurrentNotifications(true);
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		updateCurrentNotifications(true);
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags)
	{
		updateCurrentNotifications(true);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		// TODO filter
		cancelNotifications();
		rescheduleAlarms();
	}
	
	@Override
	public void onCreate()
	{
		Log.d(TAG, "onCreate");
		super.onCreate();
		
		GlobalContext.set(getApplicationContext());
		Database.load();
		Database.registerOnChangedListener(this);
		
		mAlarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		mSettings = Preferences.instance();
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
		mNotificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);		
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Database.unregisterOnChangedListener(this);
		mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if(intent.getBooleanExtra(EXTRA_SCHEDULED_START, false))
		{
			int doseTime = intent.getIntExtra(EXTRA_DOSE_TIME, Drug.TIME_INVALID);
			boolean isDoseTimeEnd = intent.getBooleanExtra(EXTRA_IS_END, true);
			
			if(doseTime != Drug.TIME_INVALID)
			{
				updateNotifications(mSettings.getActiveDate(), doseTime, isDoseTimeEnd, false);
				
				if(isDoseTimeEnd)
					scheduleNextAlarms();

				return START_STICKY;
			}
		}
		
		updateCurrentNotifications(false);
		rescheduleAlarms();
			
		return START_STICKY;
	}
	
	private void rescheduleAlarms() 
	{
		cancelAllAlarms();
		scheduleNextAlarms();
	}
	
	private void scheduleNextAlarms()
	{		
		Log.d(TAG, "Scheduling next alarms...");
		
		final Calendar now = DateTime.now();
		int activeDoseTime = mSettings.getActiveDoseTime(now);
		int nextDoseTime = mSettings.getNextDoseTime(now);
		
		if(activeDoseTime != -1)
			scheduleEndAlarm(now, activeDoseTime);
		
		scheduleBeginAlarm(now, nextDoseTime);
	}
	
	private void updateCurrentNotifications(boolean beQuiet)
	{
		Calendar now = DateTime.now();
		int doseTime = mSettings.getActiveDoseTime(now);
		if(doseTime == -1)
			return;
		
		Calendar date = mSettings.getActiveDate(now);
		updateNotifications(date, doseTime, false, beQuiet);
	}
	
	private void updateNotifications(Calendar date, int doseTime, boolean isDoseTimeEnd, boolean beQuiet)
	{
		int pendingIntakes = isDoseTimeEnd ? 0 : countOpenIntakes(date, doseTime);
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
			else
				notificationCount = 0;
			
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
			//views.setTextViewText(R.id.stat_time, new SimpleDateFormat("HH:mm").format(DateTime.now().getTime()));
			views.setTextViewText(R.id.stat_time, "");
			
			final Intent intent = new Intent(this, DrugListActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.putExtra(DrugListActivity.EXTRA_DAY, date);
			
			final Notification notification = new Notification();
			notification.icon = R.drawable.ic_stat_pill;
			notification.tickerText = getString(R.string._msg_new_notification);
			notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;
			notification.defaults |= Notification.DEFAULT_ALL;
			notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			notification.contentView = views;
			if(notificationCount > 1)
				notification.number = notificationCount;			
						
			if(beQuiet)
				notification.defaults ^= Notification.DEFAULT_ALL;
			
			mNotificationMgr.notify(R.id.notification, notification);			
		}		
	}
	
	private void cancelNotifications() {
		mNotificationMgr.cancel(R.id.notification);
	}
	
	private void scheduleBeginAlarm(Calendar time, int doseTime) {
		scheduleNextBeginOrEndAlarm(time, doseTime, false);
	}
	
	private void scheduleEndAlarm(Calendar time, int doseTime) {
		scheduleNextBeginOrEndAlarm(time, doseTime, true);
	}
	
	private void scheduleNextBeginOrEndAlarm(Calendar time, int doseTime, boolean scheduleEnd)
	{
		final long offset;
	
		if(scheduleEnd)
			offset = mSettings.getMillisUntilDoseTimeEnd(time, doseTime);
		else
			offset = mSettings.getMillisUntilDoseTimeBegin(time, doseTime);
		
		time.add(Calendar.MILLISECOND, (int) offset);
		
		Log.d(TAG, "Scheduling " + (scheduleEnd ? "end" : "begin") + " of doseTime " + doseTime + " for " + DateTime.toString(time));
		
		mAlarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), createOperation(doseTime, scheduleEnd));
	}
	
	private void cancelAllAlarms() 
	{
		Log.d(TAG, "Cancelling all alarms...");
		mAlarmMgr.cancel(createEmptyOperation());
	}
	
	private PendingIntent createEmptyOperation() {
		return createOperation(Drug.TIME_INVALID, false);
	}
	
	private PendingIntent createOperation(int doseTime, boolean isEnd) 
	{
		Intent intent = new Intent(this, NotificationService2.class);
		intent.putExtra(EXTRA_SCHEDULED_START, true);
		intent.putExtra(EXTRA_DOSE_TIME, doseTime);
		intent.putExtra(EXTRA_IS_END, isEnd);
		
		return PendingIntent.getService(getApplicationContext(), 0, intent, 0);
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
