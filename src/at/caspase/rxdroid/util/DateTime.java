package at.caspase.rxdroid.util;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class DateTime
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
	
	public static String toString(Time time)
	{
		final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		return sdf.format(time);	
	}
	    	
	public static long getOffsetFromMidnight(Date today) {
		return now().getTime() - today.getTime();
	}
	
	public static Time now() {
		return new Time(currentTimeMillis());
	}
	    	
	public static long currentTimeMillis()
	{
		Calendar now = Calendar.getInstance();
		return now.getTimeInMillis();
	}
}