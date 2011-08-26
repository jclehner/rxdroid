package at.caspase.rxdroid.util;

import at.caspase.rxdroid.Database;
import at.caspase.rxdroid.R;

public class Util
{
	public static int getDoseTimeDrawableFromDoseViewId(int doseViewId)
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
    
    public static int getDoseTimeFromDoseViewId(int doseViewId)
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
