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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import at.caspase.rxdroid.db.DatabaseHelper;
import at.caspase.rxdroid.util.Util;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	@SuppressWarnings("unused")
	private static final String TAG = PreferencesActivity.class.getName();

	private static final int MENU_RESTORE_DEFAULTS = 0;

	private static final String PREF_LOW_SUPPLY_THRESHOLD = "num_min_supply_days";
	
	SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mSharedPreferences = getPreferenceManager().getSharedPreferences();
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
		addPreferencesFromResource(R.xml.preferences);

		final Preference versionPref = findPreference("version");
		if(versionPref != null)
		{
			String summary = Version.get(Version.FORMAT_FULL) + ", DB v" + DatabaseHelper.DB_VERSION;			
			versionPref.setSummary(summary);
		}
		
		updateLowSupplyThresholdPreferenceSummary();
		Util.populateListPreferenceEntryValues(findPreference("snooze_type"));		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_RESTORE_DEFAULTS, 0, "Restore defaults").setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case MENU_RESTORE_DEFAULTS:
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setTitle("This will reset all settings to their default values.");
				builder.setNegativeButton(android.R.string.cancel, null);
				/////////////////////
				builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						mSharedPreferences.edit().clear().commit();
					}
				});
				/////////////////////
				builder.show();
				break;
			}
			default:
				// ignore
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if(PREF_LOW_SUPPLY_THRESHOLD.equals(key))
			updateLowSupplyThresholdPreferenceSummary();
	}
	
	private void updateLowSupplyThresholdPreferenceSummary()
	{
		Preference p = findPreference(PREF_LOW_SUPPLY_THRESHOLD);
		if(p != null)
		{
			String value = mSharedPreferences.getString(PREF_LOW_SUPPLY_THRESHOLD, "10");
			p.setSummary(getString(R.string._summary_min_supply_days, value));
		}
	}
}
