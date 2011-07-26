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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Util.Constants;

public enum Settings 
{
	INSTANCE;
	
	private static final String TAG = Settings.class.getName();
	private static final long beginTimes[] = { 6, 12, 18, 22 };
	private static final long endTimes[] = { 10, 15, 21, 23 };
	
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
	
	public long getDoseTimeBeginOffset(int doseTime) 
	{
		//return 3600 * 1000 * beginTimes[doseTime];
		return getTime(prefKeyPrefixes[doseTime] + "_begin").getTime();
	}
	
	public long getDoseTimeEndOffset(int doseTime)
	{
		//return 3600 * 1000 * endTimes[doseTime];
		return getTime(prefKeyPrefixes[doseTime] + "_end").getTime();
	}
	
	public int getActiveDoseTime()
	{
		final long offset = Util.DateTime.nowOffsetFromMidnight();
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
	
	public int getNextDoseTime(boolean useNextDay)
	{
		long offset = Util.DateTime.nowOffsetFromMidnight();
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
	
	public int getActiveOrNextDoseTime()
	{
		int ret = getActiveDoseTime();
		if(ret == -1)
			return getNextDoseTime();
		return ret;
	}
	
	public long getSnoozeTime()
	{
		return 10 * 1000;
	}
	
	public DumbTime getTime(String key)
	{
		if(key == null)
			return null;
		
		String value = mSharedPrefs.getString(key, null);
		if(value == null)
		{
			int resId = mApplicationContext.getResources().getIdentifier(
					"at.caspase.rxdroid:string/pref_default_" + key, null, null);
					
			value = mApplicationContext.getString(resId);
		}
		
		Log.d(TAG, "getTime: key=" + key + ", value=" + value);
		
		return DumbTime.valueOf(value);
	}
	
	private Settings()
	{
		Log.d("Settings", "Settings()");
		//mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
	}
}
