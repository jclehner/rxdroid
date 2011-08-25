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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import at.caspase.rxdroid.Util.Hasher;

/**
 * A time class that is not aware of time zones.
 * 
 * The purpose of this class is to handle the time offsets frequently encountered
 * in RxDroid without having to deal with unexpected timezone issues. Thus, in the
 * context of this application, it could actually be considered smart.
 * 
 * @author Joseph Lehner
 *
 */
public class DumbTime extends Date
{
	private static final long serialVersionUID = -8142558717636198167L;
	
	private static final String[] FORMATS = { "HH:mm:ss", "HH:mm" };
	
	private int mHours;
	private int mMinutes;
	private int mSeconds;
	private int mMillis = 0;
	
	public DumbTime(int hours, int minutes, int seconds) 
	{
		setHours(hours);
		setMinutes(minutes);
		setSeconds(seconds);		
	}
	
	public DumbTime(int hours, int minutes) {
		this(hours, minutes, 0);
	}	
	
	@Override
	public int getHours() {
		return mHours;
	}
	
	@Override
	public int getMinutes() {
		return mMinutes;
	}
	
	@Override
	public int getSeconds() {
		return mSeconds;
	}
		
	@Override
	public long getTime() {
		return mMillis + 1000 * (mHours * 3600 + mMinutes * 60 + mSeconds);
	}
	
	@Override
	public int getTimezoneOffset() {
		return 0;
	}
	

	@Override
	public void setHours(int hours) 
	{
		if(hours < 0 || hours > 23)
			throw new IllegalArgumentException();
		
		mHours = hours;
	}
	
	@Override
	public void setMinutes(int minutes) 
	{
		if(minutes < 0 || minutes > 59)
			throw new IllegalArgumentException();
		
		mMinutes = minutes;
	}
	
	@Override
	public void setSeconds(int seconds) 
	{
		if(seconds < 0 || seconds > 59)
			throw new IllegalArgumentException();
		
		mSeconds = seconds;
	}	
	
	@Override
	public boolean before(Date time) {
		return getTime() < time.getTime();
	}
	
	@Override
	public boolean after(Date time) {
		return getTime() > time.getTime();
	}
	
	@Override
	public int compareTo(Date other)
	{
		if(this.getTime() == other.getTime())
			return 0;
		
		return this.before(other) ? -1 : 1;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof DumbTime))
			return false;
		
		DumbTime other = (DumbTime) o;
		
		if(other == this)
			return true;
		
		return this.getTime() == other.getTime();		
	}
	
	@Override
	public int hashCode()
	{
		int result = Hasher.SEED;
		
		result = Hasher.hash(result, mMillis);
		result = Hasher.hash(result, mSeconds);
		result = Hasher.hash(result, mMinutes);
		result = Hasher.hash(result, mHours);
		
		return result;
	}

	@Override
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean withSeconds)
	{
		final SimpleDateFormat sdf = new SimpleDateFormat(FORMATS[withSeconds ? 0 : 1]);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(this);
	}

	public static DumbTime valueOf(String timeString)
	{
		if(timeString != null)
		{
			for(String format : FORMATS)
			{
				final SimpleDateFormat sdf = new SimpleDateFormat(format);
				try
				{
					final Date date = sdf.parse(timeString);
					return new DumbTime(date.getHours(), date.getMinutes(), date.getSeconds());				
				}
				catch(ParseException e)
				{
					// ignore
				}
			}
		}
		
		throw new IllegalArgumentException("timeString=" + timeString);
	}
		
	/**
	 * Creates an instance with an offset.
	 * 
	 * @param offset An offset from midnight, in milliseconds. The permissible range is thus [0, 86400000).
	 */
	private DumbTime(long offset) 
	{
		if(offset >= 86400000)
			throw new IllegalArgumentException(offset + " is out of range");
				
		mHours = (int) offset % (3600 * 1000);
		offset -= mHours * (3600 * 1000);
		
		mMinutes = (int) offset % (60 * 1000);
		offset -= mMinutes * (60 * 1000);
		
		mSeconds = (int) offset % 1000;
		offset -= mSeconds * 1000;
		
		mMillis = (int) offset;
	}
}
