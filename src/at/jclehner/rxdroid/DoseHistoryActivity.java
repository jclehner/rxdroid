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

import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.ui.DoseLogFragment;
import at.jclehner.rxdroid.ui.ExpandableListFragment;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.Extras;

/*
 * TODO
 *
 * Layout:
 * +---------------------------------------+
 * | 2013-05-08                            |
 * +---------------------------------------+
 * | 06:33   Morning dose taken (1 1/2)
 * |
 * |
 * +---------------------------------------+
 * | 2013-05-07                          Δ  |
 * +---------------------------------------+
 * | 06:33   Morning dose taken (1 1/2)
 * | 13:10   Evening dose taken (1)
 * |
 * |   Δ         Noon dose not taken!
 * |
 * |
 * |
 * |
 * |
 *
 *
 *
 *
 */

public class DoseHistoryActivity extends FragmentActivity
{
	private Drug mDrug;
	private boolean mIsAllCollapsed = true;

	private static final int MENU_VIEW = 0;
	private static final int MENU_COLLAPSE_EXPAND = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Components.onCreateActivity(this, 0);

		// Drug drug = (Drug) getIntent().getSerializableExtra(Extras.DRUG);
		mDrug = Drug.get(getIntent().getIntExtra(Extras.DRUG_ID, 0));

		setTitle(mDrug.getName());

		mIsAllCollapsed = Settings.getBoolean(Keys.LOG_IS_ALL_COLLAPSED, true);
		updateLogFragment();

		// setListAdapter(new DoseHistoryAdapter(this, mDrug));

	}

	@TargetApi(11)
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuItem item;

		item = menu.add(0, MENU_VIEW, 0, R.string._title_view)
				.setIcon(android.R.drawable.ic_menu_view)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						ViewOptionsDialogFragment f = new ViewOptionsDialogFragment();
						f.show(getSupportFragmentManager(), "view_options");
						return true;
					}
				});

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
		{
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}

		final int iconAttr = mIsAllCollapsed ? R.attr.actionIconExpandAll : R.attr.actionIconCollapseAll;
		final int titleResId = mIsAllCollapsed ? R.string._title_expand : R.string._title_collapse;

		item = menu.add(0, MENU_COLLAPSE_EXPAND, 0, titleResId)
				.setIcon(Theme.getResourceAttribute(iconAttr))
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						FragmentManager fm = getSupportFragmentManager();

						ExpandableListFragment f = (ExpandableListFragment) fm.findFragmentByTag("log");
						if(f != null)
						{
							if(mIsAllCollapsed)
								f.expandAll(true);
							else
								f.collapseAll();

							mIsAllCollapsed = !mIsAllCollapsed;
							Settings.putBoolean(Keys.LOG_IS_ALL_COLLAPSED, mIsAllCollapsed);

							if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
								invalidateOptionsMenu();
						}

						return true;
					}
				});

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
		{
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}


		return super.onCreateOptionsMenu(menu);
	}

	void updateLogFragment()
	{
		int flags = 0;

		if(Settings.getBoolean(Keys.LOG_SHOW_MISSED, true))
			flags |= DoseLogFragment.SHOW_MISSED;
		if(Settings.getBoolean(Keys.LOG_SHOW_SKIPPED, true))
			flags |= DoseLogFragment.SHOW_SKIPPED;
		if(Settings.getBoolean(Keys.LOG_SHOW_TAKEN, true))
			flags |= DoseLogFragment.SHOW_TAKEN;

		final FragmentManager fm = getSupportFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();
		final DoseLogFragment f = DoseLogFragment.newInstance(mDrug, flags);

		if(!Settings.getBoolean(Keys.LOG_IS_ALL_COLLAPSED, true))
			f.expandAll(false);
		else
			f.collapseAll();

		ft.replace(android.R.id.content, f, "log");
		ft.commit();
	}

	class ViewOptionsDialogFragment extends DialogFragment
	{
		private boolean[] mChecked;
		private boolean mWasChanged;

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			mChecked = new boolean[] {
					Settings.getBoolean(Keys.LOG_SHOW_MISSED, true),
					Settings.getBoolean(Keys.LOG_SHOW_SKIPPED, true),
					Settings.getBoolean(Keys.LOG_SHOW_TAKEN, true) };

			mWasChanged = false;

			final String[] items = { getString(R.string._title_missed),
					getString(R.string._title_skipped),
					getString(R.string._title_taken) };

			Builder ab = new Builder(getActivity());
			ab.setMultiChoiceItems(items, mChecked,
					new OnMultiChoiceClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which,
								boolean isChecked)
						{
							if(!mWasChanged)
								mWasChanged = true;

							mChecked[which] = isChecked;

							final String key;
							switch(which)
							{
								case 0:
									key = Keys.LOG_SHOW_MISSED;
									break;
								case 1:
									key = Keys.LOG_SHOW_SKIPPED;
									break;
								case 2:
									key = Keys.LOG_SHOW_TAKEN;
									break;
								default:
									return;
							}

							Settings.putBoolean(key, isChecked);
						}
					});

			// ab.setNegativeButton(android.R.string.cancel, null);
			ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if(mWasChanged)
						updateLogFragment();
				}
			});

			return ab.create();
		}
	}
}
