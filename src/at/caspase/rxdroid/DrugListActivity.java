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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import at.caspase.rxdroid.InfiniteViewPagerAdapter.ViewFactory;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.ui.DrugOverviewAdapter;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Util;

public class DrugListActivity extends Activity implements OnLongClickListener,
		OnDateSetListener, OnSharedPreferenceChangeListener, ViewFactory
{
	private static final String TAG = DrugListActivity.class.getName();
	private static final boolean LOGV = false;

	private static final int MENU_ADD = 0;
	private static final int MENU_PREFERENCES = 1;
	private static final int MENU_TOGGLE_FILTERING = 2;

	private static final int CMENU_TOGGLE_INTAKE = 0;
	// public static final int CMENU_CHANGE_DOSE = 1;
	private static final int CMENU_EDIT_DRUG = 2;
	// public static final int CMENU_SHOW_SUPPLY_STATUS = 3;
	private static final int CMENU_IGNORE_DOSE = 4;

	public static final String EXTRA_DATE = "date";
	public static final String EXTRA_STARTED_FROM_NOTIFICATION = "started_from_notification";

	public static final int TAG_DRUG_ID = R.id.tag_drug_id;

	private SharedPreferences mSharedPreferences;

	private ViewPager mPager;
	private TextView mTextDate;

	private Date mDate;

	private boolean mShowingAll = false;

	private int mSwipeDirection = 0;
	private int mLastPage = -1;

	private boolean mIsShowing = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.drug_list);

		mPager = (ViewPager) findViewById(R.id.drug_list_pager);
		mTextDate = (TextView) findViewById(R.id.text_date);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

		GlobalContext.set(getApplicationContext());
		Database.init();

		mTextDate.setOnLongClickListener(this);
		mTextDate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				final Date activeDate = Settings.instance().getActiveDate();
				setDate(activeDate, PAGER_INIT | PAGER_SCROLL);
			}
		});

		mPager.setOnPageChangeListener(mPageListener);

		Intent intent = getIntent();
		if(intent != null)
			mDate = (Date) intent.getSerializableExtra(EXTRA_DATE);

		if(mDate == null)
		{
			//mDate = DateTime.todayDate();
			mDate = Settings.instance().getActiveDate();
		}

		Database.registerOnChangedListener(mDatabaseListener);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		mIsShowing = true;

		final boolean wasStartedFromNotification;

		Intent intent = getIntent();
		if(intent != null)
			wasStartedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
		else
			wasStartedFromNotification = false;

		//if(wasStartedFromNotification)
		//{
			//int snoozeType = Settings.instance().getListPreferenceValueIndex("snooze_type", -1);
			//if(snoozeType == NotificationReceiver.ALARM_MODE_SNOOZE)
			//{
			//	NotificationService.snooze(this);
			//	Toast.makeText(this, R.string._toast_snoozing, Toast.LENGTH_SHORT).show();
			//}
		//}

		startNotificationService();

		setDate(mDate, PAGER_INIT);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mIsShowing = false;
	}

	@Override
	protected void onStop()
	{
		// TODO Auto-generated method stub
		super.onStop();
		mPager.removeAllViews();
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		Database.unregisterOnChangedListener(mDatabaseListener);
	}

	@TargetApi(11)
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_ADD, 0, R.string._title_add).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_PREFERENCES, 0, R.string._title_preferences).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_TOGGLE_FILTERING, 0, R.string._title_toggle_filtering).setIcon(android.R.drawable.ic_menu_view);

		if(!Version.SDK_IS_PRE_HONEYCOMB)
		{
			menu.getItem(MENU_ADD).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.getItem(MENU_PREFERENCES).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			menu.getItem(MENU_TOGGLE_FILTERING).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case MENU_ADD:
			{
				Intent intent = new Intent(Intent.ACTION_INSERT);
				intent.setClass(this, DrugEditActivity.class);
				startActivity(intent);
				return true;
			}
			case MENU_PREFERENCES:
			{
				Intent intent = new Intent();
				intent.setClass(this, PreferencesActivity.class);
				startActivity(intent);
				return true;
			}
			case MENU_TOGGLE_FILTERING:
			{
				mShowingAll = !mShowingAll;
				setDate(mDate, PAGER_INIT);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		final DoseView doseView = (DoseView) v;
		final Drug drug = doseView.getDrug();
		final int doseTime = doseView.getDoseTime();

		// menu.setHeaderIcon(android.R.drawable.ic_menu_agenda);
		menu.setHeaderTitle(drug.getName());

		// ////////////////////////////////////////////////

		final Intent editIntent = new Intent(this, DrugEditActivity.class);
		editIntent.setAction(Intent.ACTION_EDIT);
		editIntent.putExtra(DrugEditActivity.EXTRA_DRUG, drug);
		menu.add(0, CMENU_EDIT_DRUG, 0, R.string._title_edit_drug).setIntent(editIntent);

		// ////////////////////////////////////////////////

		final boolean wasDoseTaken = doseView.wasDoseTaken();
		final int toggleIntakeMessageId;

		if(wasDoseTaken)
			toggleIntakeMessageId = R.string._title_mark_not_taken;
		else
			toggleIntakeMessageId = R.string._title_mark_taken;

		menu.add(0, CMENU_TOGGLE_INTAKE, 0, toggleIntakeMessageId).setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					if(!wasDoseTaken)
						doseView.performClick();
					else
					{
						Fraction dose = new Fraction();
						for(Intake intake : Entries.findIntakes(drug, mDate, doseTime))
						{
							dose.add(intake.getDose());
							Database.delete(intake);
						}

						drug.setCurrentSupply(drug.getCurrentSupply().plus(dose));
						Database.update(drug);
					}

					return true;
				}
		});
		// ///////////////////////////////////////////////

		// menu.add(0, CMENU_CHANGE_DOSE, 0, R.string._title_change_dose);


		// menu.add(0, CMENU_SHOW_SUPPLY_STATUS, 0, "Show supply status");

		if(!wasDoseTaken)
		{
			menu.add(0, CMENU_IGNORE_DOSE, 0, R.string._title_ignore_dose)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							Database.create(new Intake(drug, mDate, doseTime));
							return true;
						}
					});
		}
	}

	public void onDrugNameClick(View view)
	{
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setClass(this, DrugEditActivity.class);

		Drug drug = Drug.get((Integer) view.getTag(TAG_DRUG_ID));
		intent.putExtra(DrugEditActivity.EXTRA_DRUG, drug);
		//intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		//startActivityForResult(intent, 0);
		startActivity(intent);
	}

	@Override
	public boolean onLongClick(View view)
	{
		if(view.getId() == R.id.text_date)
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(mDate);

			final int year = cal.get(Calendar.YEAR);
			final int month = cal.get(Calendar.MONTH);
			final int day = cal.get(Calendar.DAY_OF_MONTH);

			DatePickerDialog dialog = new DatePickerDialog(this, this, year, month, day);
			dialog.show();
			return true;
		}
		return false;
	}

	@Override
	public void onDateSet(DatePicker view, int year, int month, int day)
	{
		setDate(DateTime.date(year, month, day), PAGER_INIT);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key)
	{
		setDate(mDate, PAGER_INIT);
	}

	@Override
	public View makeView(int offset)
	{
		final TextView textView = new TextView(getApplication());
		textView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		textView.setGravity(Gravity.CENTER);

		final List<Drug> drugs = Database.getAll(Drug.class);
		if(drugs.isEmpty())
		{
			textView.setText(getString(R.string._msg_empty_list_text, getString(R.string._title_add)));
			return textView;
		}

		Collections.sort(drugs);
		final ListView listView = new ListView(getApplication());

		Calendar cal = Calendar.getInstance();
		cal.setTime(mDate);

		// mSwipeDirection is zero when we're initializing the first 3 pages
		// of the view pager (-1, 0, +1).

		if(mSwipeDirection == 0)
			cal.add(Calendar.DAY_OF_MONTH, offset);
		else
			cal.add(Calendar.DAY_OF_MONTH, mSwipeDirection < 0 ? -1 : 1);

		updateListAdapter(listView, cal.getTime(), drugs);

		if(listView.getAdapter().getCount() == 0)
		{
			textView.setText(getString(R.string._msg_no_doses_on_this_day));
			return textView;
		}

		return listView;
	}

	public void onDoseViewClick(View view)
	{
		final DoseView v = (DoseView) view;
		final Drug drug = v.getDrug();
		final int doseTime = v.getDoseTime();
		final Date date = v.getDate();

		if(!date.equals(mDate))
			Log.w(TAG, "Activity date " + mDate + " differs from DoseView date " + date);

		if(!MultipleDialogsPreventer.INSTANCE.isDialogShowing())
		{
			IntakeDialog dialog = new IntakeDialog(this, drug, doseTime, date);
			dialog.setOnShowListener(MultipleDialogsPreventer.INSTANCE);
			dialog.setOnDismissListener(MultipleDialogsPreventer.INSTANCE);
			dialog.show();
		}
		else
			Log.i(TAG, "IntakeDialog is showing, not showing a new one.");

		//Util.sleepAtMost(200);
	}

	public void onNotificationIconClick(View view)
	{
		final Drug drug = (Drug) view.getTag();
		final Calendar cal = DateTime.calendarFromDate(mDate);

		do
		{
			cal.add(Calendar.DAY_OF_MONTH, -1);
		} while(!drug.hasDoseOnDate(cal.getTime()));

		Toast.makeText(getApplicationContext(), R.string._toast_drug_notification_icon, Toast.LENGTH_SHORT).show();

		setDate(cal.getTime(), PAGER_INIT | PAGER_SCROLL);
	}

	private static final int PAGER_SCROLL = 1;
	private static final int PAGER_INIT = 1 << 1;

	private void setDate(Date date, int flags)
	{
		if(LOGV) Log.v(TAG, "setDate: date=" + date + ", flags=" + flags);

		if(!mIsShowing)
		{
			if(LOGV) Log.v(TAG, "setDate: activity is not showing; ignoring");
			return;
		}

		mDate = date;
		getIntent().putExtra(EXTRA_DATE, mDate);

		if((flags & PAGER_INIT) != 0)
		{
			final boolean smoothScroll = (flags & PAGER_SCROLL) != 0;

			mSwipeDirection = 0;
			mLastPage = -1;

			mPager.removeAllViews();
			mPager.setAdapter(new InfiniteViewPagerAdapter(this));
			mPager.setCurrentItem(InfiniteViewPagerAdapter.CENTER, smoothScroll);
		}

		updateDateString();
	}

	private void updateListAdapter(ListView listView, Date date, List<Drug> drugs)
	{
		if(listView == null)
		{
			Log.w(TAG, "updateListAdapter: listView==null");
			return;
		}

		if(drugs == null)
		{
			drugs = Database.getAll(Drug.class);
			Collections.sort(drugs);
		}

		final DrugOverviewAdapter adapter = new DrugOverviewAdapter(this, drugs, date);
		adapter.setFilter(mShowingAll ? null : new DrugFilter(date));

		listView.setAdapter(adapter);
	}

	private void startNotificationService()
	{
		NotificationReceiver.sendBroadcastToSelf(false);
		Database.registerOnChangedListener(DATABASE_WATCHER);
	}

	private void updateDateString()
	{
		//final Date date = DateTime.add(mDate, Calendar.DAY_OF_MONTH, shiftBy);
		final SpannableString dateString = new SpannableString(DateFormat.getDateFormat(this).format(mDate.getTime()));

		if(mDate.equals(DateTime.todayDate()))
			dateString.setSpan(new UnderlineSpan(), 0, dateString.length(), 0);

		mTextDate.setText(dateString);
	}

	private class DrugFilter implements CollectionUtils.Filter<Drug>
	{
		final boolean mShowDoseless = mSharedPreferences.getBoolean("show_doseless", true);
		final boolean mShowInactive = mSharedPreferences.getBoolean("show_inactive", true);

		private Date mFilterDate;

		public DrugFilter(Date date) {
			mFilterDate = date;
		}

		@Override
		public boolean matches(Drug drug)
		{
			boolean result = true;

			if(!mShowDoseless && mFilterDate != null)
			{
				if(!drug.hasDoseOnDate(mFilterDate))
					result = false;
			}

			if(!mShowInactive && !drug.isActive())
				result = false;

			if(!result && Entries.countIntakes(drug, mFilterDate, null) != 0)
				result = true;

			if(!result && DateTime.isToday(mFilterDate) && Entries.hasMissingIntakesBeforeDate(drug, mFilterDate))
				result = true;

			return result;
		}
	}

	private final OnPageChangeListener mPageListener = new OnPageChangeListener() {

		int mPage = InfiniteViewPagerAdapter.CENTER;

		@Override
		public void onPageSelected(int page)
		{
			if(LOGV) Log.v(TAG, "onPageSelected: page=" + page);

			mPage = page;

			if(mLastPage == -1)
				mLastPage = InfiniteViewPagerAdapter.CENTER;
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {}

		@Override
		public void onPageScrollStateChanged(int state)
		{
			if(LOGV)
			{
				final String[] states = { "IDLE", "DRAGGING", "SETTLING" };
				Log.v(TAG, "onPageScrollStateChanged: page=" + mPage);
				Log.v(TAG, "  state=" + states[state]);
			}

			if(state == ViewPager.SCROLL_STATE_IDLE)
			{
				mSwipeDirection = mLastPage != -1 ? mPage - mLastPage : 0;

				if(LOGV) Log.v(TAG, "onPageScrollStateChanged: mPage=" + mPage +
						", mLastPage=" + mLastPage + ", mSwipeDirection=" + mSwipeDirection);

				if(mSwipeDirection != 0)
				{
					final int shiftBy = mSwipeDirection < 0 ? -1 : 1;
					setDate(DateTime.add(mDate, Calendar.DAY_OF_MONTH, shiftBy), 0);
				}

				mLastPage = mPage;
			}
		}
	};

	private final Database.OnChangeListener mDatabaseListener = new Database.OnChangeListener() {

		@Override
		public void onEntryUpdated(Entry entry, int flags) {}

		@Override
		public void onEntryDeleted(Entry entry, int flags)
		{
			if(entry instanceof Drug)
				setDate(mDate, PAGER_INIT);
		}

		@Override
		public void onEntryCreated(Entry entry, int flags)
		{
			if(entry instanceof Drug)
				setDate(mDate, PAGER_INIT);
		}
	};

	private static enum MultipleDialogsPreventer implements OnShowListener, OnDismissListener
	{
		INSTANCE;

		private boolean mIsShowing = false;

		public void reset() {
			mIsShowing = false;
		}

		public boolean isDialogShowing() {
			return mIsShowing;
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			mIsShowing = false;
		}

		@Override
		public void onShow(DialogInterface dialog)
		{
			if(mIsShowing)
				dialog.dismiss();

			mIsShowing = true;
		}
	}

	//private static final MultipleDialogsPreventer MUTIPLE_DIALOGS_PREVENTER = new MultipleDialogsPreventer();

	private static final Database.OnChangeListener DATABASE_WATCHER = new Database.OnChangeListener() {

		@Override
		public void onEntryUpdated(Entry entry, int flags) {
			NotificationReceiver.sendBroadcastToSelf(entry instanceof Intake);
		}

		@Override
		public void onEntryDeleted(Entry entry, int flags) {
			NotificationReceiver.sendBroadcastToSelf(false);
		}

		@Override
		public void onEntryCreated(Entry entry, int flags) {
			NotificationReceiver.sendBroadcastToSelf(false);
		}
	};
}
