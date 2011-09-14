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

import java.sql.Time;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class PreferencesActivity extends PreferenceActivity implements OnPreferenceChangeListener
{
	private static final String TAG = PreferencesActivity.class.getName();

	SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mSharedPreferences = getPreferenceManager().getSharedPreferences();
		addPreferencesFromResource(R.xml.preferences);
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
