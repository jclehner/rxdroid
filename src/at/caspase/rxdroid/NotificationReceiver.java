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
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Util;

public class NotificationReceiver extends BroadcastReceiver
{
	private static final String TAG = NotificationReceiver.class.getName();

	private static final boolean LOGV = true;

	static final String EXTRA_SILENT = TAG + ".silent";
	static final String EXTRA_SNOOZE = TAG + ".snooze";
	static final String EXTRA_CANCEL_SNOOZE = TAG + ".cancel_snooze";
	static final String EXTRA_SNOOZE_STATE = TAG + ".snooze_state";

	private static final String EXTRA_IS_DELETE_INTENT = TAG + ".is_delete_intent";

	static final int ALARM_MODE_NORMAL = 0;
	static final int ALARM_MODE_REPEAT = 1;
	static final int ALARM_MODE_SNOOZE = 2;

	/**
	 * The first notification was posted.
	 * <p>
	 * OnClick behavior:
	 * <ol>
	 * <li>Launch DrugListActivity</li>
	 * <li>Delete current notification</li>
	 * <li>(Display Toast info)</li>
	 * <li>Post new notification with snoozing info</li>
	 * </ol>
	 */
	private static final int SNOOZE_STATE_0 = 0;

	/**
	 * Currently displaying "snoozing" text.
	 * <p>
	 * OnClick behavior:
	 * <ol>
	 * <li>Delete notification</li>
	 * <li>Replace with current notification, but w/o snooze behaviour</li>
	 * </ol>
	 */
	private static final int SNOOZE_STATE_1 = 1;

	/**
	 * Snoozing was cancelled. Behave as if snoozing was disabled.
	 */
	private static final int SNOOZE_STATE_2 = 2;


	private Context mContext;
	private AlarmManager mAlarmMgr;
	private Settings mSettings;
	private List<Drug> mAllDrugs;

	private int mAlarmRepeatMode;

	private boolean mIsManualSnoozeRequest = false;
	private boolean mIsSnoozeCancelRequest = false;
	private boolean mIsDeleteIntent = false;
	private boolean mDoPostSilent = false;

	private int mSnoozeState = -1;

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

		mIsManualSnoozeRequest = intent.getBooleanExtra(EXTRA_SNOOZE, false);
		mIsSnoozeCancelRequest = intent.getBooleanExtra(EXTRA_CANCEL_SNOOZE, false);
		mDoPostSilent = intent.getBooleanExtra(EXTRA_SILENT, false);
		mIsDeleteIntent = intent.getBooleanExtra(EXTRA_IS_DELETE_INTENT, false);
		mSnoozeState = intent.getIntExtra(EXTRA_SNOOZE_STATE, -1);

		if(LOGV)
		{
			Log.d(TAG,
					"onReceive\n" +
					"  is delete intent       : " + mIsDeleteIntent + "\n" +
					"  manual snooze request  : " + mIsManualSnoozeRequest + "\n" +
					"  snooze cancel request  : " + mIsSnoozeCancelRequest + "\n" +
					"  post silent notifcation: " + mDoPostSilent + "\n" +
					"  current snooze state   : " + mSnoozeState + "\n");
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
		Log.d(TAG, "Scheduling next alarms...");

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
		MyNotification.Builder builder = new MyNotification.Builder(mContext);

		/*if(mAlarmRepeatMode == ALARM_MODE_SNOOZE && mSnoozeState != SNOOZE_STATE_2)
		{
			final int nextSnoozeState;

			// if this was *not* a deleteIntent, the snooze timeout has passed, so we display
			// the notification again, resetting the snooze state.

			if(mIsDeleteIntent)
				nextSnoozeState = mSnoozeState + 1;
			else
				nextSnoozeState = -1;

			final Intent intent = new Intent(mContext, NotificationReceiver.class);
			intent.putExtra(EXTRA_SNOOZE_STATE, nextSnoozeState);
			intent.putExtra(EXTRA_IS_DELETE_INTENT, true);

			final PendingIntent deleteIntent =
					PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
			builder.setDeleteIntent(deleteIntent);
			builder.addFlags(Notification.FLAG_AUTO_CANCEL);

			if(LOGV) Log.v(TAG, "updateNotifications: snooze state was " + mSnoozeState);

			if(mSnoozeState == SNOOZE_STATE_0)
			{
				builder.setMessage1(R.string._msg_snoozing);
				builder.setContentIntent(createDrugListIntent(date));
				builder.post();
				return;
			}

			if(LOGV) Log.v(TAG, "  posting normal notification");
		}*/

		final String message2 = getLowSupplyMessage(date, doseTime);
		int pendingCount = isActiveDoseTime ? countOpenIntakes(date, doseTime) : 0;
		int forgottenCount = countForgottenIntakes(date, doseTime, isActiveDoseTime);

		if((pendingCount + forgottenCount) == 0 && message2 == null)
		{
			builder.cancel();
			return;
		}

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
		final Bundle extras = new Bundle();
		boolean isSnoozeIntent = false;
		final long offset;

		if(scheduleEnd)
		{
			final long offsetEnd = mSettings.getMillisUntilDoseTimeEnd(time, doseTime);
			final long snoozeTime = mSettings.getAlarmTimeout();

			if(LOGV)
			{
				Log.v(TAG, "scheduleNextBeginOrEndAlarm\n" +
					"  offsetEnd=" + offsetEnd + "\n" +
					"  snoozeTime=" + snoozeTime + "\n" +
					"  min: " + Math.min(offsetEnd, snoozeTime) + "\n");
			}

			switch(mAlarmRepeatMode)
			{
				case ALARM_MODE_REPEAT:
					offset = Math.min(offsetEnd, snoozeTime);
					break;

				case ALARM_MODE_SNOOZE:
					if(mIsDeleteIntent)
					{
						offset = Math.min((mSnoozeState == SNOOZE_STATE_0 ? snoozeTime : offsetEnd), offsetEnd);
						break;
					}
					// else fall through!

				default:
					offset = offsetEnd;
			}
		}
		else
			offset = mSettings.getMillisUntilDoseTimeBegin(time, doseTime);

		time.add(Calendar.MILLISECOND, (int) offset);

		if(LOGV)
		{
			Log.d(TAG, "Scheduling " + (scheduleEnd ? mAlarmRepeatMode != ALARM_MODE_REPEAT ? "end" : "next alarm" : "begin") +
					" of doseTime " + doseTime + " for " + DateTime.toString(time));
			Log.d(TAG, "Alarm will fire in " + Util.millis(time.getTimeInMillis() - System.currentTimeMillis()));
		}

		//extras.putBoolean(EXTRA_SNOOZE, isSnoozeIntent);

		mAlarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), createOperation(extras));
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
			final Fraction dose = drug.getDose(doseTime);

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
			Date yesterday = DateTime.add(date, Calendar.DAY_OF_MONTH, -1);
			count = countOpenIntakes(yesterday, Drug.TIME_NIGHT);
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
