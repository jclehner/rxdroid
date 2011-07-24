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

import android.util.Log;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Util.Constants;

public enum Settings 
{
	INSTANCE;
	
	private static final String TAG = Settings.class.getName();
	
	private static final long[] beginHours = { 5, 12, 18, 23 };
	private static final long[] endHours = { 11, 16, 21, 24 };
	private static final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };
	
	public long getDoseTimeBeginOffset(int doseTime)
	{
		// TODO
		if(doseTime == Drug.TIME_NOON)
			return 1000 * (13 * 3600 + 38 * 60);
		
		return beginHours[doseTime] * 3600 * 1000;		
	}
	
	public long getDoseTimeEndOffset(int doseTime)
	{
		// TODO
		return endHours[doseTime] * 3600 * 1000;
	}
	
	public int getActiveDoseTime()
	{
		// TODO
		assert beginHours.length == endHours.length;
		
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
}
