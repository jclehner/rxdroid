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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import android.util.Log;
import android.view.View;

public class Util {
    
	private static final String TAG = Util.class.getName();
	
    private Util() {}
    
    static class Constants
    {
        static final long MILLIS_PER_DAY = 24 * 3600 * 1000;
    }
    
    static class DateTime
    {
    	static Date today() 
    	{
    		final Timestamp today = new Timestamp(currentTimeMillis());
    		today.setHours(0);
    		today.setMinutes(0);
    		today.setSeconds(0);
    		today.setNanos(0);
    		
    		return new Date(today.getTime());
    	}
    	
    	static String toString(Time time)
    	{
    		final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    		
    		return sdf.format(time);	
    	}
    	    	
    	static long getOffsetFromMidnight(Date today) {
    		final Time now = now();
    		    			
    		long ret = now.getTime() - today.getTime();
    		
    		Log.d(TAG, "nowOffsetFromMidnight: now=" + now + "(" + now.getTime() +"), today=" + today + "(" + today.getTime() + ")");
    		Log.d(TAG, "nowOffsetFromMidnight: ret=" + ret);
    		return ret;
    	}
    	
    	static Time now() {
    		return new Time(currentTimeMillis());
    	}
    	    	
    	static long currentTimeMillis()
    	{
    		Calendar now = Calendar.getInstance();
    		return now.getTimeInMillis();
    	}
    }    
	
	/**
	 * Checks whether number lies in range.
	 
	 * @return if <code>num</code> lies in range <code>(begin, end]</code>
	 */
	static boolean inRange(final long num, final long begin, final long end) {
		return num >= begin && num < end;
	}
		
	static View findView(View parent, int id)
	{		
	    View ret = parent.findViewById(id);
	    if(ret == null)
	        throw new NoSuchElementException("No such id in view: " + id);
	    return ret;
	}
	
	static int getDoseTimeDrawableFromDoseViewId(int doseViewId)
    {
    	switch(doseViewId)
    	{
    		case R.id.morning:
    			return R.drawable.ic_morning;
    		case R.id.noon:
    			return R.drawable.ic_noon;
    		case R.id.evening:
    			return R.drawable.ic_evening;
    		case R.id.night:
    			return R.drawable.ic_night;
    	}
    	
    	throw new IllegalArgumentException();
    }
    
    static int getDoseTimeFromDoseViewId(int doseViewId)
    {
    	switch(doseViewId)
    	{
    		case R.id.morning:
    			return Database.Drug.TIME_MORNING;
    		case R.id.noon:
    			return Database.Drug.TIME_NOON;
    		case R.id.evening:
    			return Database.Drug.TIME_EVENING;
    		case R.id.night:
    			return Database.Drug.TIME_NIGHT;
    	}
    	
    	throw new IllegalArgumentException();    	
    }
}
