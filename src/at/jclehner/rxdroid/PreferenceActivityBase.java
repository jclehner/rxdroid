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

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public abstract class PreferenceActivityBase extends PreferenceActivity
{
	protected void onCreate(Bundle savedInstanceState)
	{
		Settings.init();

		// See android issue #4611
		setTheme(Version.SDK_IS_PRE_HONEYCOMB ? android.R.style.Theme : Theme.get());
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		LockscreenActivity.startMaybe(this);
		Settings.maybeLockInPortraitMode(this);
		RxDroid.setIsVisible(this, true);

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER && isHomeButtonEnabled())
		{
			ActionBar ab = getActionBar();
			ab.setDisplayShowHomeEnabled(isHomeButtonEnabled());
			ab.setDisplayHomeAsUpEnabled(isHomeButtonEnabled());
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		RxDroid.setIsVisible(this, false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
		{
			if(item.getItemId() == android.R.id.home)
			{
				final Intent intent = getHomeButtonIntent();
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);

				startActivity(intent);

				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	protected abstract Intent getHomeButtonIntent();

	protected boolean isHomeButtonEnabled() {
		return true;
	}
}
