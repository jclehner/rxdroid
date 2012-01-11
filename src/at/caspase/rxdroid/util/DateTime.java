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
import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.GlobalContext;

/**
 * Date/time utilities.
 *
 * @author Joseph Lehner
 */
public final class DateTime
{
	@SuppressWarnings("unused")
	private static final String TAG = DateTime.class.getName();

	public static Calendar calendarFromDate(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;		
	}	
	
	/**
	 * Returns the current date.
	 *
	 * @return a <code>Calendar</code> set to the current date, its time
	 *     set to 00:00:00
	 * @deprecated Use {@link #todayDate()}
	 */
	public static Calendar today() {
		return getDatePart(DateTime.now());
	}
	
	public static Date todayDate() {
		return today().getTime();
	}

	/**
	 * Returns the current time.
	 *
	 * @return the result of <code>Gregorian.getInstance()</code>
	 * @deprecated Use {@link #nowDate()}
	 */
	public static Calendar now() {
		return GregorianCalendar.getInstance();
	}
	
	public static Date nowDate() {
		return now().getTime();
	}

	/**
	 * Sets a <code>Calendar's</code> time to 00:00:00.000.
	 */
	public static Calendar getDatePart(Calendar time)
	{
		final Calendar date = (Calendar) time.clone();
		final int calFields[] = { Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND };

		for(int calField: calFields)
			date.set(calField, 0);

		return date;
	}

	/**
	 * Returns a <code>Calendar</code> with the specified date.
	 *
	 * @param year The year
	 * @param month The month (January is 0, December is 11!)
	 * @param day The date
	 */
	public static Date date(int year, int month, int day) {
		return calendar(year, month, day).getTime();
	}
	
	public static Calendar calendar(int year, int month, int day)
	{
		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);
		
		return cal;
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
		//Log.d(TAG, "isWithinRange(" + toString(time) + ", " + begin + ", " + end + ") = " + ret);
		return ret;
	}

	public static boolean isWithinRange_(Calendar time, DumbTime begin, DumbTime end)
	{
		final DumbTime theTime = DumbTime.fromCalendar(time);

		if(end.before(begin))
			return theTime.before(end) || theTime.compareTo(begin) != -1;

		return theTime.compareTo(begin) != -1 && theTime.before(end);
	}
	
	public static Date add(Date date, int field, int value)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(field, value);
		return cal.getTime();	
	}
}
