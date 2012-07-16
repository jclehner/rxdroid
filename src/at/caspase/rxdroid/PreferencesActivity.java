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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.DatabaseHelper;
import at.caspase.rxdroid.preferences.TimePreference;
import at.caspase.rxdroid.ui.FragmentTabActivity;
import at.caspase.rxdroid.util.Util;

@SuppressWarnings("deprecation")
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

		Preference p = findPreference("version");
		if(p != null)
		{
			String summary = Version.get(Version.FORMAT_FULL) + ", DB v" + DatabaseHelper.DB_VERSION;
			p.setSummary(summary);
		}

		p = findPreference("debug_sorter");
		if(p != null)
		{
			p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					//Intent intent = new Intent(PreferencesActivity.this, DrugSortActivity.class);
					//Intent intent = new Intent(PreferencesActivity.this, ObjectToPreferenceTestActivity.class);
					Intent intent = new Intent(PreferencesActivity.this, FragmentTabActivity.class);
					startActivity(intent);
					return true;
				}
			});
		}

		p = findPreference("alarm_mode");
		if(p != null)
		{
			p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					updateAlarmModePreferenceSummary(preference, newValue.toString());
					return true;
				}
			});

			updateAlarmModePreferenceSummary(p, null);
		}

		p = findPreference("debug_generate_db_source");
		if(p != null)
		{
			p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					Database.generateJavaSourceForDbUpgrade();
					return true;
				}
			});

		}

		p = findPreference("time_morning_begin");
		if(p != null)
		{
			p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					Log.d(TAG, "onPreferenceChange: preference=" + preference +
							", type=" + newValue.getClass() + ", value=" + newValue);
					return true;
				}
			});
		}

		Util.populateListPreferenceEntryValues(findPreference("alarm_mode"));

		if(!Version.SDK_IS_PRE_HONEYCOMB)
		{
			ActionBar ab = getActionBar();
			ab.setDisplayShowHomeEnabled(true);
			ab.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		updateLowSupplyThresholdPreferenceSummary();
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

		if(!Version.SDK_IS_PRE_HONEYCOMB)
		{
			if(item.getItemId() == android.R.id.home)
			{
				Intent intent = new Intent(this, DrugListActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
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

	private void updateAlarmModePreferenceSummary(Preference p, String value)
	{
		if(value == null)
		{
			if((value = ((ListPreference) p).getValue()) == null)
				return;
		}

		int index = ((ListPreference) p).findIndexOfValue(value);

		Preference alarmTimeout = findPreference("alarm_timeout");
		alarmTimeout.setEnabled(index > 0);

		switch(index)
		{
			case 1:
				final DumbTime timeout = ((TimePreference) alarmTimeout).getValue();
				final int minutes;

				if(timeout != null)
					minutes = 60 * timeout.getHours() + timeout.getMinutes();
				else
					minutes = 0;

				p.setSummary(getString(R.string._summary_alarm_mode_repeat, minutes));
				break;

			case 0:
				// fall through

			default:
				p.setSummary(R.string._summary_alarm_mode_normal);
				break;

		}
	}
}
