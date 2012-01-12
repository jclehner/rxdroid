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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Util;

public class NotificationReceiver extends BroadcastReceiver
{
	private static final String TAG = NotificationReceiver.class.getName();

	private static final boolean LOGV = true;

	//static final String EXTRA_SILENT = "be_quiet";
	static final String EXTRA_SNOOZE = "snooze";
	static final String EXTRA_CANCEL_SNOOZE = "cancel_snooze";

	static final int SNOOZE_DISABLED = 0;
	static final int SNOOZE_REPEAT = 1;
	static final int SNOOZE_MANUAL = 2;

	private Context mContext;

	private AlarmManager mAlarmMgr;
	private Settings mSettings;
	private SharedPreferences mSharedPrefs;

	private int mSnoozeType;

	private MyNotification mNotification;

	private boolean mIsManualSnoozeRequest = false;
	private boolean mIsSnoozeCancelRequest = false;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		GlobalContext.set(context.getApplicationContext());
		Database.load();

		mContext = context;
		mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		mSettings = Settings.instance();
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mSnoozeType = mSettings.getListPreferenceValueIndex("snooze_type", SNOOZE_DISABLED);
		mNotification = new MyNotification(context);

		if(intent != null)
		{
			mIsManualSnoozeRequest = intent.getBooleanExtra(EXTRA_SNOOZE, false);
			mIsSnoozeCancelRequest = intent.getBooleanExtra(EXTRA_CANCEL_SNOOZE, false);
			if(LOGV) Log.d(TAG, "onReceive: snoozeRequest=" + mIsManualSnoozeRequest + ", snoozeCancel=" + mIsSnoozeCancelRequest);
		}
		
		if(mIsManualSnoozeRequest && mSnoozeType != SNOOZE_MANUAL)
			return;

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
		Log.d(TAG, "Scheduling next alarms...");

		final Calendar now = DateTime.now();
		int activeDoseTime = mSettings.getActiveDoseTime(now);
		int nextDoseTime = mSettings.getNextDoseTime(now);

		if(activeDoseTime != -1)
			scheduleEndAlarm(now, activeDoseTime);
		else
			scheduleBeginAlarm(now, nextDoseTime);
	}

	private void updateCurrentNotifications()
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

		Date date = mSettings.getActiveDate(now);
		updateNotifications(date, doseTime, ignorePendingIntakes);
	}

	private void updateNotifications(Date date, int doseTime, boolean ignorePendingIntakes)
	{
		int pendingIntakes = ignorePendingIntakes ? 0 : countOpenIntakes(date, doseTime);
		int forgottenIntakes = countForgottenIntakes(date, doseTime);
		String lowSupplyMessage = getLowSupplyMessage(date, doseTime);

		mNotification.setPendingCount(pendingIntakes);
		mNotification.setForgottenCount(forgottenIntakes);
		mNotification.setLowSupplyMessage(lowSupplyMessage);
		mNotification.setSnoozeMessageEnabled(mIsManualSnoozeRequest);
		
		if(!mIsManualSnoozeRequest)
		{
			boolean forceUpdate = !mIsSnoozeCancelRequest && mSnoozeType == SNOOZE_REPEAT;
			mNotification.update(forceUpdate, mIsSnoozeCancelRequest ? MyNotification.FLAG_SILENT : 0);
		}
		else
			mNotification.update(false, MyNotification.FLAG_SILENT);
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
		{
			if(mSnoozeType == SNOOZE_REPEAT || mIsManualSnoozeRequest)
				offset = mSettings.getSnoozeTime();
			else
				offset = mSettings.getMillisUntilDoseTimeEnd(time, doseTime);
		}
		else
			offset = mSettings.getMillisUntilDoseTimeBegin(time, doseTime);

		time.add(Calendar.MILLISECOND, (int) offset);

		Log.d(TAG, "Scheduling " + (scheduleEnd ? mSnoozeType != SNOOZE_REPEAT ? "end" : "next alarm" : "begin") + " of doseTime " + doseTime + " for " + DateTime.toString(time));
		Log.d(TAG, "Alarm will fire in " + Util.millis(time.getTimeInMillis() - System.currentTimeMillis()));

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
		return PendingIntent.getBroadcast(mContext, 0, intent, 0);
	}

	private static int countOpenIntakes(Date date, int doseTime)
	{
		int count = 0;

		for(Drug drug : Database.getDrugs())
		{
			final Fraction dose = drug.getDose(doseTime);

			if(!drug.isActive() || dose.isZero() || !drug.hasDoseOnDate(date))
				continue;

			if(Intake.findAll(drug, date, doseTime).isEmpty())
				++count;
		}

		return count;

	}

	private static int countForgottenIntakes(Date date, int activeOrNextDoseTime)
	{
		int count = 0;

		for(int doseTime = 0; doseTime != activeOrNextDoseTime; ++doseTime)
			count += countOpenIntakes(date, doseTime);

		return count;
	}

	private String getLowSupplyMessage(Date date, int activeDoseTime)
	{
		final List<Drug> drugsWithLowSupply = new ArrayList<Drug>();
		final int minDays = Integer.parseInt(mSharedPrefs.getString("num_min_supply_days", "7"), 10);

		for(Drug drug : Database.getDrugs())
		{
			// refill size of zero means ignore supply values
			if(!drug.isActive() || drug.getRefillSize() == 0)
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
				if(Double.compare(drug.getCurrentSupplyDays(), minDays) == -1)
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
