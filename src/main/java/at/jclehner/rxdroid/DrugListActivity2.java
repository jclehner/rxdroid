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

import android.annotation.TargetApi;
import android.app.Activity;
import android.database.DataSetObserver;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.github.espiandev.showcaseview.ShowcaseViewBuilder2;

import org.joda.time.LocalDate;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import at.jclehner.androidutils.LoaderListFragment;
import at.jclehner.androidutils.RefString;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.ui.DatePickerDialog;
import at.jclehner.rxdroid.ui.DialogLike;
import at.jclehner.rxdroid.ui.ScheduleViewHolder;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Extras;
import at.jclehner.rxdroid.util.ShowcaseViews;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;
import at.jclehner.rxdroid.widget.DrugNameView;
import at.jclehner.rxdroid.widget.DrugSupplyMonitor;

public class DrugListActivity2 extends AppCompatActivity implements
		DialogLike.OnButtonClickListener
{
	private static final String TAG = DrugListActivity2.class.getSimpleName();

	public static final String EXTRA_DATE = "rxdroid:date";
	public static final String EXTRA_STARTED_FROM_NOTIFICATION = "rxdroid:started_from_notification";

	private boolean mIsShowingDbErrorDialog = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);

		super.onCreate(savedInstanceState);
		//setContentView(R.layout.simple_activity);

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
		else
		{
			if(savedInstanceState == null)
				initDrugListPagerFragment();
			else
			{
				final Fragment f = getFragmentManager().findFragmentByTag("pager");
				if(f instanceof DrugListPagerFragment)
					((DrugListPagerFragment) f).setDate(Settings.getDoseTimeInfo().displayDate(), true);
			}

			// Don't show two dialogs at once
			if(!showBackupAgentRemovalDialogIfNeccessary())
				showPowerSaveWarningDialogIfNeccessary();

			final int backupPwNags = Settings.getInt("backup_pw_nags");

			if(backupPwNags < 3)
			{
				if(!Backup.getBackupFiles(this).isEmpty())
				{
					final Dialog d = new Backup.PasswordDialog(this);
					d.setCancelable(false);
					d.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog)
						{
							Settings.putInt("backup_pw_nags",
									Settings.contains(Settings.Keys.BACKUP_KEY) ? 3 : backupPwNags + 1);
						}
					});
					d.show();
				}
				else
					Settings.putInt("backup_pw_nags", 3);
			}
		}

		NotificationReceiver.rescheduleAlarmsAndUpdateNotification(true);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if(Version.SDK_IS_LOLLIPOP_OR_NEWER)
		{
			if(getIntent().getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false))
			{
				Log.i(TAG, "Rescheduling alarms; better safe than sorry!");
				NotificationReceiver.rescheduleAlarmsAndUpdateNotification(true, false);
				getIntent().putExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
			}
		}
	}

	@Override
	public void onButtonClick(DialogLike dialogLike, int which)
	{
		if(which == DialogLike.BUTTON_NEGATIVE)
		{
			if(deleteDatabase())
			{
				Toast.makeText(this, R.string._toast_db_reset_success, Toast.LENGTH_SHORT).show();
				initDrugListPagerFragment();
			}
			else
				Toast.makeText(this, R.string._toast_db_reset_failure, Toast.LENGTH_LONG).show();
		}
		else
			finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if(mIsShowingDbErrorDialog)
		{
			menu.add(R.string._title_restore).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener()
					{
						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							final Intent intent = new Intent(DrugListActivity2.this, BackupActivity.class);
							intent.putExtra(BackupActivity.EXTRA_NO_BACKUP_CREATION, true);
							startActivity(intent);
							return true;
						}
					}
			);
		}

		return super.onCreateOptionsMenu(menu);
	}

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
		sb.append(Util.getDbErrorMessage(this, error));
		sb.append(" " + RefString.resolve(this, R.string._msg_db_error_footer));

		final DialogLike dialog = new DialogLike();
		dialog.setTitle(getString(R.string._title_error));
		dialog.setMessage(sb);
		dialog.setNegativeButtonText(getString(R.string._btn_reset));
		dialog.setPositiveButtonText(getString(R.string._btn_exit));

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, dialog).commitAllowingStateLoss();

		mIsShowingDbErrorDialog = true;

	}

	private void initDrugListPagerFragment()
	{
		mIsShowingDbErrorDialog = false;
		invalidateOptionsMenu();

		final DrugListPagerFragment f = new DrugListPagerFragment();
		final Bundle args = new Bundle();
		args.putSerializable(DrugListFragment.ARG_DATE, getIntent().getSerializableExtra(EXTRA_DATE));
		//args.putInt(DrugListFragment.ARG_PATIENT_ID, Patient.DEFAULT_PATIENT_ID);
		f.setArguments(args);
		f.setRetainInstance(false);

		getFragmentManager().beginTransaction().replace(android.R.id.content, f, "pager").commit();
	}

	private boolean showBackupAgentRemovalDialogIfNeccessary()
	{
		if(!Settings.getBoolean(Settings.Keys.USE_BACKUP_FRAMEWORK, false))
			return false;

		final AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setMessage(RefString.resolve(this, R.string._msg_backup_agent_removal));
		ab.setCancelable(false);
		ab.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Settings.remove(Settings.Keys.USE_BACKUP_FRAMEWORK);
			}
		});

		ab.show();
		return true;
	}

	private boolean showPowerSaveWarningDialogIfNeccessary()
	{
		if(Settings.wasDisplayedOnce("power_save_warning"))
			return false;

		/*final StringBuilder sb = new StringBuilder();
		sb.append("<p>");
		sb.append(getString(R.string._msg_missed_alarm, "RxDroid"));
		sb.append("</p><p><small>");

		final String[] reasons = getResources().getStringArray(R.array.missed_alarm_reasons);
		for(String reason : reasons)
		{
			sb.append("&bull; ");
			sb.append(Util.escapeHtml(reason));
			sb.append("<br/>");
		}

		sb.append("</small></p><p><small>");
		sb.append(getString(R.string._msg_power_save_warning));
		sb.append("</small></p>");*/

		final String btnStrOrig = getString(android.R.string.ok);

		final AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setMessage(R.string._msg_power_save_warning);
		ab.setCancelable(false);
		ab.setNeutralButton(btnStrOrig, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Settings.setDisplayedOnce("power_save_warning");
			}
		});

		final AlertDialog dialog = ab.create();
		dialog.show();

		final Button btn = dialog.getButton(Dialog.BUTTON_NEUTRAL);
		btn.setEnabled(false);

		new Thread() {
			@Override
			public void run()
			{
				for(int i = 0; i != 10; ++i)
				{
					final String btnStr = btnStrOrig + " (" + (10 - i) + ")";

					btn.post(new Runnable()
					{
						@Override
						public void run()
						{
							btn.setText(btnStr);
						}
					});

					Util.sleepAtMost(1100);
				}

				btn.post(new Runnable()
				{
					@Override
					public void run()
					{
						btn.setEnabled(true);
						btn.setText(btnStrOrig);
					}
				});
			}
		}.start();

		return true;
	}


	private boolean deleteDatabase()
	{
		final String packageName = getApplicationInfo().packageName;

		final File dbDir = new File(Environment.getDataDirectory(), "data/" + packageName + "/databases");
		final File currentDb = new File(dbDir, DatabaseHelper.DB_NAME);

		if(!dbDir.canWrite() || !currentDb.exists() || !currentDb.canWrite())
			return false;

		return currentDb.delete();
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

			@Override
			public void unregisterDataSetObserver(DataSetObserver observer)
			{
				try
				{
					super.unregisterDataSetObserver(observer);
				}
				catch(IllegalStateException e)
				{
					Log.w(TAG, "unregisterDataSetObserver: " + e.getMessage());
				}

			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object)
			{
				try
				{
					super.destroyItem(container, position, object);
				}
				catch(IllegalStateException e)
				{
					Log.w(TAG, "destroyItem: " + e.getMessage());
				}
			}

			@Override
			public void finishUpdate(ViewGroup container)
			{
				try
				{
					super.finishUpdate(container);
				}
				catch(IllegalStateException e)
				{
					Log.w(TAG, "finishUpdate:" + e.getMessage());
				}
			}
		};

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

		private int mLastDoseTimePeriodIndex = -1;

		private boolean mShowingAll = false;

		private Settings.DoseTimeInfo mDtInfo;
		private boolean mUseDateArg = false;

		@Override
		public void onCreate(Bundle icicle)
		{
			super.onCreate(icicle);

			setHasOptionsMenu(true);
			setMenuVisibility(true);

			final Bundle args = icicle != null ? icicle : getArguments();
			mPatientId = args.getInt(ARG_PATIENT_ID);
			mLastDoseTimePeriodIndex = -1;

			if(icicle != null)
				mUseDateArg = icicle.getBoolean("use_date_arg");
			else
				mUseDateArg = getArguments().getSerializable(ARG_DATE) != null;

			updateDoseTimeInfo();

			Log.d(TAG, "onCreate: icicle=" + icicle);
		}

		@Override
		public void onResume()
		{
			super.onResume();

			updateDoseTimeInfo();

			Date date = null;
			boolean forceSetDate = true;

			if(mUseDateArg)
				date = (Date) getArguments().getSerializable(ARG_DATE);

			Log.d(TAG, "onResume: ARG_DATE => " + date);

			if(date == null)
			{
				date = mDtInfo.displayDate();
				//forceSetDate = mLastDoseTimePeriodIndex != mDtInfo.doseTimePeriodIndex();
			}

			mLastDoseTimePeriodIndex = mDtInfo.doseTimePeriodIndex();

			setDate(date, forceSetDate);
			mUseDateArg = false;

			NotificationReceiver.registerOnDoseTimeChangeListener(this);
			SystemEventReceiver.registerOnSystemTimeChangeListener(this);
		}

		@Override
		public void onPause()
		{
			super.onPause();

			NotificationReceiver.unregisterOnDoseTimeChangeListener(this);
			SystemEventReceiver.registerOnSystemTimeChangeListener(this);
		}

		@Override
		public void onSaveInstanceState(Bundle outState)
		{
			super.onSaveInstanceState(outState);
			outState.putBoolean("use_date_arg", mUseDateArg);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.drug_list_fragment, menu);
		}

		@Override
		public void onPrepareOptionsMenu(Menu menu)
		{
			if(mDisplayedDate == null)
				return;

			final int dateCmp = mDisplayedDate.compareTo(mDtInfo.displayDate());

			if(Settings.getBoolean(Settings.Keys.USE_SAFE_MODE, false))
				menu.removeItem(R.id.menuitem_take_all);
			else
				menu.findItem(R.id.menuitem_take_all).setVisible(dateCmp == 0);

			menu.findItem(R.id.menuitem_date).setTitle(dateCmp != 0 ?
					R.string._title_today : R.string._title_go_to_date);

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
					if(mDtInfo.displayDate().equals(mDisplayedDate))
					{
						final DatePickerDialog dialog = new DatePickerDialog(getActivity(),
								LocalDate.fromDateFields(mDisplayedDate), this);
						dialog.setMinDate(DateTime.fromDateFields(
								Settings.getOldestPossibleHistoryDate(DateTime.today())));

						dialog.show();
					}
					else
					{
						setDate(mDtInfo.displayDate(), true);
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
		public void onDateSet(DatePickerDialog datePicker, LocalDate date) {
			setDate(date.toDate(), false);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			final View view = inflater.inflate(R.layout.fragment_drug_list_pager, container, false);
			mPager = ((ViewPager) view.findViewById(R.id.pager));

			return view;
		}

		@TargetApi(17)
		@Override
		public void onStart()
		{
			super.onStart();

			final FragmentManager fm = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1
					? getFragmentManager() : getChildFragmentManager();

			mPager.setSaveEnabled(false);
			mPager.setOffscreenPageLimit(OFFSCREEN_PAGES);
			mPager.addOnPageChangeListener(mPageListener);
			mPager.setAdapter(new MyPagerAdapter(fm));
			mPager.setPageMargin(Util.pixelsFromDips(getActivity(), 48));
		}

		@Override
		public void onStop()
		{
			super.onStop();

			mPager.removeAllViews();
		}

		@Override
		public void onDoseTimeBegin(Date date, int doseTime) {
			updateDoseTimeInfoAndSetToRelevantDate();
		}

		@Override
		public void onDoseTimeEnd(Date date, int doseTime) {
			updateDoseTimeInfoAndSetToRelevantDate();
		}

		@Override
		public void onTimeChanged(int type) {
			updateDoseTimeInfoAndSetToRelevantDate();
		}

		private void updateDoseTimeInfoAndSetToRelevantDate()
		{
			updateDoseTimeInfo();
			setDate(mDtInfo.displayDate(), true);
		}

		private void setDate(Date date, boolean force)
		{
			Log.d(TAG, "setDate: date=" + date + ", force=" + force);

			if(date == null)
				throw new NullPointerException();

			if(!force && date.equals(mDisplayedDate))
				return;

			mReferenceDate = mDisplayedDate = date;

			updateDoseTimeInfo();

			if(mPager == null || mPager.getAdapter() == null)
				return;

			mPager.getAdapter().notifyDataSetChanged();
			mPager.setCurrentItem(CENTER_ITEM, false);

			updateActionBar();
		}

		private void updateDoseTimeInfo() {
			mDtInfo = Settings.getDoseTimeInfo();
		}

		private Date getDateForPage(int page) {
			return DateTime.add(mReferenceDate, Calendar.DAY_OF_MONTH, page - CENTER_ITEM);
		}

		private void updateActionBar()
		{
			getActivity().invalidateOptionsMenu();

			final SpannableString dateStr = new SpannableString(DateTime.toNativeDate(mDisplayedDate));

			if(mDtInfo.displayDate().equals(mDisplayedDate))
				Util.applyStyle(dateStr, new UnderlineSpan());

			//Util.applyStyle(dateStr, new RelativeSizeSpan(0.75f));

			((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(dateStr);
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

			private final Settings.DoseTimeInfo mDtInfo;

			public Adapter(DrugListFragment fragment)
			{
				super(fragment);
				mDtInfo = Settings.getDoseTimeInfo((Calendar)
						fragment.getArguments().getSerializable("time"));
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

					final View frameSupply = (View) holder.supply.getParent();
					frameSupply.post(new Runnable()
					{
						@Override
						public void run()
						{
							final Rect rect = new Rect(0, 0, frameSupply.getWidth(),
										frameSupply.getHeight());
							frameSupply.setTouchDelegate(new TouchDelegate(
									rect, holder.supply));
						}
					});

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
				holder.supply.setToday(mDtInfo.displayDate());
				holder.supply.setDrugAndDate(wrapper.item, wrapper.date);
				holder.supply.setVisibility(wrapper.isSupplyVisible ? View.VISIBLE : View.INVISIBLE);
				holder.history.setTag(wrapper.item.getId());
				holder.missedDoseIndicator.setVisibility((wrapper.isRelevantDate && wrapper.hasMissingDoses)
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
				public boolean isRelevantDate;
				public boolean isSupplyLow;
				public boolean isSupplyVisible;
				public boolean hasMissingDoses;
				public boolean[] doseViewDimmed = { false, false, false, false };
			}

			private static final boolean[] ALL_DIMMED = { true, true, true, true };

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
			public List<DrugWrapper> doLoadInBackground()
			{
				Database.init();

				final List<Drug> allDrugs = Entries.getAllDrugs(mPatientId);
				final List<Drug> drugs = mShowAll ? allDrugs :
						(List<Drug>) CollectionUtils.filter(allDrugs, mFilter);

				Collections.sort(drugs, mComparator);
				final ArrayList<DrugWrapper> data = new ArrayList<DrugWrapper>(drugs.size());

				final int nextDoseTime = mDtInfo.nextDoseTime();
				final int activeDoseTime = mDtInfo.activeDoseTime();

				for(Drug drug : drugs)
				{
					final DrugWrapper wrapper = new DrugWrapper(drug);
					wrapper.date = mDate;
					wrapper.isRelevantDate = mDate.equals(mDtInfo.displayDate());
					wrapper.isSupplyLow = Entries.hasLowSupplies(drug, mDate);
					wrapper.hasMissingDoses = Entries.hasMissingDosesBeforeDate(drug, mDate);
					wrapper.isSupplyVisible = drug.getRefillSize() != 0 && !mDate.before(mDtInfo.displayDate());

					if(wrapper.isRelevantDate && drug.isActive())
					{
						for(int i = 0; i != wrapper.doseViewDimmed.length; ++i)
						{
							final int doseTime = Schedule.TIME_MORNING + i;
							if(doseTime < nextDoseTime || nextDoseTime == Schedule.TIME_MORNING)
							{
								if(!drug.getDose(doseTime, mDate).isZero())
									wrapper.doseViewDimmed[i] = Entries.countDoseEvents(drug, mDate, doseTime) != 0;
								else
									wrapper.doseViewDimmed[i] = true;
							}
							else
								wrapper.doseViewDimmed[i] = true;
						}
					}
					else
						wrapper.doseViewDimmed = ALL_DIMMED;

					data.add(wrapper);
				}

				return data;
			}

			private final CollectionUtils.Filter<Drug> mFilter = new CollectionUtils.Filter<Drug>()
			{
				private final boolean skipAutoDoseEventDrugs = !Settings.getBoolean(
						Settings.Keys.SHOW_SUPPLY_MONITORS, false);

				@Override
				public boolean matches(Drug drug)
				{
					if(!drug.isActive() || (drug.hasAutoDoseEvents() && skipAutoDoseEventDrugs))
						return false;

					if(Entries.countDoseEvents(drug, mDate, null) != 0)
						return true;

					if(mDtInfo.displayDate().equals(mDate))
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
					int l = getSmartSortScore(lhs);
					int r = getSmartSortScore(rhs);

					if (l != r) {
						return l < r ? -1 : 1;
					}

					return 0;
				}

				// lower score is better (higher up)
				private int getSmartSortScore(Drug drug)
				{
					if(!drug.isActive())
						return 100000 - drug.getId();

					int score = drug.hasAutoDoseEvents() ? 50000 : 0;

					if(!Entries.hasAllDoseEvents(drug, mDate, mDtInfo.activeOrNextDoseTime(), false))
						score -= 5000;

					if(!drug.getDose(mDtInfo.activeOrNextDoseTime(), mDate).isZero())
					{
						if(Entries.countDoseEvents(drug, mDate, mDtInfo.activeOrNextDoseTime()) == 0)
							score -= 3000;
					}

					if(Entries.hasLowSupplies(drug, mDate))
						score -= 1000;

					if(mDtInfo.displayDate().equals(mDate))
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
			instance.setRetainInstance(false);

			return instance;
		}

		@Override
		public void onCreate(Bundle icicle)
		{
			super.onCreate(icicle);
			setHasOptionsMenu(true);
			mDate = (Date) getArguments().getSerializable(ARG_DATE);
		}

		@Override
		public void onResume()
		{
			super.onResume();
			Database.registerEventListener(this);
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
				if(BuildConfig.DEBUG)
				{
					try
					{
						final LocalDate fragmentDate = new LocalDate(mDate);
						final LocalDate activityDate = LocalDate.parse((
								(AppCompatActivity) getActivity()).getSupportActionBar().getTitle().toString().replace('/', '-'));

						if(!fragmentDate.equals(activityDate))
						{
							Toast.makeText(getActivity(), "fragmentDate=" + fragmentDate + "\nactivityDate=" + activityDate,
									Toast.LENGTH_LONG).show();
							Log.d(TAG, "Date mismatch:\n  fragmentDate=" + fragmentDate + "\n  activityDate=" + activityDate);
						}
					}
					catch(RuntimeException e)
					{
						Log.w(TAG, e);
					}
				}

				showDoseDialog((DoseView) view, false);
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
					final LocalDate expiry = drug.getExpiryDate();
					final LocalDate supplyEnd = Entries.getSupplyEndDate(drug, mDate);

					final int toastTextId;
					final LocalDate toastDate;

					if(expiry != null && expiry.isBefore(supplyEnd))
					{
						toastTextId = R.string._toast_expiry;
						toastDate = expiry;
					}
					else
					{
						toastTextId = R.string._toast_low_supplies;
						toastDate = supplyEnd;
					}

					Toast.makeText(getActivity(), getString(toastTextId,
							DateTime.toNativeDate(toastDate)), Toast.LENGTH_LONG).show();
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

							if(drug.getRefillSize() != 0)
							{
								drug.setCurrentSupply(drug.getCurrentSupply().plus(dose));
								Database.update(drug);
							}
						}
						else if(menuItem.getItemId() == R.id.menuitem_take)
						{
							// When using the popup menu, force	the dialog to display
							// even when disabled in settings.
							showDoseDialog(doseView, Settings.getBoolean(
									Settings.Keys.SKIP_DOSE_DIALOG, true));
						}
						else if(menuItem.getItemId() == R.id.menuitem_skip)
							Database.create(new DoseEvent(drug, doseView.getDate(), doseTime));
						else if(menuItem.getItemId() == R.id.menuitem_edit)
						{
							final Intent intent = new Intent(getActivity(), DrugEditActivity2.class);
							intent.setAction(Intent.ACTION_EDIT);
							intent.putExtra(DrugEditActivity2.EXTRA_DRUG_ID, drug.getId());

							startActivity(intent);
						}

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
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
		{
			if(menu == null || menu.findItem(R.id.help) != null)
				return;

			menu.add(0, R.id.help, 0, R.string._title_help)
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
					{

						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							showHelpOverlaysIfApplicable(true);
							return true;
						}
					});
		}

		@Override
		protected void onLoaderException(final RuntimeException e)
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					((DrugListActivity2) getActivity()).onLoaderException(e);
				}
			});
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

		@Override
		public void onLoadFinished(List<LLFLoader.ItemHolder<Drug>> data)
		{
			super.onLoadFinished(data);
			setEmptyText(getEmptyText());

			showHelpOverlaysIfApplicable(false);
		}

		private void showDoseDialog(DoseView doseView, boolean force)
		{
			final Bundle args = new Bundle();
			args.putInt(DoseDialog.ARG_DRUG_ID, doseView.getDrug().getId());
			args.putInt(DoseDialog.ARG_DOSE_TIME, doseView.getDoseTime());
			args.putSerializable(DoseDialog.ARG_DATE, doseView.getDate());
			args.putBoolean(DoseDialog.ARG_FORCE_SHOW, force);

			final DoseDialog dialog = new DoseDialog(getActivity());
			dialog.setArgs(args);
			dialog.show();
		}

		private void showHelpOverlaysIfApplicable(boolean force)
		{
			final List<Drug> drugs = Entries.getAllDrugs(getArguments().getInt(ARG_PATIENT_ID,
					Patient.DEFAULT_PATIENT_ID));
			final Settings.DoseTimeInfo dtInfo = Settings.getDoseTimeInfo((Calendar)
					getArguments().getSerializable(ARG_TIME));

			// "date_swipe" is used for historic reasons
			if(force || (drugs.size() == 1 && !Settings.wasDisplayedOnce("date_swipe")))
			{
				Date date = dtInfo.displayDate();
				final Drug drug;

				// Save the activity, because if we call setDate() below, our current fragment
				// will be detached, and getActivity() might return null, thus crashing
				// ShowcaseView below!
				final Activity activity = getActivity();

				if(force)
				{
					final DoseView dv = (DoseView) getActivity().findViewById(R.id.morning);
					if(dv == null)
						return;

					drug = dv.getDrug();
				}
				else
				{
					// FIXME this is bad!

					drug = drugs.get(0);

					int i = 0;

					while(!drug.hasDoseOnDate(date) && ++i < 60)
					{
						date = DateTime.add(date, Calendar.DAY_OF_MONTH, 1);
					}

					if(i == 60)
					{
						Log.w(TAG, "Next date with dose not found");
						return;
					}

					final Fragment f = activity.getFragmentManager().findFragmentByTag("pager");
					if(f instanceof DrugListPagerFragment)
						((DrugListPagerFragment) f).setDate(date, false);
					else
						Log.w(TAG, "Found fragment is not of expected type: " + f);
				}

				int doseToHighlight = R.id.noon;

				for(int i = 0; i != Constants.DOSE_TIMES.length; ++i)
				{
					if(!drug.getDose(Schedule.TIME_MORNING + i, date).isZero())
					{
						doseToHighlight = Constants.DOSE_VIEW_IDS[i];
						break;
					}
				}

				final ShowcaseViews svs = new ShowcaseViews();

				// 1. Swipe date

				ShowcaseViewBuilder2 svb = new ShowcaseViewBuilder2(activity);
				svb.setText(R.string._help_title_swipe_date, R.string._help_msg_swipe_date);
				/*svb.setShotType(ShowcaseView.TYPE_ONE_SHOT);
				svb.setShowcaseId(0xdeadbeef + 0);
				svb.setShowcaseItem(ShowcaseView.ITEM_TITLE, 0, getActivity());

				final DisplayMetrics metrics = new DisplayMetrics();
				getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

				final float w = metrics.widthPixels;
				final float h = metrics.heightPixels;
				final float y = h * 0.6f;

				svb.setAnimatedGesture(-100, y, w, y);*/

				svs.add(tag(svb.build(), "Swipe date"));

				// 2. Edit drug
				svb = new ShowcaseViewBuilder2(activity);
				svb.setText(R.string._help_title_edit_drug, R.string._help_msg_edit_drug);
				//svb.setShotType(ShowcaseView.TYPE_ONE_SHOT);
				//svb.setShowcaseId(0xdeadbeef + 1);
				svb.setShowcaseView(R.id.drug_icon, activity);

				svs.add(tag(svb.build(false), "Edit drug"));

				// 3. Take dose & long press
				svb = new ShowcaseViewBuilder2(activity);
				svb.setText(R.string._help_title_click_dose, R.string._help_msg_click_dose);
				//svb.setShotType(ShowcaseView.TYPE_ONE_SHOT);
				//svb.setShowcaseId(0xdeadbeef + 4);
				svb.setShowcaseView(doseToHighlight, activity);

				svs.add(tag(svb.build(false), "Take dose & long press"));

				Settings.setDisplayedOnce("date_swipe");
				svs.show();
			}
		}

		private ShowcaseView tag(ShowcaseView sv, String tag)
		{
			if(sv != null)
				sv.setTag(tag);
			return sv;
		}

		private String getEmptyText()
		{
			if(Database.countAll(Drug.class) == 0)
			{
				final StringBuilder sb = new StringBuilder(
						RefString.resolve(getActivity(), R.string._msg_no_drugs_compact_ab));
				if(ViewConfiguration.get(getActivity()).hasPermanentMenuKey())
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
				getActivity().invalidateOptionsMenu();
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
