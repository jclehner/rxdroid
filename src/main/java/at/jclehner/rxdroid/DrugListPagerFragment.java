/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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


import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;

import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

public class DrugListPagerFragment extends Fragment implements DatePickerDialog.OnDateSetListener,
		NotificationReceiver.OnDoseTimeChangeListener
{
	private class MyPagerAdapter extends FragmentStatePagerAdapter
	{
		public MyPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			final Date date = getDateForPage(position);
			return DrugListActivity2.DrugListFragment.newInstance(date, mPatientId, mDtInfo);
		}

		@Override
		public int getCount() {
			return ADAPTER_ITEMS;
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
	};

	private static final String TAG = DrugListPagerFragment.class.getSimpleName();

	public static final String ARG_PATIENT_ID = DrugListActivity2.DrugListFragment.ARG_PATIENT_ID;
	public static final String ARG_DATE = DrugListActivity2.DrugListFragment.ARG_DATE;

	public static final int ITEMS_PER_SIDE = 50;
	public static final int ADAPTER_ITEMS = 1 + 2 * ITEMS_PER_SIDE;
	public static final int CENTER_ITEM = 1 + ITEMS_PER_SIDE;
	public static final int OFFSCREEN_PAGES = 2;

	private ViewPager mPager;

	private int mPatientId;
	private Date mDisplayedDate;
	private Date mDateOrigin;
	private Settings.DoseTimeInfo mDtInfo;

	public void setDate(Date date, boolean force)
	{
		if(date == null)
			throw new NullPointerException();

		if(mDateOrigin.equals(date) && !force)
			return;

		Log.d(TAG, "setDate: date=" + date);

		mDateOrigin = mDisplayedDate = date;
		mDtInfo = Settings.getDoseTimeInfo();

		mPager.getAdapter().notifyDataSetChanged();
		mPager.setCurrentItem(CENTER_ITEM, false);

		updateActionBarDate();
	}

	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		setHasOptionsMenu(true);
		setMenuVisibility(true);

		final Bundle args = icicle != null ? icicle : getArguments();

		mPatientId = args.getInt(ARG_PATIENT_ID);
		mDateOrigin = mDisplayedDate = (Date) args.getSerializable(ARG_DATE);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		final int menuResId;

		if(Settings.getBoolean(Settings.Keys.COMPACT_ACTION_BAR, Settings.Defaults.COMPACT_ACTION_BAR))
			menuResId = R.menu.activity_drug_list_compact;
		else
		{
			if(!BuildConfig.DEBUG)
				menuResId = R.menu.menu_default_drug_list_activity;
			else
				menuResId = R.menu.activity_drug_list_extended;
		}

		inflater.inflate(menuResId, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		if(Settings.getBoolean(Settings.Keys.USE_SAFE_MODE, false))
			menu.removeItem(R.id.menuitem_take_all);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.menuitem_date:
			{
				if(mDtInfo.activeDate().equals(mDisplayedDate))
				{
					DatePickerFragment f = DatePickerFragment.newInstance(mDisplayedDate, this);
					f.show(getFragmentManager(), "date");
				}
				else
				{
					setDate(mDtInfo.activeDate(), true);
				}

				return true;
			}
			case R.id.menuitem_patient:
			{

			}
			case R.id.menuitem_add:
			{
				Intent intent = new Intent(Intent.ACTION_INSERT);
				intent.setClass(getActivity(), DrugEditActivity2.class);
				startActivity(intent);
				return true;
			}
			case R.id.menuitem_preferences:
			{
				Intent intent = new Intent();
				intent.setClass(getActivity(), SettingsActivity.class);
				startActivity(intent);
				return true;
			}
			case R.id.menuitem_toggle_filtering:
			{
				/*mShowingAll = !mShowingAll;
				invalidateViewPager();*/
				return true;
			}
			case R.id.menuitem_take_all:
			{
				Entries.markAllNotifiedDosesAsTaken(mPatientId);
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDateSet(DatePicker datePicker, int year, int month, int day) {
		setDate(DateTime.date(year, month, day), false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View view = inflater.inflate(R.layout.fragment_drug_list_pager, container, false);
		mPager = ((ViewPager) view.findViewById(R.id.pager));
		mPager.setOffscreenPageLimit(OFFSCREEN_PAGES);
		mPager.setOnPageChangeListener(mPageListener);
		mPager.setAdapter(new MyPagerAdapter(getFragmentManager()));
		mPager.setPageMargin(Util.pixelsFromDips(getActivity(), 48));

		setDate(mDateOrigin, true);

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		NotificationReceiver.registerOnDoseTimeChangeListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		NotificationReceiver.unregisterOnDoseTimeChangeListener(this);
	}

	@Override
	public void onDoseTimeBegin(Date date, int doseTime) {
		setDate(date, true);
	}

	@Override
	public void onDoseTimeEnd(Date date, int doseTime) {
		setDate(date, true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		outState.putInt(ARG_PATIENT_ID, mPatientId);
		outState.putSerializable(ARG_DATE, mDisplayedDate);
	}

	private Date getDateForPage(int page) {
		return DateTime.add(mDateOrigin, Calendar.DAY_OF_MONTH, page - CENTER_ITEM);
	}

	private void updateActionBarDate()
	{
		final SpannableString dateStr = new SpannableString(DateTime.toNativeDate(mDisplayedDate));
		Util.applyStyle(dateStr, new RelativeSizeSpan(0.75f));

		if(mDtInfo.activeDate().equals(mDisplayedDate))
			Util.applyStyle(dateStr, new UnderlineSpan());

		((ActionBarActivity) getActivity()).getSupportActionBar().setSubtitle(dateStr);
	}

	private final ViewPager.OnPageChangeListener mPageListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int page)
		{
			mDisplayedDate = getDateForPage(page);
			updateActionBarDate();
			Log.d(TAG, "onPageSelected: page=" + page + ", date=" + mDisplayedDate);
		}
	};
}
