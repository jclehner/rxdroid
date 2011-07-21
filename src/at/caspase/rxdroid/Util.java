package at.caspase.rxdroid;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;

import android.util.Log;
import android.view.View;

public class Util {
    
	private static final String TAG = Util.class.getName();
	
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
		final Date date = new Date(System.currentTimeMillis());
		return date.getTime();
	}
	
	static long getDayOffsetInMillis()
	{
		long ret = System.currentTimeMillis() - getMidnightMillisFromNow();
		Log.d(TAG, "ret=" + ret);
		return ret;
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
}
