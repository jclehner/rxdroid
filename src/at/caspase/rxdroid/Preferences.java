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

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.debug.FakeSettings;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public class Preferences
{
	private static final String TAG = Preferences.class.getName();

	private static final String prefKeyPrefixes[] = { "time_morning", "time_noon", "time_evening", "time_night" };
	private static final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };

	private static SharedPreferences sSharedPrefs = null;
	private static Context sApplicationContext;
	
	private static Preferences instance;

	public synchronized static Preferences instance()
	{
		if(sApplicationContext == null)
			sApplicationContext = GlobalContext.get();
		
		sSharedPrefs = PreferenceManager.getDefaultSharedPreferences(sApplicationContext);
		
		if(instance == null)
		{
			if(sSharedPrefs.getBoolean("debug_fake_dosetimes", false))
			{
				instance = new FakeSettings();
				Log.d(TAG, "Using FakeSettings");
			}
			else
			{
				instance = new Preferences();
				Log.d(TAG, "Using Settings");
			}
		}

		return instance;
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
	
	/**
	 * Gets the time until the end of that dose time in respect to
	 * the given time.
	 * <p>
	 * As opposed to {@link #getMillisUntilDoseTimeEnd(Calendar, int)}, this
	 * function will return a negative value if the start of the given dose time
	 * lies in the past.
	 */
	public long getMillisUntilDoseTimeEndRaw(Calendar time, int doseTime) {
		return getMillisUntilDoseTimeBeginOrEnd(time, doseTime, FLAG_DONT_CORRECT_TIME);
	}

	public long getSnoozeTime() 
	{
		final long snoozeTime = getTimePreference("time_snooze").getTime();
		Log.d(TAG, "snoozeTime=" + snoozeTime);
		return snoozeTime;
	}

	private long getDoseTimeBeginOffset(int doseTime) {
		return getTimePreference(prefKeyPrefixes[doseTime] + "_begin").getTime();
	}

	public long getDoseTimeEndOffset(int doseTime) {
		return getTimePreference(prefKeyPrefixes[doseTime] + "_end").getTime();
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
	
	public DumbTime getTimePreferenceBegin(int doseTime) {
		return getTimePreference(doseTime, "_begin");
	}
	
	public DumbTime getTimePreferenceEnd(int doseTime) {
		return getTimePreference(doseTime, "_end");
	}
	
	public DumbTime getTimePreference(String key)
	{
		if(key == null)
			return null;

		String value = sSharedPrefs.getString(key, null);
		if(value == null)
		{
			int resId = sApplicationContext.getResources().getIdentifier("at.caspase.rxdroid:string/pref_default_" + key, null, null);
			value = sApplicationContext.getString(resId);
		}

		return DumbTime.valueOf(value);
	}
	
	private DumbTime getTimePreference(int doseTime, String suffix) {
		return getTimePreference(prefKeyPrefixes[doseTime] + suffix);
	}
	
	public Calendar getActiveDate(Calendar time)
	{
		final Calendar date = DateTime.date(time);
		final int activeDoseTime = getActiveDoseTime(time);
		
		if(activeDoseTime == Drug.TIME_NIGHT && hasWrappingDoseTimeNight())
		{
			final DumbTime end = new DumbTime(getDoseTimeEndOffset(Drug.TIME_NIGHT));
			if(DateTime.isWithinRange(time, new DumbTime(0), end))
				date.add(Calendar.DAY_OF_MONTH, -1);
		}
		
		return date;
	}	

	public int getActiveOrNextDoseTime() {
		return getActiveDoseTime(DateTime.now());
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
		for(int doseTime : doseTimes)
		{
			if(DateTime.isWithinRange(time, getTimePreferenceBegin(doseTime), getTimePreferenceEnd(doseTime)))
				return doseTime;
		}

		return -1;
	}
	
	public int getActiveDoseTime() {
		return getActiveDoseTime(DateTime.now());
	}

	public int getNextDoseTime(Calendar time) {
		return getNextDoseTime(time, false);
	}
	
	public int getNextDoseTime(Calendar time, boolean useNextDayOffsets)
	{
		int retDoseTime = -1;
		long smallestDiff = 0;
		
		//Log.d(TAG, "getNextDoseTime: time=" + time);
		
		for(int doseTime : doseTimes)
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
		return getNextDoseTime(DateTime.now());
	}
	
	private static final int FLAG_GET_MILLIS_UNTIL_BEGIN = 1;
	private static final int FLAG_DONT_CORRECT_TIME = 2;

	private long getMillisUntilDoseTimeBeginOrEnd(Calendar time, int doseTime, int flags)
	{
		final long timeOffset = DateTime.getOffsetFromMidnight(time);
		final long doseTimeOffset = (flags & FLAG_GET_MILLIS_UNTIL_BEGIN) != 0 ? 
				getDoseTimeBeginOffset(doseTime) : getDoseTimeEndOffset(doseTime);
		
		long ret = doseTimeOffset - timeOffset;
		
		if(ret < 0 && (flags & FLAG_DONT_CORRECT_TIME) == 0)
			ret += Constants.MILLIS_PER_DAY;
		
		return ret;
	}
}
