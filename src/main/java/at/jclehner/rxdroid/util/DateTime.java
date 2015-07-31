/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

package at.jclehner.rxdroid.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import android.text.format.DateFormat;
import android.util.Log;

import org.joda.time.LocalDate;

import at.jclehner.rxdroid.DumbTime;
import at.jclehner.rxdroid.PerThreadInstance;
import at.jclehner.rxdroid.RxDroid;

/**
 * Date/time utilities.
 *
 * @author Joseph Lehner
 */
public final class DateTime
{
	private static final String TAG = DateTime.class.getSimpleName();
	private static final boolean LOGV = false;

	private static final int[] CALENDAR_TIME_FIELDS = { Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND };

	private static final String DATE_AND_TIME_FORMAT = "yyyy-MM-dd, HH:mm:ss";
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static final HashMap<Long, DateCacheData> DATE_CACHE = new HashMap<Long, DateCacheData>();
	private static boolean sDateCacheEnabled = true;

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

	public static Date today()
	{
		final long now = System.currentTimeMillis();
		final long today = now - (now % Constants.MILLIS_PER_DAY);

		return obtainImmutableCachedDateInstance(today).date;
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
		return new ImmutableGregorianCalendar(System.currentTimeMillis());
	}

	public static Date now() {
		return nowCalendar().getTime();
	}

	public static Date min(Date date1, Date date2)
	{
		if(date1 != null && date2 != null)
			return date1.before(date2) ? date1 : date2;
		else if(date1 != null)
			return date1;
		else if(date2 != null)
			return date2;

		return null;
	}

	public static Date max(Date date1, Date date2)
	{
		if(date1 != null && date2 != null)
			return date1.after(date2) ? date1 : date2;
		else if(date1 != null)
			return date1;
		else if(date2 != null)
			return date2;

		return null;
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

	@SuppressWarnings("deprecation")
	public static boolean equalsDate(Date date1, Date date2)
	{
		if(date1 == null || date2 == null)
			return date1 == date2;

		if(date1.getDate() != date2.getDate())
			return false;

		if(date1.getMonth() != date2.getMonth())
			return false;

		if(date1.getYear() != date2.getYear())
			return false;

		return true;
	}

	public static String toString(Calendar calendar)
	{
		if(calendar == null)
			return "null";

		SimpleDateFormat sdf = PerThreadInstance.get(SimpleDateFormat.class, DATE_AND_TIME_FORMAT);
		return sdf.format(calendar.getTime());
	}

	public static String toString(long timeInMillis)
	{
		SimpleDateFormat sdf = PerThreadInstance.get(SimpleDateFormat.class, DATE_AND_TIME_FORMAT);
		return sdf.format(new Date(timeInMillis));
	}

	public static String toDateString(Date date)
	{
		if(date == null)
			return null;

		SimpleDateFormat sdf = PerThreadInstance.get(SimpleDateFormat.class, DATE_FORMAT);
		return sdf.format(date);
	}

	public static LocalDate fromDateFields(Date date) {
		return date != null ? LocalDate.fromDateFields(date) : null;
	}

	public static String toNativeDate(LocalDate date) {
		return toNativeDate(date.toDate());
	}

	public static String toNativeDate(Date date) {
		return DateFormat.getDateFormat(RxDroid.getContext()).format(date);
	}

	public static String toNativeDateAndTime(Date date)
	{
		return DateFormat.getDateFormat(RxDroid.getContext()).format(date) + ", " +
			DateFormat.getTimeFormat(RxDroid.getContext()).format(date);
	}

	public static String toNativeTime(DumbTime time) {
		return time.toString(DateFormat.is24HourFormat(RxDroid.getContext()), false);
	}

	public static String toNativeTime(Date time, boolean withSeconds)
	{
		final boolean use24HourTime = DateFormat.is24HourFormat(RxDroid.getContext());
		final StringBuilder pattern = new StringBuilder();

		if(use24HourTime)
			pattern.append("HH");
		else
			pattern.append("h");

		pattern.append(":mm");

		if(withSeconds)
			pattern.append(":ss");

		if(!use24HourTime)
			pattern.append(" aa");

		final SimpleDateFormat sdf = new SimpleDateFormat(pattern.toString());
		//sdf.setTimeZone(TimeZone.getTimeZone(("UTC")));
		return sdf.format(time);
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

	public static Date getMidnightDate(Date date) {
		return new Date(date.getTime() - getOffsetFromMidnight(date));
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
		if(field == Calendar.DAY_OF_MONTH)
			return new LocalDate(date).plusDays(value).toDate();

		Calendar cal = calendarFromDate(date);
		cal.add(field, value);
		return cal.getTime();
	}

	public static int get(Date date, int field) {
		return calendarFromDate(date).get(field);
	}

	public static Calendar copy(Calendar cal)
	{
		final GregorianCalendar copy = new GregorianCalendar(cal.getTimeZone());
		copy.setTimeInMillis(cal.getTimeInMillis());
		return copy;
	}

	public static long diffDays(Date date1, Date date2) {
		return Math.round((date2.getTime() - date1.getTime()) / (double) Constants.MILLIS_PER_DAY);
	}

	public static int getIsoWeekDayNumberIndex(Date date)
	{
		final Calendar cal = calendarFromDate(date);
		return CollectionUtils.indexOf(cal.get(Calendar.DAY_OF_WEEK), Constants.WEEK_DAYS);
	}

	public static void disableDateCache()
	{
		sDateCacheEnabled = false;
		clearDateCache();
	}

	public static void clearDateCache() {
		DATE_CACHE.clear();
	}

	private static ImmutableGregorianCalendar getImmutableInstance(Calendar cal)
	{
		final ImmutableGregorianCalendar r = new ImmutableGregorianCalendar(cal);
		r.setTimeInMillisInternal(cal.getTimeInMillis());
		return r;
	}

	private static DateCacheData obtainImmutableCachedDateInstance(long timeInMillis)
	{
		synchronized(DATE_CACHE)
		{
			//ImmutableGregorianCalendar instance = DATE_CACHE.get(timeInMillis);
			DateCacheData data = sDateCacheEnabled ? DATE_CACHE.get(timeInMillis) : null;

			if(data == null)
			{
				data = new DateCacheData();
				data.calendar = new ImmutableGregorianCalendar(timeInMillis);

				for(int field : CALENDAR_TIME_FIELDS)
					data.calendar.setInternal(field, 0);

				data.date = new ImmutableDate(data.calendar.getTimeInMillis());

				if(sDateCacheEnabled)
					DATE_CACHE.put(timeInMillis, data);
			}
			else if(LOGV)
			{
				if((++ImmutableGregorianCalendar.sCacheHits % 10) == 0)
					Log.v(TAG, ImmutableGregorianCalendar.sCacheHits + " cache hits while obtaining current date");
			}

			return data;
		}
	}

	private static final class ImmutableGregorianCalendar extends GregorianCalendar
	{
		private static final long serialVersionUID = -3883494047745731717L;

//		private static final SparseArray<ImmutableGregorianCalendar> DATE_CACHE =
//			new SparseArray<DateTime.ImmutableGregorianCalendar>();


		private static int sCacheHits = 0;

		private ImmutableGregorianCalendar(Calendar other)
		{
			super(other.getTimeZone(), Locale.getDefault());
			init(other.getTimeInMillis());
		}

		private ImmutableGregorianCalendar(long timeInMillis)
		{
			super(TimeZone.getDefault(), Locale.getDefault());
			init(timeInMillis);
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

		@SuppressWarnings("unused")
		private void setTimeZoneInternal(TimeZone timezone) {
			super.setTimeZone(timezone);
		}

		private void setTimeInMillisInternal(long milliseconds) {
			super.setTimeInMillis(milliseconds);
		}

		private void init(long timeInMillis)
		{
			time = timeInMillis;
			areFieldsSet = true;
			isTimeSet = true;
			complete();
		}
	}

	private static final class ImmutableDate extends Date
	{
		private static final long serialVersionUID = -660796950979760891L;
		private boolean mIsTimeSet = false;

		@SuppressWarnings("unused")
		public ImmutableDate(Date date) {
			setTime(date.getTime());
		}

		public ImmutableDate(long timeInMillis) {
			setTime(timeInMillis);
		}

		@Override
		public void setTime(long milliseconds)
		{
			if(!mIsTimeSet)
			{
				super.setTime(milliseconds);
				mIsTimeSet = true;
			}
			else
				throw new UnsupportedOperationException();
		}

		@Override
		public void setSeconds(int second) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setMinutes(int minute) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setHours(int hour) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setDate(int date) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setMonth(int month) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setYear(int year) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class DateCacheData
	{
		ImmutableGregorianCalendar calendar;
		Date date;
	}
}
