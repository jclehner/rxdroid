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

package at.caspase.rxdroid.debug;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.util.Log;
import at.caspase.rxdroid.DumbTime;

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
		{
			long remaining = (mTime + mMillis) - System.currentTimeMillis();
			return remaining >= 0 ? remaining : 0;			
		}
				
		return 0;
	}
	
	public DumbTime getRemainingTime() {
		return new DumbTime(getRemainingMillis(), true);
	}
	
	private void setTime() {
		mTime = System.currentTimeMillis();
	}	
}
