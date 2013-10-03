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
import android.widget.ListView;
import at.jclehner.androidutils.PreferenceActivity;
import at.jclehner.rxdroid.util.Components;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;

public abstract class PreferenceActivityBase extends PreferenceActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, 0);
		super.onCreate(savedInstanceState);

		final ListView list = getListView();
		if(list != null)
		{
			final int resId = Theme.getResourceAttribute(
					com.actionbarsherlock.R.attr.selectableItemBackground);
			list.setSelector(resId);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Components.onResumeActivity(this, 0);

		if(isHomeButtonEnabled())
		{
			final ActionBar ab = getSupportActionBar();
			ab.setDisplayShowHomeEnabled(isHomeButtonEnabled());
			ab.setDisplayHomeAsUpEnabled(isHomeButtonEnabled());
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Components.onPauseActivity(this, 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		final boolean consumed = super.onOptionsItemSelected(item);
		if(!consumed)
		{
			if(item.getItemId() == android.R.id.home)
			{
				final Intent intent = getHomeButtonIntent();
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);

				startActivity(intent);

				return true;
			}
		}

		return consumed;
	}

	/*
	@SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen ps, Preference p)
    {
		// see http://code.google.com/p/android/issues/detail?id=4611#c35
		if(Version.SDK_IS_PRE_HONEYCOMB && !mUseDarkTheme)
		{
			super.onPreferenceTreeClick(ps, p);
			setTheme(Theme.get());

			try
			{
				if(p != null)
				{
					if(p instanceof PreferenceScreen)
					{
						final Dialog d = ((PreferenceScreen) p).getDialog();

						if(d != null)
						{
							d.getWindow().getDecorView().setBackgroundDrawable(this.getWindow()
									.getDecorView().getBackground().getConstantState().newDrawable());

							//((PreferenceScreen) p).set
						}
					}
				}
			}
			catch(Exception e)
			{
				// it's ugly, but who knows if this function works on all flavours of android

				Log.w(TAG, "Caught exception - falling back to dark theme!", e);
				Intent intent = new Intent(this, getClass());
				intent.putExtra(EXTRA_DARK_THEME, true);
				startActivity(intent);
				finish();
			}

			return false;
		}

		return super.onPreferenceTreeClick(ps, p);
    }*/

	@Override
	protected boolean shouldShowIndeterminateProgressOnScreenChange() {
		return true;
	}

	protected abstract Intent getHomeButtonIntent();

	protected boolean isHomeButtonEnabled() {
		return true;
	}
}
