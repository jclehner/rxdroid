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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

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

	private static final int[] CALENDAR_TIME_FIELDS = { Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND };

	public static Calendar calendarFromDate(Date date)
	{
		final Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	/**
	 * Returns the current date.
	 *
	 * @return a <code>Calendar</code> set to the current date, its time
	 *     set to 00:00:00
	 * @deprecated Use {@link #today()}
	 */
	@Deprecated
	public static Calendar todayCalendarMutable() {
		return getDatePartMutable(DateTime.nowCalendarMutable());
	}

	public static Date today() {
		return todayCalendarMutable().getTime();
	}

	public static Date yesterday() {
		return DateTime.add(DateTime.today(), Calendar.DAY_OF_MONTH, -1);
	}

	/**
	 * Returns the current time.
	 *
	 * @return the result of <code>Gregorian.getInstance()</code>
	 * @deprecated Use {@link #now()}
	 */
	@Deprecated
	public static Calendar nowCalendarMutable() {
		return GregorianCalendar.getInstance();
	}

	public static Calendar nowCalendar() {
		return ImmutableGregorianCalendar.getInstance();
	}

	public static Date now() {
		return nowCalendarMutable().getTime();
	}

	/**
	 * Sets a <code>Calendar's</code> time to 00:00:00.000.
	 */
	public static Calendar getDatePartMutable(Calendar time)
	{
		final int year = time.get(Calendar.YEAR);
		final int month = time.get(Calendar.MONTH);
		final int day = time.get(Calendar.DAY_OF_MONTH);

		//final Calendar date = (Calendar) time.clone();
		final Calendar date = new GregorianCalendar(year, month, day);
		date.setTimeZone(time.getTimeZone());

		//for(int field: CALENDAR_TIME_FIELDS)
		//	date.set(field, 0);

		return date;
	}

	public static Calendar getDatePart(Calendar time)
	{
		final ImmutableGregorianCalendar date = getImmutableInstance(time);

		for(int field : CALENDAR_TIME_FIELDS)
			date.setInternal(field, 0);

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
		final Calendar cal = GregorianCalendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);

		return cal;
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

	public static String toNativeTime(DumbTime time) {
		return time.toString(DateFormat.is24HourFormat(GlobalContext.get()), false);
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
		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return getOffsetFromMidnight(cal);
	}

	public static boolean isWithinRange(Calendar time, DumbTime begin, DumbTime end)
	{
		final DumbTime theTime = DumbTime.fromCalendar(time);

		if(end.isLessThan(begin))
			return theTime.isLessThan(end) || theTime.compareTo(begin) != -1;

		return theTime.compareTo(begin) != -1 && theTime.isLessThan(end);
	}

	public static boolean isToday(Date date) {
		return today().equals(date);
	}

	public static Date add(Date date, int field, int value)
	{
		Calendar cal = calendarFromDate(date);
		cal.add(field, value);
		return cal.getTime();
	}

	public static int get(Date date, int field) {
		return calendarFromDate(date).get(field);
	}

	public static long diffDays(Date date1, Date date2)
	{
		return (date1.getTime() - date2.getTime()) / Constants.MILLIS_PER_DAY;
	}

	private static ImmutableGregorianCalendar getImmutableInstance(Calendar cal)
	{
		final ImmutableGregorianCalendar r = new ImmutableGregorianCalendar(cal);
		r.setTimeInMillisInternal(cal.getTimeInMillis());
		return r;
	}

	private static final class ImmutableGregorianCalendar extends GregorianCalendar
	{
		private static final long serialVersionUID = -3883494047745731717L;

		private ImmutableGregorianCalendar(Calendar other)
		{
			super(other.getTimeZone(), Locale.getDefault());
			time = other.getTimeInMillis();
			areFieldsSet = true;
			isTimeSet = true;
			complete();
		}

		@Override
		public void add(int field, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(int field, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void roll(int field, boolean increment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTimeInMillis(long milliseconds)
		{
			if(!isTimeSet)
			{
				super.setTimeInMillis(milliseconds);
				return;
			}

			throw new UnsupportedOperationException();
		}

		@Override
		public void setTimeZone(TimeZone timezone)
		{
			if(getTimeZone() == null)
			{
				super.setTimeZone(timezone);
				return;
			}

			throw new UnsupportedOperationException();
		}

		private void setInternal(int field, int value) {
			super.set(field, value);
		}

		private void setTimeZoneInternal(TimeZone timezone) {
			super.setTimeZone(timezone);
		}

		private void setTimeInMillisInternal(long milliseconds) {
			super.setTimeInMillis(milliseconds);
		}
	}
}
