/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
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

package at.jclehner.rxdroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

public class DoseTimePreferenceActivity extends PreferenceActivityBase
{
	public static final String EXTRA_IS_FIRST_LAUNCH = "at.jclehner.rxroid.extras.IS_FIRST_LAUNCH";
	private boolean mIsFirstLaunch;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mIsFirstLaunch = getIntent().getBooleanExtra(EXTRA_IS_FIRST_LAUNCH, false);

		if(mIsFirstLaunch)
			setContentView(R.layout.activity_dose_time_settings);

		addPreferencesFromResource(R.xml.dose_times);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuItem item = menu.add(R.string._title_pref_restore_defaults);
		item.setIcon(android.R.drawable.ic_menu_revert);
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				final AlertDialog.Builder ab = new AlertDialog.Builder(DoseTimePreferenceActivity.this);
				ab.setIcon(android.R.drawable.ic_dialog_alert);
				ab.setTitle(R.string._title_warning);
				ab.setMessage(R.string._title_restore_default_settings);
				ab.setNegativeButton(android.R.string.cancel, null);
				/////////////////////
				ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Settings.putString("time_morning", getString(R.string.pref_default_time_morning));
						Settings.putString("time_noon", getString(R.string.pref_default_time_noon));
						Settings.putString("time_evening", getString(R.string.pref_default_time_evening));
						Settings.putString("time_night", getString(R.string.pref_default_time_night));

						getPreferenceScreen().removeAll();
						addPreferencesFromResource(R.xml.dose_times);
					}
				});

				ab.show();

				return true;
			}
		});

		if(Version.SDK_IS_PRE_HONEYCOMB && true)
		{
			item.setIcon(R.drawable.ic_action_undo);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		else
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		return super.onCreateOptionsMenu(menu);
	}

	public void onSaveButtonClicked(View view)
	{
		Settings.putBoolean(Settings.Keys.IS_FIRST_LAUNCH, false);

		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setClass(this, DrugListActivity.class);
		startActivity(intent);

		finish();
	}

	@Override
	protected Intent getHomeButtonIntent()
	{
		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setClass(this, PreferencesActivity.class);
		return intent;
	}

	@Override
	protected boolean isHomeButtonEnabled() {
		return !mIsFirstLaunch;
	}
}
