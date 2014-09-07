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

package at.jclehner.rxdroid;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.widget.TimePicker;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.Hasher;

/**
 * A crude time class that is not aware of time zones.
 * <p>
 * The purpose of this class is to handle the time offsets frequently encountered
 * in RxDroid without having to deal with unexpected timezone issues. Thus, in the
 * context of this application, it could actually be considered smart.
 * <p>
 * Also note that, even though the interface shows a great similarity to <code>java.util.Date</code>,
 * DumbTime intentionally does not extend this class as it would only cause confusion and enable
 * operations that make no sense, like comparing a DumbTime with a <code>java.util.Date</code>.
 * @author Joseph Lehner
 *
 */
public class DumbTime implements Serializable, Comparable<DumbTime>
{
	private static final long serialVersionUID = -6977398555336283902L;

	@SuppressWarnings("unused")
	private static final String TAG = DumbTime.class.getSimpleName();

	private static final String[] FORMATS = { "HH:mm", "HH:mm:ss", "HH:mm:ss.SSS" };

	private static final int S_MILLIS = 1000;
	private static final int M_MILLIS = 60 * S_MILLIS;
	private static final int H_MILLIS = 60 * M_MILLIS;

	private int mHours;
	private int mMinutes;
	private int mSeconds;
	private int mMillis = 0;

	public DumbTime(int hours, int minutes, int seconds) {
		this(hours * H_MILLIS + minutes * M_MILLIS + seconds * S_MILLIS);
	}

	public DumbTime(int hours, int minutes) {
		this(hours, minutes, 0);
	}

	/**
	 * Creates an instance using an offset from midnight.
	 *
	 * @param offset An offset from midnight, in milliseconds. The permissible range is thus [0, 86400000).
	 */
	public DumbTime(long offset) {
		this(offset, false);
	}

	public DumbTime() {
		this(System.currentTimeMillis() % Constants.MILLIS_PER_DAY);
	}

	/**
	 * Creates an instance using an offset from midnight.
	 *
	 * @param offset An offset from midnight, in milliseconds. The permissible range is thus [0, 86400000), unless <code>
	 *     allowMoreThan24Hours</code> is <code>true</code>.
	 * @param allowMoreThan24Hours See above.
	 */
	public DumbTime(long offset, boolean allowMoreThan24Hours)
	{
		if(offset < 0 || (offset >= Constants.MILLIS_PER_DAY && !allowMoreThan24Hours))
			throw new IllegalArgumentException(offset + " is out of range");

		mHours = (int) (offset / H_MILLIS);
		offset -= (long) mHours * H_MILLIS;

		mMinutes = (int) (offset / M_MILLIS);
		offset -= (long) mMinutes * M_MILLIS;

		mSeconds = (int) (offset / S_MILLIS);
		offset -= mSeconds * S_MILLIS;

		mMillis = (int) offset;
	}

	public int getHours() {
		return mHours;
	}

	public int getMinutes() {
		return mMinutes;
	}

	public int getSeconds() {
		return mSeconds;
	}

	public long getMillisFromMidnight() {
		return mMillis + 1000 * (mHours * 3600 + mMinutes * 60 + mSeconds);
	}

	public boolean isLessThan(DumbTime time) {
		return getMillisFromMidnight() < time.getMillisFromMidnight();
	}

	public boolean isGreaterThan(DumbTime time) {
		return getMillisFromMidnight() > time.getMillisFromMidnight();
	}

	@Override
	public int compareTo(DumbTime other)
	{
		if(this.getMillisFromMidnight() == other.getMillisFromMidnight())
			return 0;
		return this.isLessThan(other) ? -1 : 1;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == null || !(o instanceof DumbTime))
			return false;

		return getMillisFromMidnight() == ((DumbTime) o).getMillisFromMidnight();
	}

	@Override
	public int hashCode()
	{
		final Hasher hasher = Hasher.getInstance();

		hasher.hash(mMillis);
		hasher.hash(mSeconds);
		hasher.hash(mMinutes);
		hasher.hash(mHours);

		return hasher.getHashCode();
	}

	@Override
	public String toString() {
		return toString(true, true);
	}

	public String toString(boolean use24HourTime, boolean withMillis)
	{
		final StringBuilder pattern = new StringBuilder();

		if(use24HourTime)
			pattern.append("HH");
		else
			pattern.append("h");

		pattern.append(":mm");

		if(withMillis)
			pattern.append(":ss.SSS");
		else if(mSeconds != 0)
			pattern.append(":ss");

		if(!use24HourTime)
			pattern.append(" aa");

		final SimpleDateFormat sdf = new SimpleDateFormat(pattern.toString());
		sdf.setTimeZone(TimeZone.getTimeZone(("UTC")));
		return sdf.format(new Date(getMillisFromMidnight()));
	}

	public boolean isWithinRange(DumbTime begin, DumbTime end, boolean allowWrap)
	{
		if(begin != null && end != null)
		{
			if(allowWrap && end.isLessThan(begin))
				return equals(begin) || isGreaterThan(begin) || isLessThan(end);
			else
				return (equals(begin) || isGreaterThan(begin)) && isLessThan(end);
		}
		else if(begin == null)
			return isLessThan(end);
		else if(end == null)
			return equals(begin) || isGreaterThan(begin);

		throw new NullPointerException();
	}

	public static DumbTime now() {
		return fromDate(new Date());
	}

	public static DumbTime fromString(String timeString)
	{
		if(timeString != null)
		{
			for(String format : FORMATS)
			{
				try
				{
					final SimpleDateFormat sdf =
						PerThreadInstance.get(SimpleDateFormat.class, format);

					return fromDate(sdf.parse(timeString));
				}
				catch(ParseException e)
				{
					// ignore
				}
			}
		}

		throw new IllegalArgumentException("timeString=" + timeString);
	}

	@SuppressWarnings("deprecation")
	public static DumbTime fromDate(Date date) {
		return new DumbTime(date.getHours(), date.getMinutes(), date.getSeconds());
	}

	public static DumbTime fromCalendar(Calendar cal) {
		return new DumbTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
	}

	public static DumbTime fromTimePicker(TimePicker picker) {
		return new DumbTime(picker.getCurrentHour(), picker.getCurrentMinute());
	}

	//public static void applyToTimePicker(TimePicker)
}

