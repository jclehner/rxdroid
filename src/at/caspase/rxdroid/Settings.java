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

import java.util.Calendar;
import java.util.Date;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.preferences.TimePeriodPreference.TimePeriod;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public class Settings
{
	private static final String TAG = Settings.class.getName();
	private static final boolean LOGV = true;

	private static final String KEY_LAST_MSG_HASH = "_last_msg_hash";
	private static final String KEY_LAST_MSG_COUNT = "_last_msg_count";

	private static final String KEYS[] = { "time_morning", "time_noon", "time_evening", "time_night" };
	private static final int DOSE_TIMES[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };

	private static SharedPreferences sSharedPrefs = null;
	private static Context sApplicationContext;

	private static Settings sInstance;

	public synchronized static Settings instance()
	{
		if(sApplicationContext == null)
			sApplicationContext = GlobalContext.get();

		sSharedPrefs = PreferenceManager.getDefaultSharedPreferences(sApplicationContext);

		if(sInstance == null)
			sInstance = new Settings();

		return sInstance;
	}

	public int filterNotificationDefaults(int defaults)
	{
		if(!sSharedPrefs.getBoolean("use_led", true))
			defaults ^= Notification.DEFAULT_LIGHTS;

		if(!sSharedPrefs.getBoolean("use_sound", true))
			defaults ^= Notification.DEFAULT_SOUND;

		if(!sSharedPrefs.getBoolean("use_vibrator", true))
			defaults ^= Notification.DEFAULT_VIBRATE;

		return defaults;
	}

	public long getMillisUntilDoseTimeBegin(Calendar time, int doseTime) {
		return getMillisUntilDoseTimeBeginOrEnd(time, doseTime, FLAG_GET_MILLIS_UNTIL_BEGIN);
	}

	public long getMillisUntilDoseTimeEnd(Calendar time, int doseTime) {
		return getMillisUntilDoseTimeBeginOrEnd(time, doseTime, 0);
	}

	public long getAlarmTimeout()
	{
		// FIXME

		if(!sSharedPrefs.getBoolean("debug_snooze_time_short", false))
			return 1800 * 1000;

		return 10000;
	}

	private long getDoseTimeBeginOffset(int doseTime) {
		return getDoseTimeBegin(doseTime).getTime();
	}

	public long getDoseTimeEndOffset(int doseTime) {
		return getDoseTimeEnd(doseTime).getTime();
	}

	public long getTrueDoseTimeEndOffset(int doseTime)
	{
		final long doseTimeBeginOffset = getDoseTimeBeginOffset(doseTime);
		long doseTimeEndOffset = getDoseTimeEndOffset(doseTime);

		if(doseTimeEndOffset < doseTimeBeginOffset)
			doseTimeEndOffset += Constants.MILLIS_PER_DAY;

		return doseTimeEndOffset;
	}

	public boolean hasWrappingDoseTimeNight() {
		return getDoseTimeEndOffset(Drug.TIME_NIGHT) != getTrueDoseTimeEndOffset(Drug.TIME_NIGHT);
	}

	public DumbTime getDoseTimeBegin(int doseTime)
	{
		final TimePeriod p = getTimePeriodPreference(doseTime);
		return p == null ? null : p.getBegin();
	}

	public DumbTime getDoseTimeEnd(int doseTime)
	{
		final TimePeriod p = getTimePeriodPreference(doseTime);
		return p == null ? null : p.getEnd();
	}

	/*public DumbTime getTimePreference(String key)
	{
		if(key == null)
			return null;

		String value = sSharedPrefs.getString(key, null);
		if(value == null)
		{
			int resId = sApplicationContext.getResources().getIdentifier("at.caspase.rxdroid:string/pref_default_" + key, null, null);
			if(resId != 0)
				value = sApplicationContext.getString(resId);
			else
				value = "00:30";
		}

		return DumbTime.fromString(value);
	}*/

	public TimePeriod getTimePeriodPreference(int doseTime)
	{
		final String key = KEYS[doseTime];

		String value = sSharedPrefs.getString(key, null);
		if(value == null)
		{
			int resId = sApplicationContext.getResources().
				getIdentifier("at.caspase.rxdroid:string/pref_default_" + key, null, null);

			if(resId == 0 || (value = sApplicationContext.getString(resId)) == null)
				throw new IllegalStateException("No default value for time preference " + key + " in strings.xml");
		}

		return TimePeriod.fromString(value);
	}

	public Date getActiveDate(Calendar time)
	{
		final Calendar cal = DateTime.getDatePart(time);
		final int activeDoseTime = getActiveDoseTime(time);

		if(activeDoseTime == Drug.TIME_NIGHT && hasWrappingDoseTimeNight())
		{
			final DumbTime end = new DumbTime(getDoseTimeEndOffset(Drug.TIME_NIGHT));
			if(DateTime.isWithinRange(time, new DumbTime(0), end))
				cal.add(Calendar.DAY_OF_MONTH, -1);
		}

		return cal.getTime();
	}

	public Date getActiveDate() {
		return getActiveDate(DateTime.nowCalendar());
	}

	public int getActiveOrNextDoseTime() {
		return getActiveDoseTime(DateTime.nowCalendar());
	}

	public int getActiveOrNextDoseTime(Calendar time)
	{
		int ret = getActiveDoseTime(time);
		if(ret == -1)
			return getNextDoseTime(time);
		return ret;
	}

	public int getActiveDoseTime(Calendar time)
	{
		for(int doseTime : DOSE_TIMES)
		{
			if(DateTime.isWithinRange(time, getDoseTimeBegin(doseTime), getDoseTimeEnd(doseTime)))
				return doseTime;
		}

		return -1;
	}

	public int getActiveDoseTime() {
		return getActiveDoseTime(DateTime.nowCalendar());
	}

	public int getNextDoseTime(Calendar time) {
		return getNextDoseTime(time, false);
	}

	public int getNextDoseTime(Calendar time, boolean useNextDayOffsets)
	{
		int retDoseTime = -1;
		long smallestDiff = 0;

		//Log.d(TAG, "getNextDoseTime: time=" + time);

		for(int doseTime : DOSE_TIMES)
		{
			long diff = getMillisUntilDoseTimeBegin(time, doseTime);
			if(useNextDayOffsets)
				diff += Constants.MILLIS_PER_DAY;

			//Log.d(TAG, "  diff " + diff + " for doseTime=" + doseTime);

			if(diff > 0 && (smallestDiff == 0 || diff < smallestDiff))
			{
				smallestDiff = diff;
				retDoseTime = doseTime;
			}
		}

		if(retDoseTime == -1)
		{
			if(!useNextDayOffsets)
			{
				//Log.d(TAG, "  retrying with offsets from next day");
				return getNextDoseTime(time, true);
			}

			throw new RuntimeException("retDoseTime == -1");
		}

		//Log.d(TAG, "  retDoseTime=" + retDoseTime);
		return retDoseTime;
	}

	public int getNextDoseTime() {
		return getNextDoseTime(DateTime.nowCalendar());
	}


	public int getLastNotificationMessageHash() {
		return sSharedPrefs.getInt(KEY_LAST_MSG_HASH, 0);
	}

	public void setLastNotificationMessageHash(int messageHash)
	{
		Editor editor = sSharedPrefs.edit();
		editor.putInt(KEY_LAST_MSG_HASH, messageHash);
		editor.commit();
	}

	public int getLastNotificationCount() {
		return sSharedPrefs.getInt(KEY_LAST_MSG_COUNT, 0);
	}

	public void setLastNotificationCount(int notificationCount)
	{
		if(notificationCount < 0 || notificationCount > 3)
			throw new IllegalArgumentException();

		Editor editor = sSharedPrefs.edit();
		editor.putInt(KEY_LAST_MSG_COUNT, notificationCount);
		editor.commit();
	}

	public int getListPreferenceValueIndex(String key, int defValue)
	{
		String valueStr = sSharedPrefs.getString(key, null);
		return valueStr != null ? Integer.parseInt(valueStr, 10) : defValue;
	}

	public int getNotificationDefaultsXorMask()
	{
		int mask = 0;

		if(!sSharedPrefs.getBoolean("use_led", true))
			mask |= Notification.DEFAULT_LIGHTS;

		if(!sSharedPrefs.getBoolean("use_sound", true))
			mask |= Notification.DEFAULT_SOUND;

		if(!sSharedPrefs.getBoolean("use_vibrator", true))
			mask |= Notification.DEFAULT_VIBRATE;

		return mask;
	}

	public String getDrugName(Drug drug)
	{
		// TODO implement scrambling

		final String name = drug.getName();
		// this should never happen unless there's a DB problem
		if(name == null || name.length() == 0)
			return "<???>";

		return name;
	}

	private static final int FLAG_GET_MILLIS_UNTIL_BEGIN = 1;
	private static final int FLAG_DONT_CORRECT_TIME = 2;

	private long getMillisUntilDoseTimeBeginOrEnd(Calendar time, int doseTime, int flags)
	{
		final long doseTimeOffsetMillis = (flags & FLAG_GET_MILLIS_UNTIL_BEGIN) != 0 ?
				getDoseTimeBeginOffset(doseTime) : getDoseTimeEndOffset(doseTime);

		final DumbTime doseTimeOffset = new DumbTime(doseTimeOffsetMillis);
		final Calendar target = DateTime.getDatePart(time);

		// simply adding the millisecond offset is tempting, but leads to errors
		// when the DST begins/ends in this interval

		target.set(Calendar.HOUR_OF_DAY, doseTimeOffset.getHours());
		target.set(Calendar.MINUTE, doseTimeOffset.getMinutes());
		target.set(Calendar.SECOND, doseTimeOffset.getSeconds());

		if(target.getTimeInMillis() < time.getTimeInMillis() && (flags & FLAG_DONT_CORRECT_TIME) == 0)
			target.add(Calendar.DAY_OF_MONTH, 1);

		return target.getTimeInMillis() - time.getTimeInMillis();
	}
}
