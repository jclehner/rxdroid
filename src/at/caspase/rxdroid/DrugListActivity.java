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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.ViewSwitcher.ViewFactory;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Timer;

public class DrugListActivity extends Activity implements
	OnLongClickListener, OnDateSetListener, OnSharedPreferenceChangeListener,
	ViewFactory, OnGestureListener, OnTouchListener
{
	private static final String TAG = DrugListActivity.class.getName();
	private static final boolean LOGV = true;

	public static final int MENU_ADD = 0;
	public static final int MENU_PREFERENCES = 1;
	public static final int MENU_TOGGLE_FILTERING = 2;

	public static final int CMENU_TOGGLE_INTAKE = 0;
	//public static final int CMENU_CHANGE_DOSE = 1;
	public static final int CMENU_EDIT_DRUG = 2;
	//public static final int CMENU_SHOW_SUPPLY_STATUS = 3;
	public static final int CMENU_IGNORE_DOSE = 4;

	public static final String EXTRA_DAY = "day";
	public static final String EXTRA_STARTED_FROM_NOTIFICATION = "started_from_notification";

	private static final int TAG_ID = R.id.tag_drug_id;

	private LayoutInflater mInflater;

	private ViewSwitcher mViewSwitcher;
	private TextView mMessageOverlay;
	private GestureDetector mGestureDetector;
	private TextView mTextDate;

	private Date mDate;

	private boolean mShowingAll = false;

	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.drug_list);

		mInflater = LayoutInflater.from(this);

		mViewSwitcher = (ViewSwitcher) findViewById(R.id.drug_list_view_flipper);
		mMessageOverlay = (TextView) findViewById(android.R.id.empty);
		mTextDate = (TextView) findViewById(R.id.med_list_footer);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

		GlobalContext.set(getApplicationContext());
		Database.init(); // must be called before mViewSwitcher.setFactory!

		mViewSwitcher.setFactory(this);
		mTextDate.setOnLongClickListener(this);

		findViewById(R.id.view_switcher_container).setOnTouchListener(this);

		mGestureDetector = new GestureDetector(this, this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		final boolean wasStartedFromNotification;

		Intent intent = getIntent();
		if(intent != null)
		{
			wasStartedFromNotification =
					intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);

			if(LOGV) Log.d(TAG, "onResume: EXTRA_STARTED_FROM_NOTIFICATION=" + wasStartedFromNotification);
		}
		else
			wasStartedFromNotification = false;

		shiftDate(0);

		if(wasStartedFromNotification)
		{
			int snoozeType = Settings.instance().getListPreferenceValueIndex("snooze_type", -1);
			if(snoozeType == NotificationReceiver.SNOOZE_MANUAL)
			{
				NotificationService.snooze(this);
				Toast.makeText(this, R.string._toast_snoozing, Toast.LENGTH_SHORT).show();
			}
		}

		startNotificationService();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_TOGGLE_FILTERING, 0, R.string._title_toggle_filtering).setIcon(android.R.drawable.ic_menu_view);
		menu.add(0, MENU_ADD, 0, R.string._title_add).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_PREFERENCES, 0, R.string._title_preferences).setIcon(android.R.drawable.ic_menu_preferences);

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

				final ListView currentView = (ListView) mViewSwitcher.getCurrentView();
				final DrugAdapter adapter = (DrugAdapter) currentView.getAdapter();

				if(mShowingAll)
					adapter.setFilter(null);
				else
					adapter.setFilter(new DrugFilter());

				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		final DoseView doseView = (DoseView) v;
		final Drug drug = Drug.get(doseView.getDrugId());
		final int doseTime = doseView.getDoseTime();

		//menu.setHeaderIcon(android.R.drawable.ic_menu_agenda);
		menu.setHeaderTitle(drug.getName());

		final boolean wasDoseTaken = doseView.wasDoseTaken();
		final int toggleIntakeMessageId;

		if(wasDoseTaken)
			toggleIntakeMessageId = R.string._title_mark_not_taken;
		else
			toggleIntakeMessageId = R.string._title_mark_taken;

		//////////////////////////////////////////////////
		menu.add(0, CMENU_TOGGLE_INTAKE, 0, toggleIntakeMessageId).setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				if(!wasDoseTaken)
					doseView.performClick();
				else
				{
					Fraction dose = new Fraction();

					for(Intake intake : Intake.findAll(drug, mDate, doseTime))
					{
						dose.add(intake.getDose());
						Database.delete(intake);
					}

					Log.d(TAG, "onMenuItemClick: adding " + dose + " to current supply of " + drug.getName());
					drug.setCurrentSupply(drug.getCurrentSupply().plus(dose));
					Database.update(drug);
				}

				return true;
			}
		});
		/////////////////////////////////////////////////

		//menu.add(0, CMENU_CHANGE_DOSE, 0, R.string._title_change_dose);

		final Intent editIntent = new Intent(this, DrugEditActivity.class);
		editIntent.setAction(Intent.ACTION_EDIT);
		editIntent.putExtra(DrugEditActivity.EXTRA_DRUG, drug);
		menu.add(0, CMENU_EDIT_DRUG, 0, R.string._title_edit_drug).setIntent(editIntent);
		//menu.add(0, CMENU_SHOW_SUPPLY_STATUS, 0, "Show supply status");

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
					}
			);
		}
	}

	public void onNavigationClick(View view)
	{
		setProgressBarIndeterminateVisibility(true);

		switch(view.getId())
		{
			case R.id.med_list_footer:
				shiftDate(0);
				break;
			case R.id.med_list_prev:
				shiftDate(-1);
				break;
			case R.id.med_list_next:
				shiftDate(+1);
				break;
			default:
				Log.w(TAG, "onNavigationClick: unhandled view id " + view.getId());
		}

		setProgressBarIndeterminateVisibility(false);
	}

	public void onDrugNameClick(View view)
	{
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setClass(this, DrugEditActivity.class);

		Drug drug = Drug.get((Integer) view.getTag(TAG_ID));
		intent.putExtra(DrugEditActivity.EXTRA_DRUG, drug);

		startActivityForResult(intent, 0);
	}

	@Override
	public boolean onLongClick(View view)
	{
		if(view.getId() == R.id.med_list_footer)
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
		setDate(DateTime.date(year, month, day));
	}

	public void onDoseClick(final View view)
	{
		final DoseView v = (DoseView) view;
		final Drug drug = Drug.get(v.getDrugId());

		final int doseTime = v.getDoseTime();

		IntakeDialog dialog = new IntakeDialog(this, drug, doseTime, mDate);
		dialog.show();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key)
	{
		// causes the ListView to be refreshed
		setDate(mDate);
	}

	@Override
	public View makeView()
	{
		ListView lv = new ListView(this);
		return lv;
	}

	/////////////

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		final float begX = e1 != null ? e1.getX() : 0.0f;
		final float endX = e2 != null ? e2.getX() : 0.0f;
		final float diffX = Math.abs(begX - endX);

		Log.d(TAG, "onFling: diffX=" + diffX + ", velocityX=" + velocityX);

		// TODO determine whether these are suitable values for this purpose
		if(diffX > 50 && Math.abs(velocityX) > 800)
		{
			shiftDate(begX < endX ? -1 : 1);
			return true;
		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	/////////////

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if(mGestureDetector.onTouchEvent(event))
			return true;
		return super.onTouchEvent(event);
	}

	/////////////

	private void startNotificationService()
	{
		Intent serviceIntent = new Intent();
		serviceIntent.setClass(this, NotificationService.class);

		startService(serviceIntent);
	}

	private void setDate(Date newDate) {
		setOrShiftDate(0, newDate);
	}

	private void shiftDate(int shiftBy) {
		setOrShiftDate(shiftBy, null);
	}

	// shift to previous (-1) or next(1) date. passing 0
	// will reset to specified date, or current date
	// if newDate is -1
	private void setOrShiftDate(int shiftBy, Date newDate)
	{
		setProgressBarIndeterminateVisibility(true);

		if(shiftBy == 0)
		{
			if(newDate == null)
				mDate = Settings.instance().getActiveDate();
			else if(mDate != newDate)
				mDate = newDate;

			mViewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
			mViewSwitcher.setOutAnimation(null);
		}
		else
		{
			mDate = DateTime.add(mDate, Calendar.DAY_OF_MONTH, shiftBy);

			if(shiftBy == 1)
			{
				mViewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
				mViewSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
			}
			else if(shiftBy == -1)
			{
				mViewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
				mViewSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
			}
			else
				throw new IllegalArgumentException();
		}

		updateNextView();
		mViewSwitcher.showNext();

		if(Database.getAll(Drug.class).isEmpty())
		{
			mMessageOverlay.setText(getString(R.string._msg_empty_list_text, getString(R.string._title_add)));
			mMessageOverlay.setVisibility(View.VISIBLE);
		}
		else if(((ListView) mViewSwitcher.getCurrentView()).getAdapter().getCount() == 0)
		{
			mMessageOverlay.setText(getString(R.string._msg_no_doses_on_this_day));
			mMessageOverlay.setVisibility(View.VISIBLE);
		}
		else
			mMessageOverlay.setVisibility(View.GONE);

		final SpannableString dateString = new SpannableString(DateFormat.getDateFormat(this).format(mDate.getTime()));

		if(mDate.equals(DateTime.todayDate()))
			dateString.setSpan(new UnderlineSpan(), 0, dateString.length(), 0);

		mTextDate.setText(dateString);
		//setTitle(getString(R.string.app_name) + " - " + dateString.toString());

		// update the intent so our Activity is restarted with the last opened date
		setIntent(getIntent().putExtra(EXTRA_DAY, mDate));

		setProgressBarIndeterminateVisibility(false);
	}

	private void updateNextView()
	{
		final ListView nextView = (ListView) mViewSwitcher.getNextView();

		final DrugAdapter adapter = new DrugAdapter(this, R.layout.dose_view, Database.getAll(Drug.class), mDate);
		adapter.setFilter(mShowingAll ? null : new DrugFilter());
		nextView.setAdapter(adapter);
		nextView.setOnTouchListener(this);
	}

	private class DrugAdapter extends ArrayAdapter<Drug>
	{
		private ArrayList<Drug> mAllItems;
		private ArrayList<Drug> mItems;
		private Date mAdapterDate;

		public DrugAdapter(Context context, int viewResId, List<Drug> items, Date date)
		{
			super(context, viewResId, items);

			mAllItems = new ArrayList<Drug>(items);
			mAdapterDate = date;
		}

		public void setFilter(CollectionUtils.Filter<Drug> filter)
		{
			if(filter != null)
				mItems = (ArrayList<Drug>) CollectionUtils.filter(mAllItems, filter);
			else
			{
				//mItems = (ArrayList<Drug>) CollectionUtils.copy(mAllItems);
				mItems = mAllItems;
			}

			notifyDataSetChanged();
		}

		@Override
		public Drug getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public int getPosition(Drug drug) {
			return mItems.indexOf(drug);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public View getView(int position, View v, ViewGroup parent)
		{
			// This function currently is the bottleneck when switching between dates, causing
			// laggish animations if there are more than 3 or 4 drugs (i.e. 12-16 DoseViews)
			//
			// All measurements were done using an HTC Desire running Cyanogenmod 7!

			Timer t = null;
			if(LOGV && position == 0)
			{
				Log.v(TAG, "getView: position=0");
				t = new Timer();
			}

			final DoseViewHolder holder;

			if(v == null)
			{
				v = mInflater.inflate(R.layout.drug_view2, null);
				if(LOGV && position == 0) Log.v(TAG, "  0: " + t);

				holder = new DoseViewHolder();

				holder.name = (TextView) v.findViewById(R.id.drug_name);
				holder.icon = (ImageView) v.findViewById(R.id.drug_icon);

				for(int i = 0; i != holder.doseViews.length; ++i)
				{
					final int doseViewId = Constants.DOSE_VIEW_IDS[i];
					holder.doseViews[i] = (DoseView) v.findViewById(doseViewId);
					registerForContextMenu(holder.doseViews[i]);
				}

				v.setTag(holder);

			}
			else
				holder = (DoseViewHolder) v.getTag();

			final Drug drug = getItem(position);
			String drugName = drug.getName();

			// shouldn't normally happen, unless there's a DB problem
			if(drugName == null || drugName.length() == 0)
				drugName = "<???>";

			holder.name.setText(drugName);
			holder.name.setTag(TAG_ID, drug.getId());
			holder.icon.setImageResource(drug.getFormResourceId());

			if(LOGV && position == 0) Log.v(TAG, "  1: " + t);

			// This part often takes more than 90% of the time spent in this function,
			// being rougly 0.025s when hasInfo returns false, and 0.008s when it
			// returns true.
			//
			// Assuming that, in the worst case, all calls to hasInfo return false, this
			// means that this part alone will, in total, take more than 100ms to complete
			// for 4 drugs.

			for(DoseView doseView : holder.doseViews)
			{
				if(!doseView.hasInfo(mAdapterDate, drug))
					doseView.setInfo(mAdapterDate, drug);
			}

			if(LOGV && position == 0) Log.v(TAG, "  2: " + t);

			return v;
		}
	}

	private class DrugFilter implements CollectionUtils.Filter<Drug>
	{
		final boolean mShowDoseless = mSharedPreferences.getBoolean("show_doseless", true);
		final boolean mShowInactive = mSharedPreferences.getBoolean("show_inactive", true);

		@Override
		public boolean matches(Drug drug)
		{
			boolean result = true;

			if(!mShowDoseless && mDate != null)
			{
				if(!drug.hasDoseOnDate(mDate))
					result = false;
			}

			if(!mShowInactive && !drug.isActive())
				result = false;

			if(!result && !Intake.findAll(drug, mDate, null).isEmpty())
				result = true;

			return result;
		}
	}

	private static class DoseViewHolder
	{
		TextView name;
		ImageView icon;
		DoseView[] doseViews = new DoseView[4];
	}
}
