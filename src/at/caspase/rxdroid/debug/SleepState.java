package at.caspase.rxdroid.debug;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.util.Log;
import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.util.Constants;

public enum SleepState
{
	INSTANCE;

	private static final String TAG = SleepState.class.getName();
	
	private long mTime = 0;
	private long mMillis = 0;
	
	public void onEnterSleep(long millis)
	{
		Log.d(TAG, "onEnterSleep: millis=" + millis);
		setTime();
		mMillis = millis;
	}
	
	public void onFinishedSleep()
	{
		setTime();
		mMillis = 0;
	}
	
	public boolean isSleeping() {
		return mMillis != 0;
	}
	
	public Calendar getBegin()
	{
		final Calendar begin = GregorianCalendar.getInstance();
		begin.setTimeInMillis(mTime);
		
		return begin;
	}
	
	public Calendar getEnd()
	{
		final Calendar end = getBegin();
		end.add(Calendar.MILLISECOND, (int) mMillis);
		
		return end;
	}
	
	public long getRemainingMillis() 
	{
		if(isSleeping())
			return (mTime + mMillis) - System.currentTimeMillis();
		
		return 0;
	}
	
	public DumbTime getRemainingTime() {
		return new DumbTime(getRemainingMillis() % Constants.MILLIS_PER_DAY);
	}
	
	private void setTime() {
		mTime = System.currentTimeMillis();
	}	
}
