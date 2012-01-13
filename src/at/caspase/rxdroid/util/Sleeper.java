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

	public void sleepPart(long millis) throws InterruptedException
	{
		if(millis < 0)
			throw new IllegalArgumentException();

		sleep(getSleepTime(millis));
	}

	public void sleepRemaining() throws InterruptedException
	{
		sleep(mEnd - System.currentTimeMillis());
	}

	public static void sleep(long time) throws InterruptedException
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

	private long getSleepTime(long millis)
	{
		final long now = System.currentTimeMillis();

		if(now < mEnd)
		{
			if(now + millis < mEnd)
				return millis;
			else
				return mEnd - now;
		}

		return 0;
	}
}
