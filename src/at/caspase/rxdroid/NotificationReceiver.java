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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public class NotificationReceiver extends BroadcastReceiver
{
	private static final String TAG = NotificationReceiver.class.getName();
	
	private static final String EXTRA_STARTED_BY_ALARM = "started_by_alarm";
	
	private Context mContext;
	
	private AlarmManager mAlarmMgr;
	private Settings mSettings;
	private SharedPreferences mSharedPrefs;
	private NotificationManager mNotificationMgr;
	
	static public void sendInitialBroadcast(Context context, boolean beQuiet)
	{
		Intent intent = new Intent(context, NotificationReceiver.class);
		intent.putExtra(EXTRA_STARTED_BY_ALARM, beQuiet);
		context.sendBroadcast(intent);
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d(TAG, "onReceive(" + context + ", " + intent + ")");
		
		GlobalContext.set(context.getApplicationContext());
		Database.load();
		
		mContext = context;
		mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		mSettings = Settings.instance();
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mNotificationMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		final boolean beQuiet = intent != null && intent.getBooleanExtra(EXTRA_STARTED_BY_ALARM, false);
				
		rescheduleAlarms();
		updateCurrentNotifications(beQuiet);		
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
		final boolean ignorePendingIntakes;
		int doseTime = mSettings.getActiveDoseTime(now);
		if(doseTime == -1)
		{
			ignorePendingIntakes = true;
			doseTime = mSettings.getNextDoseTime(now);
		}
		else
			ignorePendingIntakes = false;
		
		Calendar date = mSettings.getActiveDate(now);
		updateNotifications(date, doseTime, ignorePendingIntakes, beQuiet);
	}
	
	private void updateNotifications(Calendar date, int doseTime, boolean ignorePendingIntakes, boolean beQuiet)
	{
		int pendingIntakes = ignorePendingIntakes ? 0 : countOpenIntakes(date, doseTime);
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
			
			final RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.notification);
			views.setTextViewText(R.id.stat_title, getString(R.string._title_notifications));
			views.setTextViewText(R.id.stat_text, message);
			//views.setTextViewText(R.id.stat_time, new SimpleDateFormat("HH:mm").format(DateTime.now().getTime()));
			views.setTextViewText(R.id.stat_time, "");
			
			final Intent intent = new Intent(mContext, DrugListActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.putExtra(DrugListActivity.EXTRA_DAY, date);
			
			final Notification notification = new Notification();
			notification.icon = R.drawable.ic_stat_pill;
			notification.tickerText = getString(R.string._msg_new_notification);
			notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;
			notification.defaults |= Notification.DEFAULT_ALL;
			notification.contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
			notification.contentView = views;
			if(notificationCount > 1)
				notification.number = notificationCount;			
			
			int messageHash = message.hashCode();
			if(mSettings.getLastNotificationMessageHash() != messageHash)
			{
				mSettings.setLastNotificationMessageHash(messageHash);
				notification.flags ^= Notification.FLAG_ONLY_ALERT_ONCE;				
			}			
			else if(beQuiet)
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
		Log.d(TAG, "Alarm will fire in " + (time.getTimeInMillis() - System.currentTimeMillis()) + "ms");
		
		mAlarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), createOperation());
	}
	
	private void cancelAllAlarms() 
	{
		Log.d(TAG, "Cancelling all alarms...");
		mAlarmMgr.cancel(createOperation());
	}
	
	private PendingIntent createOperation() 
	{
		Intent intent = new Intent(mContext, NotificationReceiver.class);
		intent.putExtra(EXTRA_STARTED_BY_ALARM, true);
				
		return PendingIntent.getBroadcast(mContext, 0, intent, 0);
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
	
	private String getString(int resId) {
		return mContext.getString(resId);
	}
	
	private String getString(int resId, Object... formatArgs) {
		return mContext.getString(resId, formatArgs);
	}

}
