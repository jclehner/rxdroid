/**
 * Copyright (C) 2011, 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.caspase.rxdroid.ui;


import java.util.Date;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TabHost;
import at.caspase.androidutils.compat.TabManager;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.Settings;
import at.caspase.rxdroid.Version;
import at.caspase.rxdroid.db.Drug;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class FragmentTabActivity extends SherlockFragmentActivity
{
	private TabHost mTabHost;
	private TabManager mTabManager;

	private static final boolean FALSE = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.setContentView(R.layout.fragment_tab_activity);

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);


		ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ab.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		if(/* TODO some check */ true)
		{
			//mTabHost.setup();
			//mTabManager = new TabManager(this, mTabHost, R.id.realtabcontent);

			final Object[][] allTabArgs = {
					{ "morning", R.string._title_morning, R.drawable.ic_morning, Drug.TIME_MORNING },
					{ "noon", R.string._title_noon, R.drawable.ic_noon, Drug.TIME_NOON },
					{ "evening", R.string._title_evening, R.drawable.ic_evening, Drug.TIME_EVENING },
					{ "night", R.string._title_night, R.drawable.ic_night, Drug.TIME_NIGHT }
			};

			final Resources res = getResources();
			final Date date = Settings.instance().getActiveDate();

			for(Object[] tabArgs: allTabArgs)
			{
				final String tag = (String) tabArgs[0];
				final int titleResId = (Integer) tabArgs[1];
				final int iconResId = (Integer) tabArgs[2];
				final int doseTime = (Integer) tabArgs[3];

				/*final TabSpec tabSpec = mTabHost.newTabSpec(tag);
				tabSpec.setIndicator(getString(titleResId), res.getDrawable(iconResId));

				final Bundle args = new Bundle();
				args.putSerializable(DrugListFragment.ARG_DATE, date);
				args.putInt(DrugListFragment.ARG_DOSE_TIME, doseTime);

				mTabManager.addTab(tabSpec, DrugListFragment.class, args);*/

				final Bundle args = new Bundle();
				args.putSerializable(DrugListFragment.ARG_DATE, date);
				args.putInt(DrugListFragment.ARG_DOSE_TIME, doseTime);

				final Tab tab = ab.newTab();
				tab
					.setText(titleResId)
					//.setIcon(iconResId)
					.setTabListener(new TabListener<DrugListFragment>(this, tag, DrugListFragment.class, args));

				ab.addTab(tab);
			}
		}

	}

	@Override
	public void setContentView(int layoutResID) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setContentView(View view) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setContentView(View view, LayoutParams params) {
		throw new UnsupportedOperationException();
	}
}
