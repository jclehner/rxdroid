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

import java.sql.Time;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import at.caspase.rxdroid.Database.Drug;
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

	@Deprecated
	public long getMillisFromNowUntilDoseTimeBegin(int doseTime) {
		return getMillisFromNowUntilDoseTimeBeginOrEnd(DateTime.now(), doseTime, true);
	}
	
	public long getMillisUntilDoseTimeBegin(Time time, int doseTime) {
		return getMillisFromNowUntilDoseTimeBeginOrEnd(time, doseTime, true);
	}

	@Deprecated
	public long getMillisFromNowUntilDoseTimeEnd(int doseTime) {
		return getMillisFromNowUntilDoseTimeBeginOrEnd(DateTime.now(), doseTime, false);
	}
	
	public long getMillisUntilDoseTimeEnd(Time time, int doseTime) {
		return getMillisFromNowUntilDoseTimeBeginOrEnd(time, doseTime, false);
	}

	public long getSnoozeTime() {
		return getTimePreference("time_snooze").getTime();
	}

	private long getDoseTimeBeginOffset(int doseTime) {
		return getTimePreference(prefKeyPrefixes[doseTime] + "_begin").getTime();
	}

	public long getDoseTimeEndOffset(int doseTime) {
		return getTimePreference(prefKeyPrefixes[doseTime] + "_end").getTime();
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

	public int getActiveOrNextDoseTime() {
		return getActiveDoseTime(DateTime.now());
	}
	
	public int getActiveOrNextDoseTime(Time time)
	{
		int ret = getActiveDoseTime(time);
		if(ret == -1)
			return getNextDoseTime(time);
		return ret;
	}

	public int getActiveDoseTime(Time time)
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

	public int getNextDoseTime(Time time) {
		return getNextDoseTime(time, false);
	}
	
	public int getNextDoseTime(Time time, boolean useNextDayOffsets)
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

	private long getMillisFromNowUntilDoseTimeBeginOrEnd(Time time, int doseTime, boolean getMillisUntilBegin)
	{
		final long beginOffset = getDoseTimeBeginOffset(doseTime);
		final long endOffset = getDoseTimeEndOffset(doseTime);
		final long timeOffset = time.getTime() - DateTime.date(time).getTime();
		long offset = getMillisUntilBegin ? beginOffset : endOffset;
		
		if(timeOffset > offset)
			offset += Constants.MILLIS_PER_DAY;
			
		return offset - timeOffset;
	}
}
