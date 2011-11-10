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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import at.caspase.rxdroid.FractionInputDialog.OnFractionSetListener;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Timer;
import at.caspase.rxdroid.util.Util;

public class DrugListActivity extends Activity implements
	OnLongClickListener, OnDateSetListener, OnSharedPreferenceChangeListener,
	ViewFactory, OnGestureListener, OnTouchListener
{
	public static final String TAG = DrugListActivity.class.getName();

	public static final int MENU_ADD = 0;
	public static final int MENU_PREFERENCES = 1;
	public static final int MENU_TOGGLE_FILTERING = 2;
	
	public static final int CMENU_TOGGLE_INTAKE = 0;
	//public static final int CMENU_CHANGE_DOSE = 1;
	public static final int CMENU_EDIT_DRUG = 2;
	//public static final int CMENU_SHOW_SUPPLY_STATUS = 3;

	public static final String EXTRA_DAY = "day";
	public static final String EXTRA_STARTED_BY_NOTIFICATION = "started_from_notification";

	private static final int TAG_ID = R.id.tag_drug_id;

	private LayoutInflater mInflater;

	private ViewSwitcher mViewSwitcher;
	private GestureDetector mGestureDetector;	
	private TextView mTextDate;

	private Calendar mDate;
	
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
		mTextDate = (TextView) findViewById(R.id.med_list_footer);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());		
		mTextDate.setOnLongClickListener(this);

		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
		
		GlobalContext.set(getApplicationContext());
		Database.load(); // must be called before mViewSwitcher.setFactory!
		
		mViewSwitcher.setFactory(this);
		
		findViewById(R.id.view_switcher_container).setOnTouchListener(this);
		
		mGestureDetector = new GestureDetector(this, this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		final Intent intent = getIntent();
		final String action = intent.getAction();

		if(Intent.ACTION_VIEW.equals(action) || Intent.ACTION_MAIN.equals(action))
		{
			Serializable date = intent.getSerializableExtra(EXTRA_DAY);
			if(!(date instanceof Calendar))
			{
				Log.e(TAG, "onResume: EXTRA_DAY set, but wrong type");
				shiftDate(0);
			}
			else
				setDate((Calendar) date);
		}
		else
			throw new IllegalArgumentException("Received invalid intent; action=" + intent.getAction());
		
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
				intent.setClass(getApplicationContext(), DrugEditActivity.class);
				startActivity(intent);
				return true;
			}
			case MENU_PREFERENCES:
			{
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), PreferencesActivity.class);
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
		final Drug drug = Database.findDrug(doseView.getDrugId());
		final int doseTime = doseView.getDoseTime();
		
		//menu.setHeaderIcon(android.R.drawable.ic_menu_agenda);
		menu.setHeaderTitle(drug.getName());
		
		final int intakeStatus = doseView.getIntakeStatus();
		final int toggleMessageId;
		
		if(intakeStatus == DoseView.STATUS_TAKEN)
			toggleMessageId = R.string._title_mark_not_taken;
		else
			toggleMessageId = R.string._title_mark_taken;
		
		//////////////////////////////////////////////////
		menu.add(0, CMENU_TOGGLE_INTAKE, 0, toggleMessageId).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				if(intakeStatus != DoseView.STATUS_TAKEN)
					requestIntake(drug, mDate, doseTime, doseView.getDose(), true);
				else
				{
					Fraction dose = new Fraction();
					
					for(Intake intake : Database.findIntakes(drug, mDate, doseTime))
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
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		//if(resultCode == RESULT_OK)
		//    updateAdapter();
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
		
		Drug drug = Database.findDrug((Integer) view.getTag(TAG_ID));
		intent.putExtra(DrugEditActivity.EXTRA_DRUG, (Serializable) drug);
		
		startActivityForResult(intent, 0);
	}

	@Override
	public boolean onLongClick(View view)
	{
		if(view.getId() == R.id.med_list_footer)
		{
			final int year = mDate.get(Calendar.YEAR);
			final int month = mDate.get(Calendar.MONTH);
			final int day = mDate.get(Calendar.DAY_OF_MONTH);
			
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
		final Drug drug = Database.findDrug(v.getDrugId());

		final int doseTime = v.getDoseTime();
		final Fraction dose = drug.getDose(doseTime);
		
		requestIntake(drug, mDate, doseTime, dose, true);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) 
	{
		// causes the ListView to be refreshed
		setDate(mDate);
	}

	@Override
	public View makeView() {
		return new ListView(this);
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
		super.onTouchEvent(event);
		return mGestureDetector.onTouchEvent(event);
	}
	
	/////////////

	private void startNotificationService()
	{
		Intent serviceIntent = new Intent();
		serviceIntent.setClass(this, NotificationService2.class);
		
		startService(serviceIntent);
	}

	private void setDate(Calendar newDate) {
		setOrShiftDate(0, newDate);
	}

	private void shiftDate(int shiftBy) {
		setOrShiftDate(shiftBy, null);
	}

	// shift to previous (-1) or next(1) date. passing 0
	// will reset to specified date, or current date
	// if newDate is -1
	private void setOrShiftDate(int shiftBy, Calendar newDate)
	{
		setProgressBarIndeterminateVisibility(true);

		if(shiftBy == 0)
		{
			if(newDate == null)
				mDate = DateTime.today();
			else if(mDate != newDate)
				mDate = newDate;

			mViewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
			mViewSwitcher.setOutAnimation(null);
		}
		else
		{			
			mDate.add(Calendar.DAY_OF_MONTH, shiftBy);

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
								
		final SpannableString dateString = new SpannableString(DateFormat.getDateFormat(this).format(mDate.getTime()));

		if(mDate.equals(DateTime.today()))
			dateString.setSpan(new UnderlineSpan(), 0, dateString.length(), 0);

		mTextDate.setText(dateString);
		//setTitle(getString(R.string.app_name) + " - " + dateString.toString()); 
		
		// update the intent so our Activity is restarted with the last opened date
		setIntent(getIntent().putExtra(EXTRA_DAY, (Serializable) mDate));

		setProgressBarIndeterminateVisibility(false);
	}
	
	private void updateNextView()
	{
		final ListView nextView = (ListView) mViewSwitcher.getNextView();
		
		final DrugAdapter adapter = new DrugAdapter(this, R.layout.dose_view, Database.getDrugs(), mDate);
		adapter.setFilter(mShowingAll ? null : new DrugFilter());
		nextView.setAdapter(adapter);
		nextView.setOnTouchListener(this);
	}
	
	private void requestIntake(final Drug drug, Calendar date, int doseTime, Fraction dose, boolean askOnNormalIntake)
	{
		if(dose.equals(Fraction.ZERO))
		{
			requestUnscheduledIntake(drug, date, doseTime);
			return;
		}
		
		final Intake intake = new Intake(drug, DateTime.toSqlDate(mDate), doseTime, dose);
		final Fraction newSupply = drug.getCurrentSupply().minus(dose);
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(drug.getName() + ": " + dose);
		
		//////////////////
		final OnClickListener defaultOnClickListener = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Database.create(intake, OnDatabaseChangedListener.FLAG_IGNORE);
				Database.update(drug);

				Toast.makeText(getApplicationContext(), R.string._toast_intake_noted, Toast.LENGTH_SHORT).show();
			}
		};
		//////////////////
		
		if(newSupply.compareTo(0) == -1)
		{
			drug.setCurrentSupply(Fraction.ZERO);
			
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage("According to the database the current supplies are not sufficient for this dose.");
			builder.setPositiveButton(R.string._btn_ignore, defaultOnClickListener);
			//////////////////
			builder.setNeutralButton(R.string._btn_edit_drug, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Intent intent = new Intent(Intent.ACTION_EDIT);
                    intent.setClass(getApplicationContext(), DrugEditActivity.class);
                    intent.putExtra(DrugEditActivity.EXTRA_DRUG, (Serializable) drug);
                    intent.putExtra(DrugEditActivity.EXTRA_FOCUS_ON_CURRENT_SUPPLY, true);

                    startActivityForResult(intent, 0);		
				}
			});
			//////////////////
		}
		else
		{
			drug.setCurrentSupply(newSupply);
			
			builder.setIcon(Util.getDoseTimeDrawableFromDoseTime(doseTime));
			
			final boolean hasIntakes = !Database.findIntakes(drug, date, doseTime).isEmpty();
			if(!hasIntakes)
			{
				if(askOnNormalIntake)
				{				
					//builder.setMessage("Take the above mentioned dose now and press OK.");
					builder.setMessage(R.string._msg_intake_normal);
	                builder.setPositiveButton(android.R.string.ok, defaultOnClickListener);
	                builder.setNegativeButton(android.R.string.cancel, null);
				}
				else
				{
					// we pretend the user clicked the "OK" button
					defaultOnClickListener.onClick(null, Dialog.BUTTON_POSITIVE);
					return;
				}
			}
			else
			{
				//builder.setMessage("You have already taken this dose. Do you want to take it regardless?");
				builder.setMessage(R.string._msg_intake_already_taken);
                builder.setPositiveButton(android.R.string.yes, defaultOnClickListener);
                builder.setNegativeButton(android.R.string.no, null);
			}			
		}
		
		builder.show();
	}	
	
	private void requestUnscheduledIntake(final Drug drug, final Calendar date, final int doseTime)
	{
		final FractionInputDialog dialog = new FractionInputDialog(this, Fraction.ZERO, null);
		dialog.setKeypadEnabled(false);
		dialog.setTitle(drug.getName());
		//dialog.setIcon(Util.getDoseTimeDrawableFromDoseTime(doseTime));
		dialog.setIcon(android.R.drawable.ic_dialog_info);
		dialog.setMessage(getString(R.string._msg_intake_unscheduled));
		//dialog.setMessage("No dose is scheduled at this time - choose one now.");
		//////////////////	
		dialog.setOnFractionSetListener(new OnFractionSetListener() {
			
			@Override
			public void onFractionSet(FractionInputDialog dialog, Fraction value)
			{
				Log.d(TAG, "requestUnscheduledIntake$onFractionSet");
				requestIntake(drug, date, doseTime, value, false);				
			}
		});
		//////////////////
		dialog.show();
	}

	private class DrugAdapter extends ArrayAdapter<Drug>
	{		
		private ArrayList<Drug> mAllItems;
		private ArrayList<Drug> mItems;
		private Calendar mAdapterDate;
		
		public DrugAdapter(Context context, int viewResId, List<Drug> items, Calendar date) 
		{
			super(context, viewResId, items);
			
			mAllItems = new ArrayList<Drug>(items);
			mAdapterDate = (Calendar) date.clone();
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
			
			final DoseViewHolder holder;

			if(v == null)
			{
				v = mInflater.inflate(R.layout.drug_view2, null);
				
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
			
			holder.name.setText(drug.getName());
			holder.name.setTag(TAG_ID, drug.getId());
			holder.icon.setImageResource(drug.getFormResourceId());			
						
			// This part often takes more than 90% of the time spent in this function,
			// being rougly 0.025s when hasInfo returns false, and 0.008s when it
			// returns true.
			//
			// Assuming that, in the worst case, all calls to hasInfo return false, this
			// means that this part alone will, in total, take more than 100ms to complete 
			// for 4 drugs.
			
			Timer t = new Timer();
			for(DoseView doseView : holder.doseViews)
			{
				if(!doseView.hasInfo(mAdapterDate, drug))
					doseView.setInfo(mAdapterDate, drug);
			}
			
			Log.d(TAG, "getView: " + t);
			
			return v;
		}		
	}

	private class DrugFilter implements CollectionUtils.Filter<Drug>
	{		
		@Override
		public boolean matches(Drug drug)
		{		
			final boolean showDoseless = mSharedPreferences.getBoolean("show_doseless", true);
			final boolean showInactive = mSharedPreferences.getBoolean("show_inactive", true);
			
			if((!showDoseless && mDate != null && !drug.hasDoseOnDate(mDate)) || (!showInactive && !drug.isActive()))
				return false;
						
			return true;
		}
	}
	
	private static class DoseViewHolder
	{
		TextView name;
		ImageView icon;
		DoseView[] doseViews = new DoseView[4];			
	}
}
