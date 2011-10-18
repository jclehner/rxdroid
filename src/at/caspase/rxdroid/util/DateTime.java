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

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.text.format.DateFormat;
import android.util.Log;
import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.GlobalContext;

public final class DateTime
{
	private static final String TAG = DateTime.class.getName();
	
	public static Calendar today()
	{
		final Calendar today = now();
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		return today;
	}

	public static Calendar tomorrow() 
	{
		final Calendar day = today();
		day.add(Calendar.HOUR, 24);
		return day;
	}

	public static Calendar now() 
	{
		final Calendar now = GregorianCalendar.getInstance();
		return now;
	}
	
	public static Calendar date(Calendar time)
	{
		final Calendar date = (Calendar) time.clone();
		final int calFields[] = { Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND };
		
		for(int calField: calFields)
			date.set(calField, 0);
		
		return date;		
	}
	
	public static Calendar date(int year, int month, int day)
	{
		final Calendar date = new GregorianCalendar();
		date.set(Calendar.HOUR_OF_DAY, 0);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
		
		date.set(Calendar.YEAR, year);
		date.set(Calendar.MONTH, month);
		date.set(Calendar.DAY_OF_MONTH, day);
				
		return date;
	}
	
	public static java.sql.Date toSqlDate(Calendar cal)
	{
		final int year = cal.get(Calendar.YEAR);
		final int month = cal.get(Calendar.MONTH);
		final int day = cal.get(Calendar.DAY_OF_MONTH);		
		
		return new java.sql.Date(year - 1900, month, day);	
	}
	
	public static Time toSqlTime(Calendar cal)
	{
		final Time time = new java.sql.Time(cal.getTimeInMillis());
		return time;
	}

	public static String toString(Calendar calendar)
	{
		if(calendar == null)
			return "null";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
		return sdf.format(calendar.getTime());
	}
	
	public static String toNativeDate(Date date) {
		return DateFormat.getDateFormat(GlobalContext.get()).format(date);
	}

	public static long getOffsetFromMidnight(Calendar date)
	{
		final int hour = date.get(Calendar.HOUR_OF_DAY);
		final int minute = date.get(Calendar.MINUTE);
		final int second = date.get(Calendar.SECOND);
		final int millis = date.get(Calendar.MILLISECOND);
		
		return millis + 1000 * (hour * 3600 + minute * 60 + second);		
	}
	
	public static long getOffsetFromMidnight(Date date)
	{
		Calendar theDate = GregorianCalendar.getInstance();
		theDate.setTime(date);
		return getOffsetFromMidnight(theDate);
	}

	public static boolean isWithinRange(Calendar time, DumbTime begin, DumbTime end)
	{
		boolean ret = isWithinRange_(time, begin, end);
		Log.d(TAG, "isWithinRange(" + toString(time) + ", " + begin + ", " + end + ") = " + ret);
		return ret;
	}
	
	public static boolean isWithinRange_(Calendar time, DumbTime begin, DumbTime end)
	{
		final DumbTime theTime = DumbTime.fromCalendar(time);
						
		if(end.before(begin))
			return theTime.before(end) || theTime.compareTo(begin) != -1;
				
		return theTime.compareTo(begin) != -1 && theTime.before(end);
	}
}
