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

import java.util.Date;

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
			sApplicationContext = ContextStorage.get();
		
		if(sApplicationContext == null)
			throw new IllegalStateException("No Context available");

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

	public long getMillisFromNowUntilDoseTimeBegin(int doseTime) {
		return getMillisFromNowUntilDoseTimeBeginOrEnd(doseTime, true);
	}

	public long getMillisFromNowUntilDoseTimeEnd(int doseTime) {
		return getMillisFromNowUntilDoseTimeBeginOrEnd(doseTime, false);
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

	public int getActiveOrNextDoseTime()
	{
		int ret = getActiveDoseTime();
		if(ret == -1)
			return getNextDoseTime();
		return ret;
	}

	public int getActiveDoseTime()
	{
		for(int doseTime : doseTimes)
		{
			if(DateTime.isWithinRange(DateTime.now(), getTimePreferenceBegin(doseTime), getTimePreferenceEnd(doseTime)))
				return doseTime;
		}

		return -1;
	}

	public int getNextDoseTime() {
		return getNextDoseTime(false);
	}

	private int getNextDoseTime(boolean useNextDay)
	{
		long offset = DateTime.getOffsetFromMidnight(DateTime.today());
		if(useNextDay)
			offset -= Constants.MILLIS_PER_DAY;

		int retDoseTime = -1;
		long smallestDiff = 0;

		for(int doseTime : doseTimes)
		{
			final long diff = getDoseTimeBeginOffset(doseTime) - offset;
			if(diff >= 0)
			{
				if(retDoseTime == -1 || diff < smallestDiff)
				{
					smallestDiff = diff;
					retDoseTime = doseTime;
				}
			}
		}

		if(retDoseTime == -1 && !useNextDay)
			return getNextDoseTime(true);

		return retDoseTime;
	}

	private long getMillisFromNowUntilDoseTimeBeginOrEnd(int doseTime, boolean getMillisUntilBegin)
	{
		final long offset = getMillisUntilBegin ? getDoseTimeBeginOffset(doseTime) : getDoseTimeEndOffset(doseTime);
		final Date today = DateTime.today();

		long beginTime = today.getTime() + offset;

		if(beginTime < DateTime.currentTimeMillis())
		{
			final Date tomorrow = new Date(today.getTime() + Constants.MILLIS_PER_DAY);
			beginTime = tomorrow.getTime() + offset;

			Log.d(TAG, "Getting offset from tomorrow");
		}

		return beginTime - DateTime.currentTimeMillis();
	}
}
