package at.caspase.rxdroid;

import android.util.Log;
import at.caspase.rxdroid.Database.Drug;

public enum Settings 
{
	INSTANCE;
	
	private static final String TAG = Settings.class.getName();
	
	private static final long[] beginHours = { 5, 14, 19, 23 };
	private static final long[] endHours = { 11, 17, 23, 24 };
	private static final int doseTimes[] = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };		
	
	public long getDoseTimeBegin(int doseTime)
	{
		// TODO
		return beginHours[doseTime] * 3600 * 1000;		
	}
	
	public long getDoseTimeEnd(int doseTime)
	{
		// TODO
		return endHours[doseTime] * 3600 * 1000;
	}
	
	public int getActiveDoseTime()
	{
		// TODO
		assert beginHours.length == endHours.length;
		
		final long offset = System.currentTimeMillis() - Util.getMidnightMillisFromNow();		
		for(int doseTime : doseTimes)
		{		
			if(offset >= getDoseTimeBegin(doseTime) && offset < getDoseTimeEnd(doseTime))
				return doseTime;			
		}
		
		return -1;		
	}
	
	public int getNextDoseTime()
	{
		final long offset = System.currentTimeMillis() - Util.getMidnightMillisFromNow();
		long smallestDiff = 0;
		int retDoseTime = -1;
		
		for(int doseTime : doseTimes)
		{
			final long diff = getDoseTimeBegin(doseTime) - offset;
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
		return 1 * 60 * 1000;
	}
	
	public static long getDoseTimeOffsetInMillis(int doseType) 
	{
		// TODO implement!
		
		long hours = 0;
		
		switch(doseType)
		{
			case Database.Drug.TIME_MORNING:
				hours = 5;
				break;
			case Database.Drug.TIME_NOON:
				hours = 14;
				break;
			case Database.Drug.TIME_EVENING:
				hours = 21;
				break;
			case Database.Drug.TIME_NIGHT:
				hours = 24;
				break;
		}
		
		return hours * 3600 * 1000;
	}
	
	
	
}
