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
import java.util.Date;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Util;

public class NotificationReceiver extends BroadcastReceiver
{
	private static final String TAG = NotificationReceiver.class.getName();

	private static final boolean LOGV = true;

	static final String EXTRA_SILENT = TAG + ".silent";
	//static final String EXTRA_SNOOZE = TAG + ".snooze";
	//static final String EXTRA_CANCEL_SNOOZE = TAG + ".cancel_snooze";
	//static final String EXTRA_SNOOZE_STATE = TAG + ".snooze_state";

	private static final String EXTRA_IS_DELETE_INTENT = TAG + ".is_delete_intent";

	static final int ALARM_MODE_NORMAL = 0;
	static final int ALARM_MODE_REPEAT = 1;

	private Context mContext;
	private AlarmManager mAlarmMgr;
	private Settings mSettings;
	private List<Drug> mAllDrugs;

	private int mAlarmRepeatMode;

	private boolean mIsDeleteIntent = false;
	private boolean mDoPostSilent = false;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if(intent == null)
			return;

		GlobalContext.set(context.getApplicationContext());
		Database.init();

		mContext = context;
		mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		mSettings = Settings.instance();
		mAllDrugs = Database.getAll(Drug.class);

		mAlarmRepeatMode = mSettings.getListPreferenceValueIndex("alarm_mode", ALARM_MODE_NORMAL);

		//mIsManualSnoozeRequest = intent.getBooleanExtra(EXTRA_SNOOZE, false);
		//mIsSnoozeCancelRequest = intent.getBooleanExtra(EXTRA_CANCEL_SNOOZE, false);
		mDoPostSilent = intent.getBooleanExtra(EXTRA_SILENT, false);
		mIsDeleteIntent = intent.getBooleanExtra(EXTRA_IS_DELETE_INTENT, false);
		//mSnoozeState = intent.getIntExtra(EXTRA_SNOOZE_STATE, -1);

		if(LOGV)
		{
			Log.d(TAG,
					"onReceive\n" +
					"  is delete intent       : " + mIsDeleteIntent + "\n" +
					"  post silent notifcation: " + mDoPostSilent + "\n");
		}

		rescheduleAlarms();
		updateCurrentNotifications();
	}

	private void rescheduleAlarms()
	{
		cancelAllAlarms();
		scheduleNextAlarms();
	}

	private void scheduleNextAlarms()
	{
		if(mSettings.getDoseTimeBegin(Drug.TIME_MORNING) == null)
		{
			Log.w(TAG, "No dose-time settings available. Not scheduling alarms.");
			return;
		}


		Log.d(TAG, "Scheduling next alarms...");

		@SuppressWarnings("deprecation")
		final Calendar now = DateTime.nowCalendar();
		int activeDoseTime = mSettings.getActiveDoseTime(now);
		int nextDoseTime = mSettings.getNextDoseTime(now);

		if(activeDoseTime != -1)
			scheduleEndAlarm(now, activeDoseTime);
		else
			scheduleBeginAlarm(now, nextDoseTime);
	}

	private void updateCurrentNotifications()
	{
		@SuppressWarnings("deprecation")
		Calendar now = DateTime.nowCalendar();
		final boolean isActiveDoseTime;
		int doseTime = mSettings.getActiveDoseTime(now);
		if(doseTime == -1)
		{
			isActiveDoseTime = false;
			doseTime = mSettings.getNextDoseTime(now);
		}
		else
			isActiveDoseTime = true;

		Date date = mSettings.getActiveDate(now);
		updateNotifications(date, doseTime, isActiveDoseTime);
	}

	private void updateNotifications(Date date, int doseTime, boolean isActiveDoseTime)
	{
		final MyNotification.Builder builder = new MyNotification.Builder(mContext);

		final String message2 = getLowSupplyMessage(date, doseTime);
		int pendingCount = isActiveDoseTime ? countOpenIntakes(date, doseTime) : 0;
		int forgottenCount = countForgottenIntakes(date, doseTime, isActiveDoseTime);

		if((pendingCount + forgottenCount) == 0 && message2 == null)
		{
			builder.cancel();
			return;
		}

		builder.setTitle1(R.string._title_notification_doses);
		builder.setTitle2(R.string._title_notification_low_supplies);

		if(pendingCount != 0 && forgottenCount != 0)
			builder.setMessage1(R.string._msg_doses_fp, forgottenCount, pendingCount);
		else if(pendingCount != 0)
			builder.setMessage1(R.string._msg_doses_p, pendingCount);
		else if(forgottenCount != 0)
			builder.setMessage1(R.string._msg_doses_f, forgottenCount);

		builder.setMessage2(message2);
		builder.setForceUpdate(mAlarmRepeatMode == ALARM_MODE_REPEAT);
		builder.setContentIntent(createDrugListIntent(date));
		builder.setPersistent(true);

		if(!mDoPostSilent)
			builder.setDefaults(Notification.DEFAULT_ALL);

		builder.post();
	}

	private void scheduleBeginAlarm(Calendar time, int doseTime) {
		scheduleNextBeginOrEndAlarm(time, doseTime, false);
	}

	private void scheduleEndAlarm(Calendar time, int doseTime) {
		scheduleNextBeginOrEndAlarm(time, doseTime, true);
	}

	private void scheduleNextBeginOrEndAlarm(Calendar time, int doseTime, boolean scheduleEnd)
	{
		long offset;

		if(scheduleEnd)
		{
			offset = mSettings.getMillisUntilDoseTimeEnd(time, doseTime);

			if(mAlarmRepeatMode == ALARM_MODE_REPEAT)
				offset = Math.min(offset, mSettings.getAlarmTimeout());
		}
		else
			offset = mSettings.getMillisUntilDoseTimeBegin(time, doseTime);

		time.add(Calendar.MILLISECOND, (int) offset);

		if(LOGV)
		{
			Log.v(TAG, "Scheduling " + (scheduleEnd ? mAlarmRepeatMode != ALARM_MODE_REPEAT ? "end" : "next alarm" : "begin") +
					" of doseTime " + doseTime + " for " + DateTime.toString(time));
			Log.v(TAG, "Alarm will fire in " + Util.millis(time.getTimeInMillis() - System.currentTimeMillis()));
		}

		mAlarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), createOperation(null));
	}

	private void cancelAllAlarms()
	{
		Log.d(TAG, "Cancelling all alarms...");
		mAlarmMgr.cancel(createOperation(null));
	}

	private PendingIntent createOperation(Bundle extras)
	{
		Intent intent = new Intent(mContext, NotificationReceiver.class);
		if(extras != null)
			intent.putExtras(extras);

		return PendingIntent.getBroadcast(mContext, 0, intent, 0);
	}

	private PendingIntent createDrugListIntent(Date date)
	{
		Intent intent = new Intent(mContext, DrugListActivity.class);
		intent.putExtra(DrugListActivity.EXTRA_STARTED_FROM_NOTIFICATION, true);
		intent.putExtra(DrugListActivity.EXTRA_DATE, date);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

		return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private int countOpenIntakes(Date date, int doseTime)
	{
		int count = 0;

		for(Drug drug : mAllDrugs)
		{
			final Fraction dose = drug.getDose(doseTime, date);

			if(!drug.isActive() || dose.isZero() || !drug.hasDoseOnDate(date) || drug.getRepeatMode() == Drug.REPEAT_ON_DEMAND)
				continue;

			if(Intake.findAll(drug, date, doseTime).isEmpty())
				++count;
		}

		return count;

	}

	private int countForgottenIntakes(Date date, int activeOrNextDoseTime, boolean isActiveDoseTime)
	{
		int count = 0;

		if(!isActiveDoseTime && activeOrNextDoseTime == Drug.TIME_MORNING)
		{
			// If Drug.TIME_NIGHT ends after midnight, we must adjust the date accordingly to display
			// the correct notifications.

			/*final Settings settings = Settings.instance();
			final Calendar cal = DateTime.calendarFromDate(date);

			if(settings.getTrueDoseTimeEndOffset(Drug.TIME_NIGHT) >= Constants.MILLIS_PER_DAY)
				cal.add(Calendar.DAY_OF_MONTH, -1);*/

			final Date checkDate = DateTime.add(date, Calendar.DAY_OF_MONTH, -1);
			count = countOpenIntakes(checkDate, Drug.TIME_NIGHT);

			if(LOGV) Log.v(TAG, "countForgottenIntakes: checkDate=" + checkDate + ", count=" + count);

			//if(LOGV) Log.v(TAG, "countFrgottenIntakes: cal=" + DateTime.toString(cal));
		}
		else
		{
			for(int doseTime = Drug.TIME_MORNING; doseTime != activeOrNextDoseTime; ++doseTime)
				count += countOpenIntakes(date, doseTime);
		}

		return count;
	}

	private String getLowSupplyMessage(Date date, int activeDoseTime)
	{
		final List<Drug> drugsWithLowSupply = new ArrayList<Drug>();
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		final int minDays = Integer.parseInt(sp.getString("num_min_supply_days", "7"), 10);

		for(Drug drug : mAllDrugs)
		{
			// Refill size of zero means ignore supply values
			if(!drug.isActive() || drug.getRefillSize() == 0)
				continue;

			double dailyDose = 0;

			for(int doseTime = 0; doseTime != Drug.TIME_INVALID; ++doseTime)
			{
				final Fraction dose = drug.getDose(doseTime, date);
				if(dose.compareTo(0) != 0)
					dailyDose += dose.doubleValue();
			}

			if(dailyDose != 0)
			{
				if(Double.compare(Entries.getSupplyDaysLeftForDrug(drug, null), minDays) == -1)
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

	private String getString(int resId, Object... formatArgs) {
		return mContext.getString(resId, formatArgs);
	}

	/* package */ static void sendBroadcastToSelf(boolean silent) {
		sendBroadcastToSelf(null, silent);
	}

	/* package */ static void sendBroadcastToSelf(Context context, boolean silent)
	{
		if(context == null)
			context = GlobalContext.get();
		Intent intent = new Intent(context, NotificationReceiver.class);
		intent.putExtra(NotificationReceiver.EXTRA_SILENT, silent);
		context.sendBroadcast(intent);
	}


	/*static
	{
		final Runnable r = new Runnable() {

			@Override
			public void run()
			{
				while(GlobalContext.get(true) == null)
				{
					Log.i(TAG, "Waiting for context...");

					try
					{
						wait(100);
					}
					catch (InterruptedException e)
					{
						Log.e(TAG, "Thread while waiting for context");
						return;
					}
				}

				Log.i(TAG, "Got context, registering database listener");

				NotificationReceiver.sendBroadcastToSelf(false);

				Database.registerOnChangedListener(new Database.OnChangeListener() {

					@Override
					public void onEntryUpdated(Entry entry, int flags) {
						NotificationReceiver.sendBroadcastToSelf(entry instanceof Intake);
					}

					@Override
					public void onEntryDeleted(Entry entry, int flags) {
						NotificationReceiver.sendBroadcastToSelf(false);
					}

					@Override
					public void onEntryCreated(Entry entry, int flags) {
						NotificationReceiver.sendBroadcastToSelf(false);
					}
				});
			}
		};

		final Thread t = new Thread(r);
		t.setName("DatabaseWatcherRegistrar");
		t.start();
	}*/
}
