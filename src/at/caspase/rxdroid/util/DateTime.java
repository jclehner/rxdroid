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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class DateTime
{
	public static Date today()
	{
		final Timestamp today = new Timestamp(currentTimeMillis());
		today.setHours(0);
		today.setMinutes(0);
		today.setSeconds(0);
		today.setNanos(0);

		return new Date(today.getTime());
	}
	
	public static Date tomorrow() {
		return new Date(today().getTime() + Constants.MILLIS_PER_DAY);		
	}
	
	public static Time now() {
		return new Time(currentTimeMillis());
	}	
	
	public static GregorianCalendar calendarFromDate(Date date) {
		return new GregorianCalendar(date.getYear(), date.getMonth(), date.getDay());
	}

	public static String toString(Time time)
	{
		final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		return sdf.format(time);
	}

	public static long getOffsetFromMidnight(Date today) {
		return now().getTime() - today.getTime();
	}

	public static long currentTimeMillis()
	{
		Calendar now = Calendar.getInstance();
		return now.getTimeInMillis();
	}
}
