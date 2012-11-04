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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;
import at.caspase.rxdroid.db.DatabaseHelper;
import at.caspase.rxdroid.util.Util;

@SuppressWarnings("deprecation")
public class PreferencesActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, OnPreferenceClickListener, OnPreferenceChangeListener
{
	private static final String TAG = PreferencesActivity.class.getName();

	private static final int MENU_RESTORE_DEFAULTS = 0;

	SharedPreferences mSharedPreferences;

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if(Version.SDK_IS_PRE_HONEYCOMB)
		{
			// See android issue #4611
			setTheme(android.R.style.Theme);
		}
		else
			setTheme(Theme.get());

		super.onCreate(savedInstanceState);

		mSharedPreferences = getPreferenceManager().getSharedPreferences();
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
		addPreferencesFromResource(R.xml.preferences);

		Preference p = findPreference(Settings.Keys.VERSION);
		if(p != null)
		{
			final int format = BuildConfig.DEBUG ? Version.FORMAT_FULL : Version.FORMAT_SHORT;
			String summary = Version.get(format) + ", DB v" + DatabaseHelper.DB_VERSION;

			if(BuildConfig.DEBUG)
			{
				try
				{
					final String apkModDate = new Date(new File(getPackageCodePath()).lastModified()).toString();
					summary = summary + "\n" + apkModDate;
				}
				catch(NullPointerException e)
				{
					// eat
				}
			}
			else
				summary = getString(R.string.app_name) + " " + summary;

			p.setSummary(summary);
		}

		p = findPreference(Settings.Keys.HISTORY_SIZE);
		if(p != null)
			Util.populateListPreferenceEntryValues(p);

		p = findPreference(Settings.Keys.LICENSES);
		if(p != null)
			p.setOnPreferenceClickListener(this);

		p = findPreference(Settings.Keys.THEME_IS_DARK);
		if(p != null)
			p.setOnPreferenceChangeListener(this);

		p = findPreference(Settings.Keys.NOTIFICATION_SOUND);
		if(p != null)
		{
			p.setOnPreferenceChangeListener(this);

			final String key = Settings.Keys.NOTIFICATION_SOUND;
			final String defValue = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString();
			onPreferenceChange(p, Settings.getString(key, defValue));
		}

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
		LockscreenActivity.startMaybe(this);
		Settings.maybeLockInPortraitMode(this);
		RxDroid.setIsVisible(this, true);
		updateLowSupplyThresholdPreferenceSummary();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		RxDroid.setIsVisible(this, false);
	}

	@TargetApi(11)
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuItem item = menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string._title_restore_default_settings)
				.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case MENU_RESTORE_DEFAULTS:
				showDialog(R.id.preference_reset_dialog);
				return true;

			default:
				// ignore
		}

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
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
		if(Settings.Keys.LOW_SUPPLY_THRESHOLD.equals(key))
			updateLowSupplyThresholdPreferenceSummary();
		else if(Settings.Keys.HISTORY_SIZE.equals(key))
		{
			if("4".equals(Settings.getString(Settings.Keys.HISTORY_SIZE, null)))
				Toast.makeText(getApplicationContext(), R.string._toast_unlimited_history_size, Toast.LENGTH_LONG).show();
		}

		NotificationReceiver.sendBroadcastToSelf(true);
	}

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		if(Settings.Keys.LICENSES.equals(preference.getKey()))
		{
			showDialog(R.id.licenses_dialog);
			return true;
		}

		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		final String key = preference.getKey();

		if(Settings.Keys.NOTIFICATION_SOUND.equals(key))
		{
			final Uri uri = Uri.parse((String) newValue);
			final Ringtone ringtone = RingtoneManager.getRingtone(this, uri);

			if(ringtone != null)
				preference.setSummary(ringtone.getTitle(this));
		}
		else if(Settings.Keys.THEME_IS_DARK.equals(key))
		{
			Theme.clearAttributeCache();

			final Context context = RxDroid.getContext();

			Toast.makeText(context, R.string._toast_theme_changed, Toast.LENGTH_LONG).show();

			final PackageManager pm = context.getPackageManager();
			final Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			RxDroid.doStartActivity(intent);

			finish();
		}

		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		if(id == R.id.licenses_dialog)
		{
			String license;
			InputStream is = null;

			try
			{
				final AssetManager aMgr = getResources().getAssets();
				is = aMgr.open("LICENSE-GPLv3.html", AssetManager.ACCESS_BUFFER);

				license = Util.streamToString(is);
			}
			catch(IOException e)
			{
				Log.w(TAG, e);
				license = "Licensed under the GNU GPLv3";
			}
			finally
			{
				Util.closeQuietly(is);
			}

			final WebView wv = new WebView(getApplicationContext());
			wv.loadData(license, "text/html", null);

			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setTitle(R.string._title_licenses);
			ab.setView(wv);
			ab.setPositiveButton(android.R.string.ok, null);

			return ab.create();
		}
		else if(id == R.id.preference_reset_dialog)
		{
			final AlertDialog.Builder ab= new AlertDialog.Builder(this);
			ab.setIcon(android.R.drawable.ic_dialog_alert);
			ab.setTitle(R.string._title_restore_default_settings);
			ab.setNegativeButton(android.R.string.cancel, null);
			/////////////////////
			ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					mSharedPreferences.edit().clear().commit();
				}
			});

			return ab.create();
		}

		return super.onCreateDialog(id);
	}



	private void updateLowSupplyThresholdPreferenceSummary()
	{
		Preference p = findPreference(Settings.Keys.LOW_SUPPLY_THRESHOLD);
		if(p != null)
		{
			String value = mSharedPreferences.getString(Settings.Keys.LOW_SUPPLY_THRESHOLD, "10");
			p.setSummary(getString(R.string._summary_min_supply_days, value));
		}
	}
}
