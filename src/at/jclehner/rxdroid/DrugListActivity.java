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

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;
import at.jclehner.rxdroid.Fraction.MutableFraction;
import at.jclehner.rxdroid.InfiniteViewPagerAdapter.ViewFactory;
import at.jclehner.rxdroid.NotificationReceiver.OnDoseTimeChangeListener;
import at.jclehner.rxdroid.Settings.Defaults;
import at.jclehner.rxdroid.Settings.DoseTimeInfo;
import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Intake;
import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.ui.DrugOverviewAdapter;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Extras;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.widget.AutoDragSortListView;
import at.jclehner.rxdroid.widget.DrugSupplyMonitor;

import com.mobeta.android.dslv.DragSortListView;

public class DrugListActivity extends FragmentActivity implements OnLongClickListener,
		OnDateSetListener, OnSharedPreferenceChangeListener, ViewFactory
{
	private static final String TAG = DrugListActivity.class.getSimpleName();
	private static final boolean LOGV = false;

	private static final boolean DEBUG_DATE_MISMATCH = true;

	private static final int CMENU_TOGGLE_INTAKE = 0;
	private static final int CMENU_EDIT_DRUG = 2;
	private static final int CMENU_IGNORE_DOSE = 4;
	private static final int CMENU_LOG = 5;
	private static final int CMENU_DUMP = 6;

	private static final int DIALOG_INFO = 0;

	public static final String EXTRA_DATE = "date";
	public static final String EXTRA_STARTED_FROM_NOTIFICATION = "started_from_notification";

	public static final int TAG_DRUG_ID = R.id.tag_drug_id;

	private ViewPager mPager;
	private TextView mTextDate;

	private Date mOriginalDate;
	private Date mCurrentDate;

	private boolean mShowingAll = false;
	private int mCurrentPatientId = Patient.DEFAULT_PATIENT_ID;

	private int mSwipeDirection = 0;
	private int mLastPage = -1;

	private boolean mIsShowing = false;

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, 0);

		setContentView(R.layout.drug_list);

		mPager = (ViewPager) findViewById(R.id.drug_list_pager);
		mTextDate = (TextView) findViewById(R.id.text_date);

		mTextDate.setOnLongClickListener(mDateClickListener);
		mTextDate.setOnClickListener(mDateClickListener);

		// FIXME hack
		if(!Version.SDK_IS_PRE_HONEYCOMB)
			mTextDate.setVisibility(View.GONE);

		//mPager.setOnPageChangeListener(mPageListener);
		mPager.setOffscreenPageLimit(1);

		//startNotificationService();
		NotificationReceiver.rescheduleAlarmsAndUpdateNotification(true);

		Database.registerEventListener(mDatabaseListener);
		Settings.registerOnChangeListener(this);

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		Components.onResumeActivity(this, 0);
		mIsShowing = true;

		final Intent intent = getIntent();
		Date date = null;

		if(intent != null)
			date = (Date) intent.getSerializableExtra(EXTRA_DATE);

		if(date == null)
			date = Settings.getActiveDate();

		setDate(date, PAGER_INIT);
		NotificationReceiver.registerOnDoseTimeChangeListener(mDoseTimeListener);

		if(true)
		{
			final String isoLang = Locale.getDefault().getLanguage();
			if(!CollectionUtils.contains(Version.LANGUAGES, isoLang))
			{
				final String language = Locale.getDefault().getDisplayLanguage(Locale.US);
				showInfoDialog("missing_translation_" + isoLang, R.string._msg_no_translation, language);
			}
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mIsShowing = false;
		Components.onPauseActivity(this, 0);
		NotificationReceiver.unregisterOnDoseTimeChangeListener(mDoseTimeListener);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		mPager.removeAllViews();
		Database.unregisterEventListener(mDatabaseListener);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event)
	{
		// Using the trackball to navigate the ViewPager is very likely to cause a
		// problem with Activity date / DoseView date mismatch. Until this is fixed,
		// we eat all trackball events.
		return true;
	}

	@TargetApi(11)
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		final int menuResId;

		if(Settings.getBoolean(Keys.COMPACT_ACTION_BAR, Defaults.COMPACT_ACTION_BAR))
			menuResId = R.menu.activity_drug_list_compact;
		else
			menuResId = R.id.menu_default_drug_list_activity;

		new MenuInflater(this).inflate(menuResId, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
		{
			final int titleResId = isCurrentDateActive() ? R.string._title_go_to_date : R.string._title_today;
			menu.findItem(R.id.menuitem_date).setTitle(titleResId);
		}

		menu.findItem(R.id.menuitem_toggle_filtering).setTitle(mShowingAll ? R.string._title_filter : R.string._title_show_all);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.menuitem_date:
			{
				if(isCurrentDateActive())
					mDateClickListener.onLongClick(null);
				else
					mDateClickListener.onClick(null);

				return true;
			}
			case R.id.menuitem_patient:
			{

			}
			case R.id.menuitem_add:
			{
				Intent intent = new Intent(Intent.ACTION_INSERT);
				intent.setClass(this, DrugEditActivity.class);
				startActivity(intent);
				return true;
			}
			case R.id.menuitem_preferences:
			{
				Intent intent = new Intent();
				intent.setClass(this, PreferencesActivity.class);
				startActivity(intent);
				return true;
			}
			case R.id.menuitem_toggle_filtering:
			{
				mShowingAll = !mShowingAll;
				invalidateViewPager();
				return true;
			}

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, final View v, ContextMenuInfo menuInfo)
	{
		final DoseView doseView = (DoseView) v;

		if(toastIfPastMaxHistoryAge(doseView.getDate()))
			return;

		final Drug drug = doseView.getDrug();
		final int doseTime = doseView.getDoseTime();

		// menu.setHeaderIcon(android.R.drawable.ic_menu_agenda);
		menu.setHeaderTitle(drug.getName());

		// ////////////////////////////////////////////////

		final Intent editIntent = new Intent(this, DrugEditActivity.class);
		editIntent.setAction(Intent.ACTION_EDIT);
		editIntent.putExtra(DrugEditActivity.EXTRA_DRUG_ID, drug.getId());
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
						MutableFraction dose = new MutableFraction();
						for(Intake intake : Entries.findIntakes(drug, mCurrentDate, doseTime))
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

		if(!wasDoseTaken)
		{
			menu.add(0, CMENU_IGNORE_DOSE, 0, R.string._title_ignore_dose)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							Database.create(new Intake(drug, doseView.getDate(), doseTime));
							return true;
						}
					});
		}

		if(BuildConfig.DEBUG)
		{
			menu.add(0, CMENU_LOG, 0, "Log")
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							Intent intent = new Intent(getApplicationContext(), DoseHistoryActivity.class);
							intent.putExtra(Extras.DRUG_ID, drug.getId());
							startActivity(intent);
							return true;
						}
					});

			menu.add(0, CMENU_DUMP, 0, "Dump")
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							Util.dumpObjectMembers(TAG, Log.VERBOSE, drug, drug.getName());
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
		intent.putExtra(DrugEditActivity.EXTRA_DRUG_ID, drug.getId());
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		//startActivityForResult(intent, 0);
		startActivity(intent);
	}

	@Override
	public boolean onLongClick(View view)
	{
		if(view.getId() == R.id.text_date)
		{

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
		if(Settings.Keys.THEME_IS_DARK.equals(key))
			finish(); // TODO fix this naughty hack
		else if(mIsShowing)
			invalidateViewPager();
	}

	@Override
	public View makeView(int offset)
	{
		if(offset <= -(InfiniteViewPagerAdapter.MAX/2))
		{
			if(LOGV) Log.d(TAG, "makeView: returning stub for offset=" + offset);
			return new ViewStub(this);
		}

		final View v = getLayoutInflater().inflate(R.layout.drug_list_fragment, null);
		final AutoDragSortListView listView = (AutoDragSortListView) v.findViewById(android.R.id.list);
		final TextView emptyView = (TextView) v.findViewById(android.R.id.empty);

		final Date date = DateTime.add(mOriginalDate, Calendar.DAY_OF_MONTH, offset);

		if(LOGV) Log.d(TAG, "makeView: date=" + DateTime.toDateString(date));

		final List<Drug> drugs = Entries.getAllDrugs(mCurrentPatientId);
		Collections.sort(drugs, new DrugComparator());

		updateListAdapter(listView, date, drugs);

		final int emptyResId;

		if(drugs.isEmpty())
		{
			if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
			{
				if(Settings.getBoolean(Keys.COMPACT_ACTION_BAR, Defaults.COMPACT_ACTION_BAR))
				{
					Log.d(TAG, "COMPACT_ACTION_BAR");
					emptyResId = R.string._msg_no_drugs_compact_ab;
				}
				else
				{
					Log.d(TAG, "EXTENDED_ACTION_BAR");
					emptyResId = R.string._msg_no_drugs_extended_ab;
				}
			}
			else
				emptyResId = R.string._msg_no_drugs_no_ab;
		}
		else
			emptyResId = R.string._msg_no_doses_on_this_day;

		emptyView.setText(getString(emptyResId, getString(R.string._title_add)));

		listView.setEmptyView(emptyView);
		listView.setDragHandleId(R.id.drug_icon);

		return v;
	}

	public void onDoseViewClick(View view)
	{
		final DoseView doseView = (DoseView) view;
		Date date = doseView.getDate();

		if(toastIfPastMaxHistoryAge(date))
			return;
		else if(!date.equals(mCurrentDate))
		{
			Log.i(TAG, "DoseView date " + DateTime.toDateString(date) +
					" differs from Activity date " + DateTime.toDateString(mCurrentDate) + " ");

			invalidateViewPager();
			date = mCurrentDate;

			if(BuildConfig.DEBUG)
				Toast.makeText(this, "Invoked workaround!", Toast.LENGTH_SHORT).show();

			//throw new IllegalStateException("Activity date " + mCurrentDate + " differs from DoseView date " + date);
		}

		final Bundle args = new Bundle();
		args.putInt(IntakeDialog.ARG_DRUG_ID, doseView.getDrug().getId());
		args.putInt(IntakeDialog.ARG_DOSE_TIME, doseView.getDoseTime());
		args.putSerializable(IntakeDialog.ARG_DATE, date);

		showDialog(R.id.dose_dialog, args);
	}

	public void onMissedIndicatorClicked(View view)
	{
		final Drug drug = (Drug) view.getTag();
		final Calendar cal = DateTime.calendarFromDate(mCurrentDate);

		do
		{
			cal.add(Calendar.DAY_OF_MONTH, -1);
		} while(!drug.hasDoseOnDate(cal.getTime()));

		Toast.makeText(getApplicationContext(), R.string._toast_drug_notification_icon, Toast.LENGTH_SHORT).show();

		setDate(cal.getTime(), PAGER_INIT | PAGER_SCROLL);
	}

	public void onLowSupplyIndicatorClicked(View view)
	{
		final Drug drug = ((DrugSupplyMonitor) view).getDrug();
		if(drug == null)
			return;

		final Date today = DateTime.today();

		final int daysLeft = Entries.getSupplyDaysLeftForDrug(drug, today);
		final String dateString = DateTime.toNativeDate(DateTime.add(today, Calendar.DAY_OF_MONTH, daysLeft));

		Toast.makeText(this, getString(R.string._toast_low_supplies, dateString), Toast.LENGTH_LONG).show();
	}

	public void onSupplyMonitorLongClick(View view)
	{
		final Drug drug = ((DrugSupplyMonitor) view).getDrug();
		if(drug == null)
			return;

		final DrugSupplyEditFragment dialog = DrugSupplyEditFragment.newInstance(drug);
		dialog.show(getSupportFragmentManager(), "supply_edit_dialog");
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id, Bundle args)
	{
		if(id == R.id.dose_dialog)
			return new IntakeDialog(this);
		else if(id == DIALOG_INFO)
		{
			final String msg = args.getString("msg");
			final String onceId = args.getString("once_id");

			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setIcon(android.R.drawable.ic_dialog_info);
			ab.setTitle(R.string._title_info);
			ab.setMessage(msg);
			ab.setCancelable(false);
			ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Settings.setDisplayedOnce(onceId);
				}
			});

			return ab.create();
		}

		return super.onCreateDialog(id, args);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args)
	{
		if(id == R.id.dose_dialog)
		{
			if(!Database.exists(Drug.class, args.getInt(IntakeDialog.ARG_DRUG_ID, -1)))
			{
				// If the drug currently associated with the dialog is deleted,
				// setArgs() throws when attempting to restore from the non-existent
				// drug id.
				return;
			}

			((IntakeDialog) dialog).setArgs(args);
		}
		else
			super.onPrepareDialog(id, dialog, args);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	private static final int PAGER_SCROLL = 1;
	private static final int PAGER_INIT = 1 << 1;

	private void invalidateViewPager() {
		setDate(mCurrentDate, PAGER_INIT);
	}

	@TargetApi(11)
	private void setDate(Date date, int flags)
	{
		if(DEBUG_DATE_MISMATCH) Log.d(TAG, "setDate(" + date + ", " + flags + ")");

		if(!mIsShowing)
		{
			if(LOGV) Log.v(TAG, "setDate: activity is not showing; ignoring");
			return;
		}

		if(toastIfPastMaxHistoryAge(date))
			return;


		mCurrentDate = date;
		getIntent().putExtra(EXTRA_DATE, date);

		if((flags & PAGER_INIT) != 0)
		{
			mOriginalDate = date;
			mSwipeDirection = 0;

			mPager.setOnPageChangeListener(null);
			mPager.removeAllViews();

			final int drugCount = Database.countAll(Drug.class);
			if(drugCount != 0)
			{
				final boolean smoothScroll = (flags & PAGER_SCROLL) != 0;

				mPager.setAdapter(new InfiniteViewPagerAdapter(this));
				mPager.setCurrentItem(InfiniteViewPagerAdapter.CENTER, smoothScroll);

				mLastPage = InfiniteViewPagerAdapter.CENTER;

				if(drugCount == 1)
					showInfoDialog("date_swipe", R.string._msg_swipe_to_change_date);
				else if(drugCount >= 2)
					showInfoDialog(Settings.OnceIds.DRAG_DROP_SORTING, R.string._msg_drag_drop_sorting);
			}
			else
			{
				mPager.setAdapter(new PagerAdapter() {

					@Override
					public boolean isViewFromObject(View v, Object o) {
						return v == (View) o;
					}

					@Override
					public int getCount()
					{
						// TODO Auto-generated method stub
						return 1;
					}

					@Override
					public Object instantiateItem(ViewGroup container, int position)
					{
						final View v = makeView(0);
						Util.detachFromParent(v);
						container.addView(v);
						return v;
					}

					@Override
					public void destroyItem(ViewGroup container, int position, Object item) {
						container.removeView((View) item);
					}
				});

				mPager.setCurrentItem(0);
				mLastPage = 0;
			}

			mPager.setOnPageChangeListener(mPageListener);
		}

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
			invalidateOptionsMenu();

		updateDateString();
	}

	private boolean isCurrentDateActive() {
		return DateTime.isToday(mCurrentDate);
	}

	private void updateListAdapter(DragSortListView listView, Date date, List<Drug> drugs)
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
//
//	private void startNotificationService()
//	{
//		NotificationReceiver.rescheduleAlarmsAndUpdateNotification(false);
//		Database.registerEventListener(mDatabaseWatcher);
//	}

	@TargetApi(11)
	private void updateDateString()
	{
		if(mCurrentDate == null)
			return;

		final SpannableString dateString =
				new SpannableString(DateFormat.getDateFormat(this).format(mCurrentDate.getTime()));

		if(isCurrentDateActive())
			Util.applyStyle(dateString, new UnderlineSpan());

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
		{
			Util.applyStyle(dateString, new RelativeSizeSpan(0.75f));
			getActionBar().setSubtitle(dateString);
		}
		else
			mTextDate.setText(dateString);
	}

	private boolean toastIfPastMaxHistoryAge(Date date)
	{
		if(Settings.isPastMaxHistoryAge(DateTime.today(), date))
		{
			Toast.makeText(this, R.string._toast_past_max_history_age, Toast.LENGTH_LONG).show();
			//mPager.setCurrentItem(mLastPage);
			//setDate(mDate, PAGER_INIT | PAGER_SCROLL);
			return true;
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	private void showInfoDialog(String onceId, int msgResId, Object... args)
	{
		if(Settings.wasDisplayedOnce(onceId))
			return;

		Bundle bundle = new Bundle();
		bundle.putString("once_id", onceId);
		bundle.putString("msg", getString(msgResId, args));

		showDialog(DIALOG_INFO, bundle);
	}

	static class DrugFilter implements CollectionUtils.Filter<Drug>
	{
		final boolean mShowSupplyMonitors = Settings.getBoolean(Settings.Keys.SHOW_SUPPLY_MONITORS, false);

		private Date mFilterDate;

		public DrugFilter(Date date) {
			mFilterDate = date;
		}

		@Override
		public boolean matches(Drug drug)
		{
			if(mFilterDate == null)
				return true;

			if(drug.isAutoAddIntakesEnabled())
			{
				if(Entries.hasLowSupplies(drug))
					return true;

				return mShowSupplyMonitors;
			}

			if(!drug.isActive())
				return false;

			if(Entries.countIntakes(drug, mFilterDate, null) != 0)
				return true;

			if(Entries.hasLowSupplies(drug))
				return true;

			if(DateTime.isToday(mFilterDate) && Entries.hasMissingIntakesBeforeDate(drug, mFilterDate))
				return true;

			if(!drug.hasDoseOnDate(mFilterDate))
				return false;

			return true;
		}
	}

	interface OnClickAndLongClickListener extends OnClickListener, OnLongClickListener {};

	private final OnClickAndLongClickListener mDateClickListener = new OnClickAndLongClickListener() {

		@Override
		public boolean onLongClick(View v)
		{
			/*Calendar cal = DateTime.calendarFromDate(mDate);

			final int year = cal.get(Calendar.YEAR);
			final int month = cal.get(Calendar.MONTH);
			final int day = cal.get(Calendar.DAY_OF_MONTH);

			DatePickerDialog dialog = new DatePickerDialog(DrugListActivity.this, DrugListActivity.this, year, month, day);
			dialog.setCancelable(true);
			dialog.show();
			return true;*/

			DatePickerFragment datePicker = DatePickerFragment.newInstance(mCurrentDate, DrugListActivity.this);
			datePicker.show(getSupportFragmentManager(), "datePicker");
			return true;
		}

		@Override
		public void onClick(View v)
		{
			final Date activeDate = Settings.getActiveDate();
			setDate(activeDate, PAGER_INIT | PAGER_SCROLL);
		}
	};

	private final OnPageChangeListener mPageListener = new OnPageChangeListener() {

		int mPage = InfiniteViewPagerAdapter.CENTER;

		@Override
		public void onPageSelected(int page)
		{
			if(DEBUG_DATE_MISMATCH) Log.d(TAG, "onPageSelected(" + page + ")");

			mPage = page;

			//final int swipeDirection;

			if(mLastPage == -1)
			{
				mLastPage = InfiniteViewPagerAdapter.CENTER;
				mSwipeDirection = 0;
			}
			else
				mSwipeDirection = mPage - mLastPage;

			if(mSwipeDirection != 0)
			{
				final Date date = DateTime.add(mCurrentDate, Calendar.DAY_OF_MONTH, mSwipeDirection);
				setDate(date, 0);
				//if(LOGV) Log.d(TAG, "onPageSelected: swipe " + (mSwipeDirection < 0 ? "right" : "left"));

			}
			//else if(LOGV) Log.d(TAG, "onPageSelected: no swipe");

			mLastPage = page;
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {}

		@Override
		public void onPageScrollStateChanged(int state)
		{
//			if(LOGV)
//			{
//				final String[] states = { "IDLE", "DRAGGING", "SETTLING" };
//				Log.v(TAG, "onPageScrollStateChanged: page=" + mPage);
//				Log.v(TAG, "  state=" + states[state]);
//			}
//
//			if(state == ViewPager.SCROLL_STATE_IDLE)
//			{
//				mSwipeDirection = mLastPage != -1 ? mPage - mLastPage : 0;
//
//				if(LOGV) Log.v(TAG, "onPageScrollStateChanged: mPage=" + mPage +
//						", mLastPage=" + mLastPage + ", mSwipeDirection=" + mSwipeDirection);
//
//				if(mSwipeDirection != 0)
//				{
//					//final int shiftBy = mSwipeDirection < 0 ? -1 : 1;
//					//setDate(DateTime.add(mDate, Calendar.DAY_OF_MONTH, shiftBy), 0);
//				}
//
//				//mLastPage = mPage;
//			}
		}
	};

	private final Database.OnChangeListener mDatabaseListener = new Database.EmptyOnChangeListener() {

		@SuppressWarnings("deprecation")
		@Override
		public void onEntryDeleted(Entry entry, int flags)
		{
			try
			{
				DrugListActivity.this.removeDialog(R.id.dose_dialog);
			}
			catch(Exception e)
			{

			}

			if(entry instanceof Drug)
				invalidateViewPager();
		}

		@Override
		public void onEntryCreated(Entry entry, int flags)
		{
			if(entry instanceof Drug)
				invalidateViewPager();
		}
	};

	private final OnDoseTimeChangeListener mDoseTimeListener = new OnDoseTimeChangeListener() {

		@Override
		public void onDoseTimeBegin(Date date, int doseTime)
		{
			if(!date.equals(mCurrentDate))
				setDate(date, PAGER_INIT);
		}

		public void onDoseTimeEnd(Date date, int doseTime)
		{
			invalidateViewPager();
		}
	};

	static class DrugComparator implements Comparator<Drug>
	{
		private int mDoseTime;
		private Date mDate;
		private boolean mSmartSortEnabled;

		DrugComparator()
		{
			final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();
			mDoseTime = dtInfo.activeDoseTime();

			if(mDoseTime != Schedule.TIME_INVALID)
				mDate = dtInfo.activeDate();
			else
			{
				mDoseTime = dtInfo.nextDoseTime();
				mDate = dtInfo.nextDoseTimeDate();
			}

			mSmartSortEnabled = Settings.getBoolean(Keys.USE_SMART_SORT, false);
		}

		@Override
		public int compare(Drug lhs, Drug rhs)
		{
			if(mSmartSortEnabled)
			{
				boolean lActive = lhs.isActive();
				boolean rActive = rhs.isActive();

				if(lActive != rActive)
					return lActive ? -1 : 1;

				boolean lHasDose = lhs.hasDoseOnDate(mDate);
				boolean rHasDose = rhs.hasDoseOnDate(mDate);

				if(lHasDose != rHasDose)
					return lHasDose ? -1 : 1;

				lHasDose = !lhs.getDose(mDoseTime, mDate).isZero();
				rHasDose = !rhs.getDose(mDoseTime, mDate).isZero();

				if(lHasDose != rHasDose)
					return lHasDose ? -1 : 1;
			}

			return lhs.compareTo(rhs);
		}
	}
}
