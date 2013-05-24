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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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
