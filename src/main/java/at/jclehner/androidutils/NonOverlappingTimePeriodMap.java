/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
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

package at.jclehner.androidutils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.jclehner.rxdroid.util.Constants;

public class NonOverlappingTimePeriodMap<T> extends HashMap<NonOverlappingTimePeriodMap.DatePeriod, T>
{
	public static class DatePeriod
	{
		private final Date mBegin, mEnd;

		public DatePeriod(Date begin, Date end)
		{
			mBegin = min(begin, end);
			mEnd = max(begin, end);
		}

		public boolean contains(Date date) {
			return date.before(mEnd) && (date.after(mBegin) || date.equals(mBegin));
		}

		/* package */ boolean contains(long time) {
			return time >= mBegin.getTime() && time < mEnd.getTime();
		}

		public Date begin() {
			return mBegin;
		}

		public Date end() {
			return mEnd;
		}

		public long days() {
			return daysBetween(mBegin, mEnd);
		}

		public static long daysBetween(Date date1, Date date2)
		{
			final Date min = min(date1, date2);
			final Date max = max(date1, date2);
			return (max.getTime() - min.getTime()) / Constants.MILLIS_PER_DAY;
		}

		@Override
		public boolean equals(Object o)
		{
			if(o == null || !(o instanceof DatePeriod))
				return false;

			final DatePeriod other = (DatePeriod) o;
			return other.mBegin.equals(mBegin) && other.mEnd.equals(mEnd);
		}

		@Override
		public int hashCode() {
			return mBegin.hashCode() ^ mEnd.hashCode();
		}

		private static Date min(Date date1, Date date2) {
			return date1.compareTo(date2) < 0 ? date1 : date2;
		}

		private static Date max(Date date1, Date date2) {
			return date1.compareTo(date2) > 0 ? date1 : date2;
		}
	}

	public boolean containsDate(Date date)
	{
		for(DatePeriod period : keySet())
		{
			if(period.contains(date))
				return true;
		}

		return false;
	}

	/* package */ boolean containsTime(long time)
	{
		for(DatePeriod period : keySet())
		{
			if(period.contains(time))
				return true;
		}

		return false;
	}

	public boolean containsPeriod(DatePeriod period)
	{
		final long last = period.mEnd.getTime() - 1;
		return containsTime(period.mBegin.getTime()) || containsTime(last);
	}

	public T get(Date date)
	{
		for(DatePeriod key : keySet())
		{
			if(key.contains(date))
				return get(key);
		}

		return null;
	}

	public List<T> getAllStartingBefore(Date date, boolean dateInclusive) {
		return getAllMatching(date, true, true, dateInclusive);
	}

	public List<T> getAllStartingAfter(Date date, boolean dateInclusive) {
		return getAllMatching(date, true, false, dateInclusive);
	}

	public List<T> getAllEndingBefore(Date date, boolean dateInclusive) {
		return getAllMatching(date, false, true, dateInclusive);
	}

	public List<T> getAllEndingAfter(Date date, boolean dateInclusive) {
		return getAllMatching(date, false, false, dateInclusive);
	}

	@Override
	public T put(DatePeriod key, T value)
	{
		if(containsPeriod(key))
			throw new IllegalArgumentException("Attempting to add overlapping period " + key);

		return super.put(key, value);
	}

	@Override
	public final void putAll(Map<? extends DatePeriod, ? extends T> map)
	{
		for(DatePeriod key : map.keySet())
			put(key, map.get(key));
	}

	private List<T> getAllMatching(Date date, boolean matchBegin, boolean matchBefore, boolean dateInclusive)
	{
		ArrayList<T> list = new ArrayList<T>();

		for(DatePeriod key : keySet())
		{
			final Date matchDate = matchBegin ? key.mBegin : key.mEnd;

			if((matchBefore && matchDate.before(date))
				|| (!matchBefore && matchDate.after(date))
				|| (dateInclusive && matchDate.equals(date)))
			{
				list.add(get(key));
			}
		}

		return list;
	}
}
