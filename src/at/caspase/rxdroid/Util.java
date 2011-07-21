package at.caspase.rxdroid;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import android.util.Log;
import android.view.View;

public class Util {
    
    private Util() {}
    
    static public class Constants
    {
        static final int MILLIS_PER_DAY = 24 * 3600 * 1000;
    }
	
	/**
	 * Checks whether number lies in range.
	 
	 * @return if <code>num</code> lies in range <code>(begin, end]</code>
	 */
	static boolean inRange(final long num, final long begin, final long end) {
		return num >= begin && num < end;
	}
	
	static long getMidnightMillisFromNow()
	{
		final Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
				
		final long now = calendar.getTimeInMillis();
		return now - (now % Constants.MILLIS_PER_DAY);
	}
	
	static long getDayOffsetInMillis()
	{
		return System.currentTimeMillis() - getMidnightMillisFromNow();
	}
	
	static String getDateString(final long time) 
	{
	    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	    return sdf.format(new Date(time));
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
    
    static String visibilityToString(int visibility)
    {
    	switch(visibility)
    	{
    		case View.VISIBLE:
    			return "VISIBLE";
    			
    		case View.INVISIBLE:
    			return "INVISIBLE";
    			
    		case View.GONE:
    			return "GONE";
    	}
    	
    	throw new IllegalArgumentException();
    }
}
