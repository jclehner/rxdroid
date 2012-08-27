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
import android.os.Bundle;
import android.util.Log;
import at.caspase.androidutils.EventDispatcher;
import at.caspase.rxdroid.Settings.DoseTimeInfo;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.db.Schedule;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Util;

public class NotificationReceiver extends BroadcastReceiver
{
	private static final String TAG = NotificationReceiver.class.getName();
	private static final boolean LOGV = true;

	private static final Class<?>[] EVENT_HANDLER_ARG_TYPES = { Date.class, Integer.TYPE };

	public interface OnDoseTimeChangeListener
	{
		void onDoseTimeBegin(Date date, int doseTime);
		void onDoseTimeEnd(Date date, int doseTime);
	}

	static final String EXTRA_SILENT = TAG + ".silent";
	//static final String EXTRA_SNOOZE = TAG + ".snooze";
	//static final String EXTRA_CANCEL_SNOOZE = TAG + ".cancel_snooze";
	//static final String EXTRA_SNOOZE_STATE = TAG + ".snooze_state";

	static final String EXTRA_DATE = "at.caspase.rxdroid.extra.DATE";
	static final String EXTRA_DOSE_TIME = "at.caspase.rxdroid.extra.DOSE_TIME";
	static final String EXTRA_IS_DOSE_TIME_END = "at.caspase.rxdroid.extra.IS_DOSE_TIME_END";

	private static final String EXTRA_IS_DELETE_INTENT = TAG + ".is_delete_intent";

	static final int ALARM_MODE_NORMAL = 0;
	static final int ALARM_MODE_REPEAT = 1;

	private Context mContext;
	private AlarmManager mAlarmMgr;
	private List<Drug> mAllDrugs;

	private int mAlarmRepeatMode;

	private boolean mIsDeleteIntent = false;
	private boolean mDoPostSilent = false;

	private static final EventDispatcher<OnDoseTimeChangeListener> sEventMgr =
			new EventDispatcher<OnDoseTimeChangeListener>();

	public static void registerOnDoseTimeChangeListener(OnDoseTimeChangeListener l) {
		sEventMgr.register(l);
	}

	public static void unregisterOnReceiveListener(OnDoseTimeChangeListener l) {
		sEventMgr.unregister(l);
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if(intent == null)
			return;

		Database.init();

		final int doseTime = intent.getIntExtra(EXTRA_DOSE_TIME, Schedule.TIME_INVALID);
		if(doseTime != Schedule.TIME_INVALID)
		{
			final Date date = (Date) intent.getSerializableExtra(EXTRA_DATE);
			final boolean isDoseTimeEnd = intent.getBooleanExtra(EXTRA_IS_DOSE_TIME_END, false);
			final String eventName = isDoseTimeEnd ? "onDoseTimeEnd" : "onDoseTimeBegin";

			sEventMgr.post(eventName, EVENT_HANDLER_ARG_TYPES, date, doseTime);
		}

		mContext = context;
		mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		mAllDrugs = Database.getAll(Drug.class);
		mAlarmRepeatMode = Settings.getListPreferenceValueIndex("alarm_mode", ALARM_MODE_NORMAL);
		mDoPostSilent = intent.getBooleanExtra(EXTRA_SILENT, false);
		mIsDeleteIntent = intent.getBooleanExtra(EXTRA_IS_DELETE_INTENT, false);

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
		if(Settings.getDoseTimeBegin(Drug.TIME_MORNING) == null)
		{
			Log.w(TAG, "No dose-time settings available. Not scheduling alarms.");
			return;
		}

		if(LOGV) Log.d(TAG, "Scheduling next alarms...");

		final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();

		if(dtInfo.activeDoseTime != Schedule.TIME_INVALID)
			scheduleEndAlarm(dtInfo.currentTime, dtInfo.activeDoseTime);
		else
			scheduleBeginAlarm(dtInfo.currentTime, dtInfo.nextDoseTime);
	}

	private void updateCurrentNotifications()
	{
		final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();
		final boolean isActiveDoseTime;

		int doseTime = dtInfo.activeDoseTime;
		if(doseTime == Schedule.TIME_INVALID)
		{
			isActiveDoseTime = false;
			doseTime = dtInfo.nextDoseTime;
		}
		else
			isActiveDoseTime = true;

		updateNotifications(dtInfo.activeDate, doseTime, isActiveDoseTime);
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

		builder.setIcon1(R.drawable.ic_stat_normal);
		builder.setIcon2(R.drawable.ic_stat_exclamation);

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
			offset = Settings.getMillisUntilDoseTimeEnd(time, doseTime);

			if(mAlarmRepeatMode == ALARM_MODE_REPEAT)
				offset = Math.min(offset, Settings.getAlarmTimeout());
		}
		else
			offset = Settings.getMillisUntilDoseTimeBegin(time, doseTime);

		time.add(Calendar.MILLISECOND, (int) offset);

		if(LOGV)
		{
			Log.v(TAG, "Scheduling " + (scheduleEnd ? mAlarmRepeatMode != ALARM_MODE_REPEAT ? "end" : "next alarm" : "begin") +
					" of doseTime " + doseTime + " for " + DateTime.toString(time));
			Log.v(TAG, "Alarm will fire in " + Util.millis(time.getTimeInMillis() - System.currentTimeMillis()));
		}

		final Bundle extras = new Bundle();
		extras.putInt(EXTRA_DOSE_TIME, doseTime);
		extras.putBoolean(EXTRA_IS_DOSE_TIME_END, scheduleEnd);

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
		intent.setAction(Intent.ACTION_MAIN);

		if(extras != null)
			intent.putExtras(extras);

		return PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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

			// XXX is this really neccessary?
			if(drug.isAutoAddIntakesEnabled())
				continue;

			if(Entries.countIntakes(drug, date, doseTime) == 0)
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

			final Date checkDate = DateTime.add(date, Calendar.DAY_OF_MONTH, -1);
			count = countOpenIntakes(checkDate, Drug.TIME_NIGHT);
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

		for(Drug drug : mAllDrugs)
		{
			if(Settings.hasLowSupplies(drug))
				drugsWithLowSupply.add(drug);
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
		final Intent intent = new Intent(context, NotificationReceiver.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.putExtra(NotificationReceiver.EXTRA_SILENT, silent);
		context.sendBroadcast(intent);
	}
}
