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
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public enum Settings 
{
	INSTANCE;
	
	private static final String TAG = Settings.class.getName();
		
	private static final String prefKeyPrefixes[] = { "time_morning", "time_noon", "time_evening", "time_night" };
	private static final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };
	
	private Context mApplicationContext = null;
	private SharedPreferences mSharedPrefs = null;
		
	public void setApplicationContext(Context context)
	{
		if(mApplicationContext == null)
		{
			mApplicationContext = context.getApplicationContext();
			mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
		}
	}
	
	public Notification blessNotification(Notification notification)
	{
		int flags = notification.flags;
		
		if(mSharedPrefs.getBoolean("use_led", true))
			flags |= Notification.DEFAULT_LIGHTS;
		
		if(mSharedPrefs.getBoolean("use_sound", true))
			flags |= Notification.DEFAULT_SOUND;
		
		if(mSharedPrefs.getBoolean("use_vibrator", true))
			flags |= Notification.DEFAULT_VIBRATE;
		
		notification.flags = flags;
		return notification;		
	}
	
	public long getMillisFromNowUntilDoseTimeBegin(int doseTime) {
		return getMillisFromNowUntilDoseTimeBeginOrEnd(doseTime, true);
	}
	
	public long getMillisFromNowUntilDoseTimeEnd(int doseTime) {
		return getMillisFromNowUntilDoseTimeBeginOrEnd(doseTime, false);
	}
	
	public long getSnoozeTime() {
		return getTimePreference("time_snooze").getTime();
		//return 10 * 1000;
	}	

	public long getDoseTimeBeginOffset(int doseTime) {
		return getTimePreference(prefKeyPrefixes[doseTime] + "_begin").getTime();
	}
	
	public long getDoseTimeEndOffset(int doseTime) {
		return getTimePreference(prefKeyPrefixes[doseTime] + "_end").getTime();
	}
	
	public DumbTime getTimePreference(String key)
	{
		if(key == null)
			return null;
		
		String value = mSharedPrefs.getString(key, null);
		if(value == null)
		{
			int resId = mApplicationContext.getResources().getIdentifier("at.caspase.rxdroid:string/pref_default_" + key, null, null);
			value = mApplicationContext.getString(resId);
		}
		
		Log.d(TAG, "getTimePreference(" + key + ") -> " + value);
				
		return DumbTime.valueOf(value);
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
		final long offset = DateTime.getOffsetFromMidnight(DateTime.today());
		for(int doseTime : doseTimes)
		{
			if(offset >= getDoseTimeBeginOffset(doseTime) && offset < getDoseTimeEndOffset(doseTime))
			{
				Log.d(TAG, "getDoseTime: active=" + doseTime);
				return doseTime;
			}
		}
		
		Log.d(TAG, "getActiveDoseTime: none active");
		
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
			Log.d(TAG, "getNextDoseTime: diff was " + diff + "ms for doseTime=" + doseTime);
		}
		
		if(retDoseTime == -1 && !useNextDay)
		{
			Log.d(TAG, "getNextDoseTime: retrying with next day");
			return getNextDoseTime(true);
		}			
		
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
		
		long diff = beginTime - DateTime.currentTimeMillis();
		
		//final DumbTime ret = new DumbTime(beginTime - Util.DateTime.currentTimeMillis());
		//Log.d(TAG, "Time until " + (getMillisUntilBegin ? "begin" : "end") + " of doseTime " + doseTime + ": " + ret.toString(true));
				
		return diff;
	}
	
	

	
}
