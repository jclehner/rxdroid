package at.caspase.rxdroid.util;

import android.util.Log;
import at.caspase.rxdroid.debug.SleepState;

public class Sleeper
{
	private static final String TAG = Sleeper.class.getName();
	
	private long mEnd;
	private boolean mIsSnoozing = false;
	
	public Sleeper(long millis)
	{
		if(millis <= 0)
			throw new IllegalArgumentException();
		
		mEnd = System.currentTimeMillis() + millis;
	}
	
	public synchronized void snooze(long millis) throws InterruptedException
	{
		try
		{
			mIsSnoozing = true;
			wait(getSleepTime(millis));
		}
		finally
		{
			mIsSnoozing = false;
		}
	}
	
	public synchronized void wake()
	{
		if(mIsSnoozing)
			notify();
	}
	
	public void sleep(long millis) throws InterruptedException
	{
		if(millis < 0)
			throw new IllegalArgumentException();
		
		doSleep(getSleepTime(millis));
	}
	
	public void sleep() throws InterruptedException
	{
		doSleep(mEnd - System.currentTimeMillis());
	}
	
	private long getSleepTime(long millis)
	{
		final long now = System.currentTimeMillis();
		
		if(now < mEnd)
		{
			if((now + millis) < mEnd)
				return millis;
			else
				return mEnd - now;
		}
		
		return 0;
	}
	
	private static void doSleep(long time) throws InterruptedException
	{
		if(time > 0)
		{
			Log.d(TAG, "doSleep(" + time + ")");
			SleepState.INSTANCE.onEnterSleep(time);
			try
			{
				Thread.sleep(time);
			}
			finally
			{
				SleepState.INSTANCE.onFinishedSleep();
			}
		}
	}
	
}
