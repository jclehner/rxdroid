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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.github.espiandev.showcaseview.ShowcaseViewBuilder2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import at.jclehner.androidutils.DatePickerDialog;
import at.jclehner.androidutils.LoaderListFragment;
import at.jclehner.androidutils.RefString;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.ui.DialogLike;
import at.jclehner.rxdroid.ui.ScheduleViewHolder;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Extras;
import at.jclehner.rxdroid.util.ShowcaseViews;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;
import at.jclehner.rxdroid.widget.DrugNameView;
import at.jclehner.rxdroid.widget.DrugSupplyMonitor;

public class DrugListActivity2 extends ActionBarActivity
{
	public static final String EXTRA_DATE = "rxdroid:date";
	public static final String EXTRA_STARTED_FROM_NOTIFICATION = "rxdroid:started_from_notification";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_activity);

		final ActionBar ab = getSupportActionBar();
		ab.setDisplayShowHomeEnabled(true);
		ab.setDisplayUseLogoEnabled(true);
		ab.setLogo(R.drawable.ic_logo_padded);

		if(Settings.getBoolean(Settings.Keys.IS_FIRST_LAUNCH, true))
		{
			final Intent intent = new Intent(this, DoseTimePreferenceActivity2.class);
			intent.putExtra(DoseTimePreferenceActivity2.EXTRA_IS_FIRST_LAUNCH, true);
			startActivity(intent);
			finish();
		}
		else if(savedInstanceState == null)
		{
			final DrugListPagerFragment f = new DrugListPagerFragment();
			final Bundle args = new Bundle();
			args.putSerializable(DrugListFragment.ARG_DATE, getIntent().getSerializableExtra(EXTRA_DATE));
			//args.putInt(DrugListFragment.ARG_PATIENT_ID, Patient.DEFAULT_PATIENT_ID);
			f.setArguments(args);

			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f, "pager").commit();
		}
	}

	public static class DrugListPagerFragment extends Fragment implements DatePickerDialog.OnDateSetListener,
			NotificationReceiver.OnDoseTimeChangeListener, SystemEventReceiver.OnSystemTimeChangeListener
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
				return DrugListFragment.newInstance(date, mDtInfo.currentTime(), mPatientId, mShowingAll);
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

		public static final String ARG_PATIENT_ID = DrugListFragment.ARG_PATIENT_ID;
		public static final String ARG_DATE = DrugListFragment.ARG_DATE;

		public static final int ITEMS_PER_SIDE = 50;
		public static final int ADAPTER_ITEMS = 1 + 2 * ITEMS_PER_SIDE;
		public static final int CENTER_ITEM = 1 + ITEMS_PER_SIDE;
		public static final int OFFSCREEN_PAGES = 1;

		private ViewPager mPager;

		private int mPatientId;
		private Date mDisplayedDate;
		private Date mReferenceDate;

		private boolean mShowingAll = false;

		private Settings.DoseTimeInfo mDtInfo;
		private boolean mHasExplicitDateRequest = false;

		private int mLastActiveDoseTime;
		private Date mLastActiveDate;

		private ShowcaseViews mShowcaseQueue = new ShowcaseViews();

		@Override
		public void onCreate(Bundle icicle)
		{
			super.onCreate(icicle);

			setHasOptionsMenu(true);
			setMenuVisibility(true);

			final Bundle args = icicle != null ? icicle : getArguments();
			mPatientId = args.getInt(ARG_PATIENT_ID);
			mDisplayedDate = (Date) getArguments().getSerializable(ARG_DATE);
			mHasExplicitDateRequest = mDisplayedDate != null;

			if(icicle != null && mDisplayedDate == null)
			{
				mLastActiveDoseTime = icicle.getInt("last_active_dose_time", -1);
				mLastActiveDate = (Date) icicle.getSerializable("last_active_date");
				mDisplayedDate = (Date) icicle.getSerializable("last_displayed_date");
				mReferenceDate = (Date) icicle.getSerializable("reference_date");

				updateDoseTimeInfo();
			}
		}

		@Override
		public void onResume()
		{
			super.onResume();

			updateDoseTimeInfo();

			if(BuildConfig.DEBUG)
			{
				Toast.makeText(getActivity(), "mHasExplicitDateRequest = "
						+ mHasExplicitDateRequest, Toast.LENGTH_LONG).show();
			}

			if(!mHasExplicitDateRequest)
			{
				// If we're resuming, we want the date to stay untouched,
				// unless the active doseTime and/or date have changed since the
				// last resume.

				if(mDtInfo.activeDoseTime() != mLastActiveDoseTime && !mDtInfo.activeDate().equals(mLastActiveDate))
				{
					Log.i(TAG, "Last active dose time and/or date differ");
					setDate(mDtInfo.activeDate(), true);
				}
				else if(mDisplayedDate != null)
				{
					Log.i(TAG, "Setting date from mDisplayedDate");
					setDate(mDisplayedDate, false);
				}
				else
				{
					Date date = (Date) getArguments().getSerializable("date");
					if(date == null)
					{
						Log.i(TAG, "Setting date to active date");
						date = mDtInfo.activeDate();
					}
					else
						Log.i(TAG, "Date obtained from arguments");

					setDate(date, true);
				}
			}
			else
				setDate(mDisplayedDate, true);

			mHasExplicitDateRequest = false;

			NotificationReceiver.registerOnDoseTimeChangeListener(this);
			SystemEventReceiver.registerOnSystemTimeChangeListener(this);

			mShowcaseQueue.show();
		}

		@Override
		public void onPause()
		{
			super.onPause();

			NotificationReceiver.unregisterOnDoseTimeChangeListener(this);
			SystemEventReceiver.registerOnSystemTimeChangeListener(this);

			updateLastDisplayInfos();
		}

		@Override
		public void onSaveInstanceState(Bundle outState)
		{
			super.onSaveInstanceState(outState);

			outState.putInt("last_active_dose_time", mDtInfo.activeDoseTime());
			outState.putSerializable("last_active_date", mDtInfo.activeDate());
			outState.putSerializable("last_displayed_date", mDisplayedDate);
			outState.putSerializable("reference_date", mReferenceDate);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.activity_drug_list_compact, menu);
		}

		@Override
		public void onPrepareOptionsMenu(Menu menu)
		{
			if(Settings.getBoolean(Settings.Keys.USE_SAFE_MODE, false))
				menu.removeItem(R.id.menuitem_take_all);
			else if(mDisplayedDate != null)
			{
				menu.findItem(R.id.menuitem_take_all).setEnabled(
						mDisplayedDate.equals(mDtInfo.activeDate()));
			}

			menu.findItem(R.id.menuitem_toggle_filtering).setTitle(!mShowingAll ?
				 R.string._title_show_all : R.string._title_filter);
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
						DatePickerDialog.withDate(getActivity(), 0, this, mDisplayedDate).show();
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
					mShowingAll = !mShowingAll;
					setDate(mDisplayedDate, true);
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
			mPager.setAdapter(new MyPagerAdapter(getChildFragmentManager()));
			mPager.setPageMargin(Util.pixelsFromDips(getActivity(), 48));

			return view;
		}

		@Override
		public void onDoseTimeBegin(Date date, int doseTime) {
			updateDoseTimeInfoAndSetToActiveDate();
		}

		@Override
		public void onDoseTimeEnd(Date date, int doseTime) {
			updateDoseTimeInfoAndSetToActiveDate();
		}

		@Override
		public void onTimeChanged(int type) {
			updateDoseTimeInfoAndSetToActiveDate();
		}

		private void updateDoseTimeInfoAndSetToActiveDate()
		{
			updateDoseTimeInfo();
			setDate(mDtInfo.activeDate(), true);
		}

		private void setDate(Date date, boolean force)
		{
			if(date == null)
				throw new NullPointerException();

			if(!force && date.equals(mDisplayedDate))
				return;

			mReferenceDate = mDisplayedDate = date;

			updateDoseTimeInfo();
			updateLastDisplayInfos();

			showHelpOverlaysIfApplicable();

			mPager.getAdapter().notifyDataSetChanged();
			mPager.setCurrentItem(CENTER_ITEM, false);

			updateActionBar();
		}

		private void updateLastDisplayInfos()
		{
			mLastActiveDoseTime = mDtInfo.activeDoseTime();
			mLastActiveDate = mDtInfo.activeDate();
		}

		private void updateDoseTimeInfo() {
			mDtInfo = Settings.getDoseTimeInfo();
		}

		private Date getDateForPage(int page) {
			return DateTime.add(mReferenceDate, Calendar.DAY_OF_MONTH, page - CENTER_ITEM);
		}

		private void updateActionBar()
		{
			getActivity().supportInvalidateOptionsMenu();

			final SpannableString dateStr = new SpannableString(DateTime.toNativeDate(mDisplayedDate));

			if(mDtInfo.activeDate().equals(mDisplayedDate))
				Util.applyStyle(dateStr, new UnderlineSpan());

			Util.applyStyle(dateStr, new RelativeSizeSpan(0.75f));

			((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(dateStr);
		}

		private void showHelpOverlaysIfApplicable()
		{
			if(false && Database.countAll(Drug.class) != 0)
			{
				mShowingAll = true;

				// 1. Swipe date

				ShowcaseViewBuilder2 svb = new ShowcaseViewBuilder2(getActivity());
				svb.setText(R.string._help_title_swipe_date, R.string._help_msg_swipe_date);
				svb.setShotType(ShowcaseView.TYPE_ONE_SHOT);
				svb.setShowcaseId(0xdeadbeef + 0);
				//svb.setShowcaseItem(ShowcaseView.ITEM_TITLE, 0, getActivity());

				final DisplayMetrics metrics = new DisplayMetrics();
				getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

				final float w = metrics.widthPixels;
				final float h = metrics.heightPixels;
				final float y = h * 0.6f;

				svb.setAnimatedGesture(-100, y, w, y);

				mShowcaseQueue.add(tag(svb.build(), "Swipe date"));

				// 2. Edit drug
				svb = new ShowcaseViewBuilder2(getActivity());
				svb.setText(R.string._help_title_edit_drug, R.string._help_msg_edit_drug);
				svb.setShotType(ShowcaseView.TYPE_ONE_SHOT);
				svb.setShowcaseId(0xdeadbeef + 1);
				svb.setShowcaseView(R.id.drug_name, getActivity());
				mShowcaseQueue.add(tag(svb.build(false), "Edit drug"));

				// 3. Take dose & long press
				svb = new ShowcaseViewBuilder2(getActivity());
				svb.setText(R.string._help_title_click_dose, R.string._help_msg_click_dose);
				svb.setShotType(ShowcaseView.TYPE_ONE_SHOT);
				svb.setShowcaseId(0xdeadbeef + 4);
				svb.setShowcaseView(R.id.noon, getActivity());
				mShowcaseQueue.add(tag(svb.build(false), "Take dose & long press"));
			}
		}

		private ShowcaseView tag(ShowcaseView sv, String tag)
		{
			if(sv != null)
				sv.setTag(tag);
			return sv;
		}

		private final ViewPager.OnPageChangeListener mPageListener = new ViewPager.SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected(int page)
			{
				if(mReferenceDate != null)
				{
					mDisplayedDate = getDateForPage(page);
					updateActionBar();
					Log.d(TAG, "onPageSelected: page=" + page + ", date=" + mDisplayedDate);
				}
			}
		};
	}

	public static class DrugListFragment extends LoaderListFragment<Drug> implements View.OnClickListener,
			View.OnLongClickListener, Database.OnChangeListener
	{
		static class Adapter extends LLFAdapter<Drug>
		{
			static class ViewHolder extends ScheduleViewHolder
			{
				DrugNameView name;
				ImageView icon;
				DrugSupplyMonitor supply;
				View history;
				View missedDoseIndicator;
			}

			public Adapter(DrugListFragment fragment) {
				super(fragment);
			}

			@Override
			public View getView(int position, View view, ViewGroup viewGroup)
			{
				final ViewHolder holder;

				if(view == null)
				{
					view = mInflater.inflate(R.layout.drug_view, viewGroup, false);
					holder = new ViewHolder();
					holder.icon = (ImageView) view.findViewById(R.id.drug_icon);
					holder.name = (DrugNameView) view.findViewById(R.id.drug_name);
					holder.supply = (DrugSupplyMonitor) view.findViewById(R.id.text_supply);
					holder.history = view.findViewById(R.id.frame_history_menu);
					holder.missedDoseIndicator = view.findViewById(R.id.img_missed_dose_warning);

					holder.name.setOnClickListener((View.OnClickListener) mFragment);
					holder.history.setOnClickListener((View.OnClickListener) mFragment);
					holder.supply.setOnClickListener((View.OnClickListener) mFragment);

					holder.supply.setOnLongClickListener((View.OnLongClickListener) mFragment);

					holder.setDoseViewsAndDividersFromLayout(view);

					for(DoseView doseView : holder.doseViews)
					{
						doseView.setOnClickListener((View.OnClickListener) mFragment);
						doseView.setOnLongClickListener((View.OnLongClickListener) mFragment);
						//doseView.setOnCreateContextMenuListener(mFragment);
					}

					view.setTag(holder);
				}
				else
					holder = (ViewHolder) view.getTag();

				final Loader.DrugWrapper wrapper = getItemHolder(position);

				holder.name.setDrug(wrapper.item);
				holder.name.setTag(wrapper.item.getId());

				holder.icon.setImageResource(Util.getDrugIconDrawable(wrapper.item.getIcon()));
				holder.supply.setDrugAndDate(wrapper.item, wrapper.date);
				holder.supply.setVisibility(wrapper.isSupplyVisible ? View.VISIBLE : View.INVISIBLE);
				holder.history.setTag(wrapper.item.getId());

				holder.missedDoseIndicator.setVisibility((wrapper.isActiveDate && wrapper.hasMissingDoses)
					? View.VISIBLE : View.GONE);

				for(int i = 0; i != holder.doseViews.length; ++i)
				{
					final DoseView doseView = holder.doseViews[i];

					if(!doseView.hasInfo(wrapper.date, wrapper.item))
						doseView.setDoseFromDrugAndDate(wrapper.date, wrapper.item);

					doseView.setDimmed(wrapper.doseViewDimmed[i]);
				}

				final int dividerVisibility;
				if(view.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
					dividerVisibility = View.GONE;
				else
					dividerVisibility = View.VISIBLE;

				for(View divider : holder.dividers)
				{
					if(divider != null)
						divider.setVisibility(dividerVisibility);
				}

				return view;
			}
		}

		static class Loader extends LLFLoader<Drug>
		{
			public static class DrugWrapper extends ItemHolder<Drug>
			{
				public DrugWrapper(Drug drug) {
					super(drug);
				}

				public Date date;
				public boolean isActiveDate;
				public boolean isSupplyLow;
				public boolean isSupplyVisible;
				public boolean hasMissingDoses;
				public boolean[] doseViewDimmed = { false, false, false, false };
			}

			private final boolean mShowAll;
			private final int mPatientId;
			private final Date mDate;
			private final Settings.DoseTimeInfo mDtInfo;

			public Loader(Context context, Bundle args)
			{
				super(context);

				checkArgs(args);

				mShowAll = args.getBoolean(ARG_SHOW_ALL);
				mDate = (Date) args.getSerializable(ARG_DATE);
				mPatientId = args.getInt(ARG_PATIENT_ID);
				mDtInfo = Settings.getDoseTimeInfo((Calendar) args.getSerializable("time"));
			}

			@Override
			public List<DrugWrapper> loadInBackground()
			{
				Database.init();

				final List<Drug> allDrugs = Entries.getAllDrugs(mPatientId);
				final List<Drug> drugs = mShowAll ? allDrugs :
						(List<Drug>) CollectionUtils.filter(allDrugs, mFilter);

				Collections.sort(drugs, mComparator);
				final ArrayList<DrugWrapper> data = new ArrayList<DrugWrapper>(drugs.size());

				for(Drug drug : drugs)
				{
					final DrugWrapper wrapper = new DrugWrapper(drug);
					wrapper.date = mDate;
					wrapper.isActiveDate = mDate.equals(mDtInfo.activeDate());
					wrapper.isSupplyLow = Entries.hasLowSupplies(drug, mDate);
					wrapper.hasMissingDoses = Entries.hasMissingDosesBeforeDate(drug, mDate);
					wrapper.isSupplyVisible = (drug.getRefillSize() != 0 || !drug.getCurrentSupply().isZero()) && !mDate.before(mDtInfo.activeDate());

					if(wrapper.isActiveDate)
					{
						// This will "underflow" if nextDoseTime is TIME_MORNING, but
						// this doesn't matter since it just dims all doses
						final int maxDoseTimeForNoDim = mDtInfo.nextDoseTime() - 1;

						if(maxDoseTimeForNoDim >= Schedule.TIME_MORNING && mDtInfo.activeDoseTime() != Schedule.TIME_INVALID)
						{
							for(int i = 0; i != wrapper.doseViewDimmed.length; ++i)
							{
								final int doseTime = Schedule.TIME_MORNING + i;
								if(doseTime <= maxDoseTimeForNoDim && !drug.getDose(doseTime, mDate).isZero())
									wrapper.doseViewDimmed[i] = Entries.countDoseEvents(drug, mDate, doseTime) != 0;
								else
									wrapper.doseViewDimmed[i] = true;
							}
						}
					}
					else if(wrapper.date.after(mDtInfo.activeDate()))
						wrapper.doseViewDimmed = new boolean[] { true, true, true, true };

					data.add(wrapper);
				}

				return data;
			}

			private final CollectionUtils.Filter<Drug> mFilter = new CollectionUtils.Filter<Drug>()
			{
				@Override
				public boolean matches(Drug drug)
				{
					if(!drug.isActive() || drug.hasAutoDoseEvents())
						return false;

					if(Entries.countDoseEvents(drug, mDate, null) != 0)
						return true;

					if(mDtInfo.activeDate().equals(mDate))
					{
						if(Entries.hasMissingDosesBeforeDate(drug, mDate))
							return true;

						if(Entries.hasLowSupplies(drug, mDate))
							return true;
					}

					if(!drug.hasDoseOnDate(mDate))
						return false;

					return true;
				}
			};

			private final Comparator<Drug> mComparator = new Comparator<Drug>() {

				@Override
				public int compare(Drug lhs, Drug rhs)
				{
					if(Settings.getBoolean(Settings.Keys.USE_SMART_SORT, false))
					{
						if(getSmartSortScore(lhs) < getSmartSortScore(rhs))
							return -1;
						else
							return 1;
					}

					return lhs.compareTo(rhs);
				}

				// lower score is better (higher up)
				private int getSmartSortScore(Drug drug)
				{
					if(!drug.isActive())
						return 10000 - drug.getId();

					int score = 0;

					if(!Entries.hasAllDoseEvents(drug, mDate, mDtInfo.activeOrNextDoseTime(), false))
						score -= 5000;

					if(!drug.getDose(mDtInfo.activeOrNextDoseTime(), mDate).isZero())
					{
						if(Entries.countDoseEvents(drug, mDate, mDtInfo.activeOrNextDoseTime()) == 0)
							score -= 3000;
					}

					if(Entries.hasLowSupplies(drug, mDate))
						score -= 1000;

					if(DateTime.isToday(mDate))
					{
						if(Entries.hasMissingDosesBeforeDate(drug, mDate))
							score -= 1000;
					}

					if(drug.hasDoseOnDate(mDate))
						score -= 2500;

					return score;
				}
			};
		}

		public static String ARG_DATE = "date";
		public static String ARG_TIME = "time";
		public static String ARG_PATIENT_ID = "patient_id";
		public static String ARG_SHOW_ALL = "show_all";

		private Date mDate;

		public static DrugListFragment newInstance(Date date, Calendar time, int patientId, boolean showAll)
		{
			final Bundle args = new Bundle();
			args.putSerializable(ARG_DATE, date);
			args.putSerializable(ARG_TIME, time);
			args.putInt(ARG_PATIENT_ID, patientId);
			args.putBoolean(ARG_SHOW_ALL, showAll);

			final DrugListFragment instance = new DrugListFragment();
			instance.setArguments(args);

			return instance;
		}

		@Override
		public void onCreate(Bundle icicle)
		{
			super.onCreate(icicle);
			mDate = (Date) getArguments().getSerializable(ARG_DATE);
		}

		@Override
		public void onResume()
		{
			super.onResume();
			Database.registerEventListener(this);

			setEmptyText(getEmptyText());
		}

		@Override
		public void onPause()
		{
			super.onPause();
			Database.unregisterEventListener(this);
		}

		@Override
		public void onClick(View view)
		{
			if(view.getId() == R.id.frame_history_menu)
			{
				Intent intent = new Intent(getActivity(), DoseHistoryActivity.class);
				intent.putExtra(Extras.DRUG_ID, (Integer) view.getTag());
				startActivity(intent);
			}
			else if(view instanceof DoseView)
			{
				final Bundle args = new Bundle();
				args.putInt(DoseDialog.ARG_DRUG_ID, ((DoseView) view).getDrug().getId());
				args.putInt(DoseDialog.ARG_DOSE_TIME, ((DoseView) view).getDoseTime());
				args.putSerializable(DoseDialog.ARG_DATE, mDate);
				args.putBoolean(DoseDialog.ARG_FORCE_SHOW, false);

				final DoseDialog dialog = new DoseDialog(getActivity());
				dialog.setArgs(args);
				dialog.show();
			}
			else if(view.getId() == R.id.drug_name)
			{
				Intent intent = new Intent(getActivity(), DrugEditActivity2.class);
				intent.setAction(Intent.ACTION_EDIT);
				intent.putExtra(DrugEditActivity2.EXTRA_DRUG_ID, (Integer) view.getTag());

				startActivity(intent);
			}
			else if(view instanceof DrugSupplyMonitor)
			{
				final Drug drug = ((DrugSupplyMonitor) view).getDrug();
				if(drug != null)
				{
					//final Date today = DateTime.today();

					final int daysLeft = Entries.getSupplyDaysLeftForDrug(drug, mDate);
					final String dateString = DateTime.toNativeDate(DateTime.add(mDate, Calendar.DAY_OF_MONTH, daysLeft));

					Toast.makeText(getActivity(), getString(R.string._toast_low_supplies, dateString), Toast.LENGTH_LONG).show();
				}
			}
		}

		@Override
		public boolean onLongClick(View view)
		{
			if(view instanceof DrugSupplyMonitor)
			{
				final Drug drug = ((DrugSupplyMonitor) view).getDrug();
				if(drug != null)
				{
					final DrugSupplyEditFragment dialog = DrugSupplyEditFragment.newInstance(drug);
					dialog.show(getFragmentManager(), "supply_edit_dialog");
				}
			}
			else if(view instanceof DoseView)
			{
				final PopupMenu pm = new PopupMenu(getActivity(), view);
				pm.inflate(R.menu.dose_view_context_menu);

				final Menu menu = pm.getMenu();

				final DoseView doseView = (DoseView) view;
				final Drug drug = doseView.getDrug();
				final int doseTime = doseView.getDoseTime();

				if(doseView.getDose().isZero())
				{
					menu.removeItem(R.id.menuitem_skip);
					menu.removeItem(R.id.menuitem_remove_dose);
				}
				else if(doseView.wasDoseTaken())
					menu.removeItem(R.id.menuitem_skip);
				else
					menu.removeItem(R.id.menuitem_remove_dose);

				pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
				{
					@Override
					public boolean onMenuItemClick(MenuItem menuItem)
					{
						if(menuItem.getItemId() == R.id.menuitem_remove_dose)
						{
							final Fraction.MutableFraction dose = new Fraction.MutableFraction();
							for(DoseEvent intake : Entries.findDoseEvents(drug, mDate, doseTime))
							{
								dose.add(intake.getDose());
								Database.delete(intake);
							}

							drug.setCurrentSupply(drug.getCurrentSupply().plus(dose));
							Database.update(drug);
						}
						else if(menuItem.getItemId() == R.id.menuitem_take)
							doseView.performClick();
						else if(menuItem.getItemId() == R.id.menuitem_skip)
							Database.create(new DoseEvent(drug, doseView.getDate(), doseTime));
						else
							return false;

						return true;
					}
				});

				pm.show();

				doseView.setBackgroundColor(Theme.getColorAttribute(R.attr.colorControlHighlight));

				pm.setOnDismissListener(new PopupMenu.OnDismissListener()
				{
					@Override
					public void onDismiss(PopupMenu popupMenu)
					{
						//doseView.setConstantBackgroundResource(0);
						//doseView.setForceSelected(false);
						doseView.setBackgroundResource(Theme.getResourceAttribute(R.attr.selectableItemBackground));
					}
				});
			}


			return true;
		}

		@Override
		protected void onLoaderException(RuntimeException e)
		{
			final DatabaseHelper.DatabaseError error;

			if(e instanceof WrappedCheckedException && ((WrappedCheckedException) e).getCauseType() == DatabaseHelper.DatabaseError.class)
				error = (DatabaseHelper.DatabaseError) e.getCause();
			else if(e instanceof DatabaseHelper.DatabaseError)
				error = (DatabaseHelper.DatabaseError) e;
			else
				throw e;

			final StringBuilder sb = new StringBuilder();

			switch(error.getType())
			{
				case DatabaseHelper.DatabaseError.E_GENERAL:
					sb.append(getString(R.string._msg_db_error_general));
					break;

				case DatabaseHelper.DatabaseError.E_UPGRADE:
					sb.append(getString(R.string._msg_db_error_upgrade));
					break;

				case DatabaseHelper.DatabaseError.E_DOWNGRADE:
					sb.append(getString(R.string._msg_db_error_downgrade));
					break;
			}

			sb.append(" " + RefString.resolve(getActivity(), R.string._msg_db_error_footer));

			final DialogLike dialog = new DialogLike();
			dialog.setTitle(getString(R.string._title_database));
			dialog.setMessage(sb);

			getFragmentManager().beginTransaction().replace(android.R.id.content, dialog).commit();
		}

		@Override
		protected LLFAdapter<Drug> onCreateAdapter() {
			return new Adapter(this);
		}

		@Override
		protected LLFLoader<Drug> onCreateLoader() {
			return new Loader(getActivity(), getArguments());
		}

		private void reloadLoader() {
			getLoaderManager().restartLoader(0, null, this);
		}

		private String getEmptyText()
		{
			if(Database.countAll(Drug.class) == 0)
			{
				final boolean hasHardwareMenuKey;
				if(Version.SDK_IS_PRE_HONEYCOMB)
					hasHardwareMenuKey = true;
				else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				{
					// For Honeycomb, there appears to be no way to find out. As it
					// targets tablets only, we will assume that none of these have a
					// hardware menu key...
					hasHardwareMenuKey = false;
				}
				else
				{
					hasHardwareMenuKey = ViewConfiguration.get(getActivity()).hasPermanentMenuKey();
				}

				final StringBuilder sb = new StringBuilder(
						RefString.resolve(getActivity(), R.string._msg_no_drugs_compact_ab));
				if(hasHardwareMenuKey)
					sb.append(" " + getString(R.string._help_msg_menu_hardware));
				else
					sb.append(" " + getString(R.string._help_msg_menu_ab_overflow));

				return sb.toString();
			}
			else
				return getString(R.string._msg_no_doses_on_this_day);
		}

		private static void checkArgs(Bundle args)
		{
			if(!args.containsKey(ARG_DATE) || !args.containsKey(ARG_PATIENT_ID)
					|| !args.containsKey(ARG_TIME))
				throw new IllegalArgumentException("args=" + args.keySet());
		}



		@Override
		public void onEntryCreated(Entry entry, int flags)
		{
			if(entry instanceof Drug)
				reloadLoader();
			else if(entry instanceof DoseEvent)
				getActivity().supportInvalidateOptionsMenu();
		}

		@Override
		public void onEntryDeleted(Entry entry, int flags)
		{
			if(entry instanceof Drug)
				reloadLoader();
		}

		@Override
		public void onEntryUpdated(Entry entry, int flags)
		{
			reloadLoader();
		}
	}
}
