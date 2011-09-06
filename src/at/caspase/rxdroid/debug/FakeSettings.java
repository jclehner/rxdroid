package at.caspase.rxdroid.debug;

import android.util.Log;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Preferences;


/**
 * Fake Settings class for debugging.
 * 
 * @see at.caspase.rxdroid.Preferences
 * @author Joseph Lehner
 *
 */
public class FakeSettings extends Preferences
{
	static final String TAG = FakeSettings.class.getName();
	static int sActiveDoseTime = Drug.TIME_MORNING;
		
	@Override
	public long getMillisFromNowUntilDoseTimeBegin(int doseTime) {
		return 30 * 1000;
	}
	
	@Override
	public long getMillisFromNowUntilDoseTimeEnd(int doseTime) {
		return 30 * 1000;
	}
	
	@Override
	public long getSnoozeTime() {
		return 15 * 1000;
	}
	
	@Override
	public int getActiveDoseTime()
	{
		if(sActiveDoseTime == Drug.TIME_NIGHT)
			sActiveDoseTime = Drug.TIME_MORNING;			
			
		return sActiveDoseTime++;
	}
	
	@Override
	public int getNextDoseTime()
	{
		if(sActiveDoseTime == Drug.TIME_NIGHT)
			return Drug.TIME_MORNING;
		
		return sActiveDoseTime;
	}	
}
