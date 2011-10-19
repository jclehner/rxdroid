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
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import at.caspase.rxdroid.db.DatabaseHelper;
import at.caspase.rxdroid.debug.SleepState;
import at.caspase.rxdroid.util.DateTime;

public class PreferencesActivity extends PreferenceActivity
{
	@SuppressWarnings("unused")
	private static final String TAG = PreferencesActivity.class.getName();

	private static final int MENU_RESTORE_DEFAULTS = 0;

	SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mSharedPreferences = getPreferenceManager().getSharedPreferences();
		addPreferencesFromResource(R.xml.preferences);
		
		final Preference versionPref = findPreference("version");
		if(versionPref != null)
			versionPref.setSummary(Version.get(Version.FORMAT_FULL) + ", DB v" + DatabaseHelper.DB_VERSION);
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
	public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref)
	{
		if(pref.getKey().equals("debug_view_sleep_state"))
		{
			final TextView textView = new TextView(this);
			textView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
			
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Sleep state");
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setView(textView);
						
			final Runnable dialogUpdater = new Runnable() {
				
				@Override
				public void run()
				{					
					final String message = 
						"Sleeping : " + SleepState.INSTANCE.isSleeping() + "\n" +
						"Begin    : " + DateTime.toString(SleepState.INSTANCE.getBegin()) + "\n" + 
						"End      : " + DateTime.toString(SleepState.INSTANCE.getEnd()) + "\n" + 
						"Remaining: " + SleepState.INSTANCE.getRemainingTime().toString(true);
					textView.setText(message);
				}
			};
			
			final Handler handler = new Handler(getMainLooper());
			
			final Thread updaterThread = new Thread(new Runnable() {
				
				@Override
				public void run()
				{
					while(true)
					{
						handler.post(dialogUpdater);
						try
						{
							Thread.sleep(1000);
						}
						catch (InterruptedException e)
						{
							break;
						}
					}
				}
			});
			
			updaterThread.start();
			
			AlertDialog dialog = builder.create();
			
			dialog.setOnDismissListener(new OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface dialog)
				{
					updaterThread.interrupt();					
				}
			});
			
			dialog.show();
			
			return true;
		}
		
		return super.onPreferenceTreeClick(prefScreen, pref);
		
	}
}
