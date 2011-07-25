package at.caspase.rxdroid;

import java.sql.Time;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;


public class PreferenceTabActivity extends PreferenceActivity implements OnPreferenceChangeListener
{
	private static final String TAG = PreferenceTabActivity.class.getName();
	
	SharedPreferences mSharedPreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mSharedPreferences = getPreferenceManager().getSharedPreferences();
		addPreferencesFromResource(R.xml.prefs_times);		
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		final String key = preference.getKey();
		
		Log.d(TAG, "onPreferenceChange: key=" + key);
		
		if("time_night_end".equals(key))
		{
			final Time begin = getTimeSharedPreference("time_night_begin");
			final Time end = getTimeSharedPreference("time_night_end");
			
			if(end.before(begin))
				preference.setSummary(preference.getSummary() + " (on the next day)");
		}
		
		return false;
	}
	
	private Time getTimeSharedPreference(String key)
	{
		try
		{
			return Time.valueOf(mSharedPreferences.getString(key, null));
		}
		catch(IllegalArgumentException e)
		{
			return new Time(0, 0, 0);
		}
	}	
}
