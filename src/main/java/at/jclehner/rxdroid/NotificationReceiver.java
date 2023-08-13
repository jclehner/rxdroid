/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

package at.jclehner.rxdroid;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationManagerCompat;
import android.text.Html;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.jclehner.androidutils.EventDispatcher;
import at.jclehner.rxdroid.Settings.DoseTimeInfo;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.preferences.TimePeriodPreference.TimePeriod;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Millis;
import at.jclehner.rxdroid.util.Util;

public class NotificationReceiver extends BroadcastReceiver
{
	private static final String TAG = NotificationReceiver.class.getSimpleName();
	private static final boolean LOGV = BuildConfig.DEBUG;

	private static final int[] IDS = {
			R.id.notification,
			R.id.notification + 1,
			R.id.notification + 2
	};

	private static final boolean USE_WEARABLE_HACK = true && Version.SDK_IS_JELLYBEAN_OR_NEWER;

	private static final int LED_CYCLE_MS = 5000;
	private static final int LED_ON_MS = 500;
	private static final int LED_OFF_MS = LED_CYCLE_MS - LED_ON_MS;

	private static final Class<?>[] EVENT_HANDLER_ARG_TYPES = { Date.class, int.class };

	public interface OnDoseTimeChangeListener
	{
		void onDoseTimeBegin(Date date, int doseTime);
		void onDoseTimeEnd(Date date, int doseTime);
	}

	static final String EXTRA_SILENT = "at.jclehner.rxdroid.extra.SILENT";
	static final String EXTRA_DATE = "at.jclehner.rxdroid.extra.DATE";
	static final String EXTRA_DOSE_TIME = "at.jclehner.rxdroid.extra.DOSE_TIME";
	static final String EXTRA_IS_DOSE_TIME_END = "at.jclehner.rxdroid.extra.IS_DOSE_TIME_END";
	static final String EXTRA_IS_ALARM_REPETITION = "at.jclehner.rxdroid.extra.IS_ALARM_REPETITION";
	static final String EXTRA_FORCE_UPDATE = "at.jclehner.rxdroid.extra.FORCE_UPDATE";
	static final String EXTRA_REFILL_SNOOZE_DRUGS = "drug_id_list";

	static final String CHANNEL_DEFAULT = "rxdroid_channel_1";
	static final String CHANNEL_QUIET = "rxdroid_channel_quiet";

	private static final String ACTION_MARK_ALL_AS_TAKEN = "at.jclehner.rxdroid.ACTION_MARK_ALL_AS_TAKEN";

	/**
	 * Refill-reminder snooze concept:
	 *
	 * - If displaying *only* the refill reminder (no dose notifications),
	 *   add an action to "remind tomorrow".
	 * - If the action is clicked, store the date that is "tomorrow", and
	 *   update notifications.
	 * - When updating notifications, check the date against the stored date
	 *   and skip the refill reminder if applicable.
	 * - For now (and simplicity's sake) clear that date when changing system
	 *   date/time/timezone).
	 *
	 */
	private static final String ACTION_SNOOZE_REFILL_REMINDER = "snooze";
	// Drugs affected by the refill reminder's snoozing
	private static final String REFILL_REMINDER_SNOOZE_DRUGS = "refill_reminder_snooze_drugs";

	private static final int NOTIFICATION_NORMAL = 0;
	private static final int NOTIFICATION_FORCE_UPDATE = 1;
	private static final int NOTIFICATION_FORCE_SILENT = 2;

	private static final int ID_NORMAL = R.id.notification;
	private static final int ID_WEARABLE = 1;
	private static final int ID_ERROR = 5;

	private Context mContext;
	private AlarmManager mAlarmMgr;
	private DoseTimeInfo mDtInfo;

	private List<Drug> mAllDrugs;

	private boolean mDoPostSilent = false;
	private boolean mForceUpdate = false;

	private boolean mUseWearableHack = USE_WEARABLE_HACK;

	private static final EventDispatcher<OnDoseTimeChangeListener> sEventMgr =
			new EventDispatcher<OnDoseTimeChangeListener>();

	public static void registerOnDoseTimeChangeListener(OnDoseTimeChangeListener l) {
		sEventMgr.register(l);
	}

	public static void unregisterOnDoseTimeChangeListener(OnDoseTimeChangeListener l) {
		sEventMgr.unregister(l);
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if(intent == null)
			return;

		mContext = context;

		Settings.init();
		mDtInfo = Settings.getDoseTimeInfo();

		try
		{
			Database.init();
		}
		catch(DatabaseHelper.DatabaseError e)
		{
			handleDatabaseError(e);
			return;
		}

		getNotificationManager().cancel(ID_ERROR);

		mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		mDoPostSilent = intent.getBooleanExtra(EXTRA_SILENT, false);
		mAllDrugs = Database.getAll(Drug.class);

		final Date date = (Date) intent.getSerializableExtra(EXTRA_DATE);

		if(ACTION_MARK_ALL_AS_TAKEN.equals(intent.getAction()))
		{
			Entries.markAllNotifiedDosesAsTaken(0);
			//
		}
		else if(ACTION_SNOOZE_REFILL_REMINDER.equals(intent.getAction()))
		{
			final Date nextReminderDate = DateTime.add(
					mDtInfo.displayDate(), Calendar.DAY_OF_MONTH, 1);
			Settings.putDate(Settings.Keys.NEXT_REFILL_REMINDER_DATE, nextReminderDate);

			final String drugIds = Settings.getString(REFILL_REMINDER_SNOOZE_DRUGS, "");
			Settings.putString(REFILL_REMINDER_SNOOZE_DRUGS, drugIds + " " +
					intent.getStringExtra(EXTRA_REFILL_SNOOZE_DRUGS));

			// Pre-Jellybean has no actions, so we let the user know what he just did
			// by showing a Toast.
			if(!Version.SDK_IS_JELLYBEAN_OR_NEWER)
				RxDroid.toastLong(R.string._toast_remind_tomorrow);
		}
		else
		{
			final boolean isAlarmRepetition = intent.getBooleanExtra(EXTRA_IS_ALARM_REPETITION, false);

			final int doseTime = intent.getIntExtra(EXTRA_DOSE_TIME, Schedule.TIME_INVALID);
			if(doseTime != Schedule.TIME_INVALID)
			{
				if(!isAlarmRepetition)
				{

					final boolean isDoseTimeEnd = intent.getBooleanExtra(EXTRA_IS_DOSE_TIME_END, false);
					final String eventName = isDoseTimeEnd ? "onDoseTimeEnd" : "onDoseTimeBegin";

					sEventMgr.post(eventName, EVENT_HANDLER_ARG_TYPES, date, doseTime);
				}
			}

			mForceUpdate = isAlarmRepetition ? true : intent.getBooleanExtra(EXTRA_FORCE_UPDATE, false);
			rescheduleAlarms();
		}

		if(mUseWearableHack)
		{
			try
			{
				context.getPackageManager().getApplicationInfo("com.google.android.wearable.app", 0);
				Log.i(TAG, "Found Android Wear app; using wearable hack");
			}
			catch(PackageManager.NameNotFoundException e)
			{
				Log.i(TAG, "Would use wearable hack, but Android Wear app not found!");
				mUseWearableHack = false;
			}
		}

		updateCurrentNotifications();
	}

	private void handleDatabaseError(DatabaseHelper.DatabaseError e)
	{
		final NotificationCompat.Builder nb = new NotificationCompat.Builder(mContext,
				CHANNEL_DEFAULT);
		nb.setSmallIcon(R.drawable.ic_stat_exclamation);
		nb.setContentTitle(getString(R.string._title_database));
		nb.setContentText(Util.getDbErrorMessage(mContext, e));
		nb.setContentIntent(createDrugListIntent(null));
		nb.setColor(Theme.getColorAttribute(R.attr.colorPrimary));
		nb.setOngoing(true);

		getNotificationManager().notify(ID_ERROR, nb.build());
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

		if(LOGV) Log.i(TAG, "Scheduling next alarms...");

		final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();

		if(dtInfo.activeDoseTime() != Schedule.TIME_INVALID)
			scheduleNextBeginOrEndAlarm(dtInfo, true);
		else
			scheduleNextBeginOrEndAlarm(dtInfo, false);
	}

	private void updateCurrentNotifications()
	{
		final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();
		final boolean isActiveDoseTime;

		Date date = dtInfo.activeDate();
		int doseTime = dtInfo.activeDoseTime();

		if(doseTime == Schedule.TIME_INVALID)
		{
			isActiveDoseTime = false;
			doseTime = dtInfo.nextDoseTime();
			date = dtInfo.nextDoseTimeDate();
		}
		else
			isActiveDoseTime = true;

		final int mode;

		if(mForceUpdate)
			mode = NOTIFICATION_FORCE_UPDATE;
		else if(mDoPostSilent)
			mode = NOTIFICATION_FORCE_SILENT;
		else
			mode = NOTIFICATION_NORMAL;

		updateNotification(date, doseTime, isActiveDoseTime, mode);
	}

	private void scheduleNextBeginOrEndAlarm(DoseTimeInfo dtInfo, boolean scheduleEnd)
	{
		final int doseTime = scheduleEnd ? dtInfo.activeDoseTime() : dtInfo.nextDoseTime();
		final Calendar time = dtInfo.currentTime();
		final Date doseTimeDate = scheduleEnd ? dtInfo.activeDate() : dtInfo.nextDoseTimeDate();

		final Bundle alarmExtras = new Bundle();
		alarmExtras.putSerializable(EXTRA_DATE, doseTimeDate);
		alarmExtras.putInt(EXTRA_DOSE_TIME, doseTime);
		alarmExtras.putBoolean(EXTRA_IS_DOSE_TIME_END, scheduleEnd);
		alarmExtras.putBoolean(EXTRA_SILENT, false);

		long offset;

		if(scheduleEnd)
			offset = Settings.getMillisUntilDoseTimeEnd(time, doseTime);
		else
			offset = Settings.getMillisUntilDoseTimeBegin(time, doseTime);

		long triggerAtMillis = time.getTimeInMillis() + offset;

		final int alarmRepeatMins = Settings.getStringAsInt(Settings.Keys.ALARM_REPEAT, 0);
		final long alarmRepeatMillis = alarmRepeatMins == -1 ? Millis.seconds(10) : Millis.minutes(alarmRepeatMins);

		if(alarmRepeatMillis > 0)
		{
			alarmExtras.putBoolean(EXTRA_FORCE_UPDATE, true);

			final long base = dtInfo.activeDate().getTime();
			int i = 0;

			while(base + (i * alarmRepeatMillis) < time.getTimeInMillis())
				++i;

			// We must tell the receiver whether the alarm is an actual dose time's
			// end or begin, or merely a repetition.

			final long triggerAtMillisWithRepeatedAlarm = base + i * alarmRepeatMillis;
			if(triggerAtMillisWithRepeatedAlarm < triggerAtMillis)
			{
				triggerAtMillis = triggerAtMillisWithRepeatedAlarm;
				alarmExtras.putBoolean(EXTRA_IS_ALARM_REPETITION, true);
			}

			//triggerAtMillis = base + (i * alarmRepeatMillis);
		}

		final long triggerDiffFromNow = triggerAtMillis - System.currentTimeMillis();
		if(triggerDiffFromNow < 0)
		{
			if(triggerDiffFromNow < Millis.seconds(-5))
				Log.w(TAG, "Alarm time is in the past by less than 5 seconds.");
			else
			{
				Log.w(TAG, "Alarm time is in the past. Ignoring...");
				return;
			}
		}

		if(alarmExtras.getBoolean(EXTRA_IS_ALARM_REPETITION))
			Log.i(TAG, "Scheduling next alarm for " + DateTime.toString(triggerAtMillis));
		else
		{
			Log.i(TAG, "Scheduling " + (scheduleEnd ? "end" : "begin") + " of doseTime " +
					doseTime + " on date " + DateTime.toDateString(doseTimeDate) + " for " +
					DateTime.toString(triggerAtMillis));
		}

		Log.i(TAG, "Alarm will go off in " + Util.millis(triggerDiffFromNow));

		setAlarm(triggerAtMillis, createOperation(alarmExtras));
	}

	private void setAlarm(long triggerAtMillis, PendingIntent operation)
	{
		final int mode = AlarmManager.RTC_WAKEUP;

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			mAlarmMgr.set(mode, triggerAtMillis, operation);
		else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			mAlarmMgr.setExact(mode, triggerAtMillis, operation);
		else
			mAlarmMgr.setExactAndAllowWhileIdle(mode, triggerAtMillis, operation);
	}

	private void cancelAllAlarms() {
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
		final Intent intent = new Intent(mContext, DrugListActivity2.class);
		intent.putExtra(DrugListActivity2.EXTRA_STARTED_FROM_NOTIFICATION, true);
		//intent.putExtra(DrugListActivity2.EXTRA_DATE, date);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

		return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public void updateNotification(Date date, int doseTime, boolean isActiveDoseTime, int mode)
	{
		new MyNotificationBuilder(date, doseTime,
				isActiveDoseTime, mode).update();
	}

	private static boolean isNowWithinQuietHours()
	{
		if(!Settings.isChecked(Settings.Keys.QUIET_HOURS, false))
			return false;

		final String quietHoursStr = Settings.getString(Settings.Keys.QUIET_HOURS);
		if(quietHoursStr == null)
			return false;

		final TimePeriod quietHours = TimePeriod.fromString(quietHoursStr);
		return quietHours.contains(DumbTime.now());
	}

	class MyNotificationBuilder
	{
		private final Date mDate;
		private final int mMode;

		private final List<Drug> mLowSupplyDrugs = new ArrayList<Drug>();
		private final int missedDoseCount;
		private final int dueDoseCount;

		private boolean mUseGroups = true;
		private String mGroup = mUseGroups ? "rxdroid" : null;

		private Notification mNtfSummary;
		private Notification mNtfDoses;
		private Notification mNtfRefill;

		private CharSequence mTextDoses;
		private CharSequence mTextRefill;

		private boolean mNoClear = true;

		public MyNotificationBuilder(Date date, int doseTime, boolean isActiveDoseTime, int mode)
		{
			mDate = date;
			mMode = mode;

			getDrugsWithLowSupplies(date, doseTime, mLowSupplyDrugs);
			missedDoseCount = getDrugsWithMissedDoses(date, doseTime, isActiveDoseTime);
			dueDoseCount = isActiveDoseTime ? getDrugsWithDueDoses(date, doseTime) : 0;
		}

		public void update()
		{
			buildDosesNotification();
			buildRefillReminderNotification();
			buildSummaryNotification();

			final NotificationManagerCompat nm = NotificationManagerCompat.from(mContext);

			if(mNtfSummary == null)
			{
				cancelAllNotifications();
				return;
			}
			else
				cancelWearNotifications();

			int i = 0;

			nm.notify(IDS[i], mNtfSummary);

			if(mUseGroups)
			{
				for(Notification n : getPages())
					nm.notify(IDS[++i], n);
			}
		}

		private void cancelWearNotifications()
		{
			final NotificationManagerCompat nm = NotificationManagerCompat.from(mContext);

			for(int i = 1; i != IDS.length; ++i)
				nm.cancel(IDS[i]);
		}

		private void buildDosesNotification()
		{
			if(missedDoseCount == 0 && dueDoseCount == 0)
			{
				mNtfDoses = null;
				mTextDoses = null;
				return;
			}

			final StringBuilder sb = new StringBuilder();

			if(dueDoseCount != 0)
				sb.append(RxDroid.getQuantityString(R.plurals._qmsg_due, dueDoseCount));

			if(missedDoseCount != 0)
			{
				if(sb.length() != 0)
					sb.append(", ");

				sb.append(RxDroid.getQuantityString(R.plurals._qmsg_missed, missedDoseCount));
			}

			mTextDoses = sb.toString();

			final NotificationCompat.Builder nb = createPageBuilder(R.string._title_notification_doses, mTextDoses);
			nb.setPriority(NotificationCompat.PRIORITY_HIGH);
			addDoseActions(nb);
			mNtfDoses = nb.build();
		}

		private void buildRefillReminderNotification()
		{
			if(mLowSupplyDrugs.size() == 0)
			{
				mNtfRefill = null;
				mTextRefill = null;
				return;
			}

			final String first = Entries.getDrugName(mLowSupplyDrugs.get(0));

			if(mLowSupplyDrugs.size() == 1)
				mTextRefill = getString(R.string._qmsg_low_supply_single, first);
			else
			{
				final String second = Entries.getDrugName(mLowSupplyDrugs.get(1));
				mTextRefill = RxDroid.getQuantityString(R.plurals._qmsg_low_supply_multiple, mLowSupplyDrugs.size() - 1, first, second);
			}

			final NotificationCompat.Builder nb = createPageBuilder(R.string._title_notification_low_supplies, mTextRefill);
			addRefillReminderActions(nb, true);
			mNtfRefill = nb.build();
		}

		private void buildSummaryNotification()
		{
			if(mTextDoses == null && mTextRefill == null)
			{
				mNtfSummary = null;
				return;
			}

			final int titleResId = mTextDoses != null ? R.string._title_notification_doses : R.string._title_notification_low_supplies;
			final int iconResId = mTextRefill != null ? R.drawable.ic_stat_exclamation : R.drawable.ic_stat_normal;
			final CharSequence contentText = mTextDoses != null ? mTextDoses : mTextRefill;
			final int priority = mTextDoses != null ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT;

			final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
					CHANNEL_DEFAULT);
			builder.setContentIntent(createDrugListIntent(mDate));
			builder.setTicker(getString(R.string._msg_new_notification));
			builder.setCategory(NotificationCompat.CATEGORY_REMINDER);
			builder.setColor(!BuildConfig.DEBUG ? Theme.getColorAttribute(R.attr.colorPrimary) : Color.GREEN);
			builder.setWhen(0);

			builder.setStyle(createSummaryStyle(builder));

			builder.setContentTitle(getString(titleResId));
			builder.setSmallIcon(iconResId);
			builder.setContentText(contentText);
			builder.setPriority(priority);

			if(!mUseGroups)
			{
				builder.extend(new NotificationCompat.WearableExtender()
						.addPages(getPages()));
			}
			else
			{
				builder.setGroup(mGroup);
				builder.setGroupSummary(true);
			}

			if(!addDoseActions(builder))
				addRefillReminderActions(builder, false);

			applyNotificationModalities(builder);

			mNtfSummary = builder.build();

			if(mNoClear && mUseGroups)
				mNtfSummary.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		}

		private boolean addDoseActions(NotificationCompat.Builder builder)
		{
			if(mTextDoses != null && !Settings.getBoolean(Settings.Keys.USE_SAFE_MODE, false))
			{
				final Intent intent = new Intent(mContext, NotificationReceiver.class);
				intent.setAction(ACTION_MARK_ALL_AS_TAKEN);

				PendingIntent operation = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				if(Version.SDK_IS_JELLYBEAN_OR_NEWER)
				{
					addAction(builder, new int[] { R.drawable.ic_action_tick_white, R.drawable.ic_wearableaction_tick },
							R.string._title_take_all_doses, operation);
				}
				else if(Settings.getBoolean(Settings.Keys.SWIPE_TO_TAKE_ALL, false))
				{
					builder.setDeleteIntent(operation);
					mNoClear = false;
				}

				return true;
			}

			return false;
		}

		private void addRefillReminderActions(NotificationCompat.Builder builder, boolean force)
		{
			if(force || (mTextDoses == null && mTextRefill != null))
			{
				// Use simple string like "1 33 56 10231"
				final StringBuilder drugIds = new StringBuilder();
				for(Drug drug : mLowSupplyDrugs)
				{
					drugIds.append(drug.getId());
					drugIds.append(' ');
				}

				final Intent intent = new Intent(mContext, NotificationReceiver.class);
				intent.setAction(ACTION_SNOOZE_REFILL_REMINDER);
				intent.putExtra(EXTRA_REFILL_SNOOZE_DRUGS, drugIds.toString());
				intent.putExtra(EXTRA_DATE, mDate);

				PendingIntent operation = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				if(Version.SDK_IS_JELLYBEAN_OR_NEWER)
				{
					addAction(builder, new int[] { R.drawable.ic_action_snooze_white, R.drawable.ic_wearableaction_snooze },
							R.string._title_remind_tomorrow, operation);
				}
				else
				{
					builder.setDeleteIntent(operation);
					mNoClear = false;
				}
			}
		}

		private void applyNotificationModalities(NotificationCompat.Builder builder)
		{
			final int currentHash = ("" + mTextDoses + mTextRefill).hashCode();
			final int lastHash = Settings.getInt(Settings.Keys.LAST_MSG_HASH);
			final boolean inQuietHours = isNowWithinQuietHours();

			int mode = mMode;

			if(mode == NOTIFICATION_FORCE_UPDATE || currentHash != lastHash)
			{
				builder.setOnlyAlertOnce(false);
				Settings.putInt(Settings.Keys.LAST_MSG_HASH, currentHash);
			}
			else
				builder.setOnlyAlertOnce(true);

			// The rest is handled by notification channels on >= Oreo
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				if(inQuietHours)
					builder.setChannelId(CHANNEL_QUIET);
				return;
			}

			// Prevents low supplies from constantly annoying the user with
			// notification's sound and/or vibration if alarms are repeated.
			if(mTextRefill != null && mTextDoses == null)
				mode = NOTIFICATION_FORCE_SILENT;

			int defaults = 0;

			final String lightColor = Settings.getString(Settings.Keys.NOTIFICATION_LIGHT_COLOR, "");
			if(lightColor.length() == 0)
				defaults |= Notification.DEFAULT_LIGHTS;
			else
			{
				try
				{
					int ledARGB = Integer.parseInt(lightColor, 16);
					if(ledARGB != 0)
					{
						ledARGB |= 0xff000000; // set alpha to ff
						builder.setLights(ledARGB, LED_ON_MS, LED_OFF_MS);
					}
				}
				catch(NumberFormatException e)
				{
					Log.e(TAG, "Failed to parse light color; using default", e);
					defaults |= Notification.DEFAULT_LIGHTS;
				}
			}

			if(mode != NOTIFICATION_FORCE_SILENT)
			{
				if(!inQuietHours)
				{
					final String ringtone = Settings.getString(Settings.Keys.NOTIFICATION_SOUND);
					if(ringtone != null)
					{
						Uri uri = Uri.parse(ringtone);
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
						{
							if("file".equals(uri.getScheme()))
							{
								uri = Util.getExternalFileUri(new File(uri.getPath()));
							}
						}

						builder.setSound(uri);
					}
					else
						defaults |= Notification.DEFAULT_SOUND;

					if(LOGV) Log.i(TAG, "Sound: " + (ringtone != null ? ringtone.toString() : "(default)"));
				}
				else
					Log.i(TAG, "Currently within quiet hours; muting sound...");
			}

			if(mode != NOTIFICATION_FORCE_SILENT && Settings.getBoolean(Settings.Keys.USE_VIBRATOR, true))
				defaults |= Notification.DEFAULT_VIBRATE;

			builder.setDefaults(defaults);
		}

		private List<Notification> getPages()
		{
			final List<Notification> notifications = new ArrayList<Notification>();
			if(mUseWearableHack)
			{
				if(mNtfRefill != null)
					notifications.add(mNtfRefill);
				if(mNtfDoses != null)
					notifications.add(mNtfDoses);
			}

			return notifications;
		}

		private NotificationCompat.Style createSummaryStyle(NotificationCompat.Builder builder)
		{
			if(mTextDoses == null || mTextRefill == null)
				return null;

			final NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(builder);

			if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
			{
				style.setBigContentTitle(getString(R.string.app_name));
				style.addLine(createLine(R.string._title_notification_low_supplies, mTextRefill));
				style.addLine(createLine(R.string._title_notification_doses, mTextDoses));
			}
			else
			{
				style.setBigContentTitle(createLine(R.string._title_notification_doses, mTextDoses));
				style.addLine(createLine(R.string._title_notification_low_supplies, mTextRefill));
			}

			return style;
		}

		private NotificationCompat.Builder createPageBuilder(int titleResId, CharSequence text)
		{
			final BigTextStyle style = new NotificationCompat.BigTextStyle();
			style.setBigContentTitle(getString(titleResId));
			style.setSummaryText(text);

			return new NotificationCompat.Builder(mContext, CHANNEL_DEFAULT)
					.setStyle(style)
					.setContentTitle(getString(titleResId))
					.setContentText(text)
					.setGroup(mGroup)
					.setGroupSummary(false)
					.setContentIntent(createDrugListIntent(mDate))
					.setSmallIcon(R.drawable.ic_stat_normal);
		}

		private void addAction(NotificationCompat.Builder builder, int[] icons, int titleResId, PendingIntent operation)
		{
			builder.addAction(icons[0], getString(titleResId), operation);
			builder.extend(new NotificationCompat.WearableExtender().addAction(
					buildAction(icons[1], titleResId, operation)));
		}

		private NotificationCompat.Action buildAction(int icon, int titleResId, PendingIntent operation) {
			return new NotificationCompat.Action.Builder(icon, getString(titleResId), operation).build();
		}

		private CharSequence createLine(int titleResId, CharSequence text) {
			return Html.fromHtml("<b>" + mContext.getString(titleResId) + "</b> " + text);
		}
	}

	private  int getDrugsWithDueDoses(Date date, int doseTime) {
		return Entries.getDrugsWithDueDoses(mAllDrugs, date, doseTime, null);
	}

	private int getDrugsWithMissedDoses(Date date, int activeOrNextDoseTime, boolean isActiveDoseTime) {
		return Entries.getDrugsWithMissedDoses(mAllDrugs, date, activeOrNextDoseTime, isActiveDoseTime, null);
	}

	private int getDrugsWithLowSupplies(Date date, int doseTime, List<Drug> outDrugs)
	{
		final Date nextRefillReminderDate = Settings.getDate(Settings.Keys.NEXT_REFILL_REMINDER_DATE);
		final Set<Integer> snoozedDrugIds = toIntSet(Settings.getString(REFILL_REMINDER_SNOOZE_DRUGS, ""));

		if(nextRefillReminderDate == null || !mDtInfo.displayDate().before(nextRefillReminderDate) || snoozedDrugIds.isEmpty())
		{
			snoozedDrugIds.clear();
			Settings.putString(REFILL_REMINDER_SNOOZE_DRUGS, null);
			Settings.putDate(Settings.Keys.NEXT_REFILL_REMINDER_DATE, null);
			Log.d(TAG, "Clearing refill reminder snooze info");
		}

		int count = 0;

		for(Drug drug : mAllDrugs)
		{
			final boolean isSnoozed = snoozedDrugIds.contains(drug.getId());

			if(Entries.hasLowSupplies(drug, date))
			{
				if(!isSnoozed)
				{
					++count;
					if(outDrugs != null)
						outDrugs.add(drug);
				}
			}
			else if(isSnoozed)
			{
				Log.d(TAG, "Removing " + drug + " from snoozed drugs");
				snoozedDrugIds.remove(drug.getId());
				Settings.putString(REFILL_REMINDER_SNOOZE_DRUGS, fromIntSet(snoozedDrugIds));
			}
		}

		return count;
	}

	private String getString(int resId, Object... formatArgs) {
		return mContext.getString(resId, formatArgs);
	}

	private NotificationManagerCompat getNotificationManager() {
        return NotificationManagerCompat.from(mContext);
	}

	/* package */ static void cancelAllNotifications()
	{
		final NotificationManagerCompat nm = NotificationManagerCompat.from(RxDroid.getContext());

		for(int id : IDS)
			nm.cancel(id);
	}

	/* package */ static void rescheduleAlarmsAndUpdateNotification(boolean silent) {
		rescheduleAlarmsAndUpdateNotification(null, silent);
	}

	/* package */ static void rescheduleAlarmsAndUpdateNotification(boolean silent, boolean forceUpdate) {
		rescheduleAlarmsAndUpdateNotification(null, silent, forceUpdate);
	}

	/* package */ static void rescheduleAlarmsAndUpdateNotification(Context context, boolean silent) {
		rescheduleAlarmsAndUpdateNotification(context, silent, false);
	}

	/* package */ static void rescheduleAlarmsAndUpdateNotification(Context context, boolean silent, boolean forceUpdate)
	{
		if(context == null)
			context = RxDroid.getContext();
		final Intent intent = new Intent(context, NotificationReceiver.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.putExtra(EXTRA_SILENT, silent);
		intent.putExtra(EXTRA_FORCE_UPDATE, forceUpdate);
		context.sendBroadcast(intent);
	}

	private static Set<Integer> toIntSet(String str)
	{
		final Set<Integer> intSet = new HashSet<Integer>();
		for(String element : str.split(" "))
		{
			if(element.length() != 0)
				intSet.add(Integer.valueOf(element, 10));
		}

		return intSet;
	}

	private static String fromIntSet(Set<Integer> set)
	{
		final StringBuilder sb = new StringBuilder();

		for(Integer i : set)
		{
			sb.append(i);
			sb.append(" ");
		}

		return sb.toString();
	}
}
