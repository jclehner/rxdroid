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
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnLongClickListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.ViewSwitcher.ViewFactory;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;
import at.caspase.rxdroid.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Timer;
import at.caspase.rxdroid.util.Util;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;

public class DrugListActivity extends OrmLiteBaseActivity<Database.Helper> implements
	OnDatabaseChangedListener, OnLongClickListener, OnDateSetListener, OnSharedPreferenceChangeListener, ViewFactory
{
	public static final String TAG = DrugListActivity.class.getName();

	public static final int MENU_ADD = Menu.FIRST;
	public static final int MENU_DELETE = MENU_ADD + 1;
	public static final int MENU_PREFERENCES = MENU_ADD + 2;
	public static final int MENU_DEBUG_FILL = MENU_ADD + 3;

	public static final String EXTRA_DAY = "day";

	private static final int TAG_ID = R.id.tag_drug_id;

	private LayoutInflater mInflater;
	
	private ViewSwitcher mViewSwitcher;
	private DrugAdapter mAdapter;
		
	private TextView mTextDate;
	
	private Date mDate;

	private Dao<Database.Drug, Integer> mDao;
	private Dao<Database.Intake, Integer> mIntakeDao;

	private SharedPreferences mSharedPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.drug_list);
		
		mInflater = LayoutInflater.from(this);
		
		mDao = getHelper().getDrugDao();
		mIntakeDao = getHelper().getIntakeDao();
		mViewSwitcher = (ViewSwitcher) findViewById(R.id.drug_list_view_flipper);
		mTextDate = (TextView) findViewById(R.id.med_list_footer);
		
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		mViewSwitcher.setFactory(this);
		mTextDate.setOnLongClickListener(this);		

		mAdapter = makeAdapter();
		
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
		Database.registerOnChangedListener(this);
		Preferences.setContext(getApplicationContext());
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		final Intent intent = getIntent();
		final String action = intent.getAction();

		if(Intent.ACTION_VIEW.equals(action) || Intent.ACTION_MAIN.equals(action))
		{
			//mViewSwitcher.removeAllViews();
			//mAdapter = makeAdapter();
						
			setDate((Date) intent.getSerializableExtra(EXTRA_DAY));			
		}
		else
			throw new IllegalArgumentException("Received invalid intent; action=" + intent.getAction());
		
		if(!NotificationService.isRunning())
		{
			startNotificationService();
			Log.w(TAG, "onResume: Notification service was not running");
			
			if(!mSharedPreferences.getBoolean("debug_enabled", false))
			{			
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string._title_warning);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setMessage(R.string._msg_warning_svc_not_running);
				builder.setPositiveButton(android.R.string.ok, null);
				builder.show();
			}
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		Database.unregisterOnChangedListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_ADD, 0, "Add").setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_PREFERENCES, 0, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);

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
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		//if(resultCode == RESULT_OK)
		//	updateAdapter();
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
				throw new IllegalArgumentException("Unhandled view " + view.getClass().getSimpleName() + ", id=" + view.getId());
		}
		
		setProgressBarIndeterminateVisibility(false);
	}

	public void onDrugNameClick(View view)
	{
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setClass(this, DrugEditActivity.class);

		try
		{
			Database.Drug drug = mDao.queryForId((Integer) view.getTag(TAG_ID));
			intent.putExtra(DrugEditActivity.EXTRA_DRUG, (Serializable) drug);
		}
		catch(SQLException e)
		{
			throw new RuntimeException(e);
		}

		startActivityForResult(intent, 0);
	}

	@Override
	public boolean onLongClick(View view)
	{
		if(view.getId() == R.id.med_list_footer)
		{
			DatePickerDialog dialog = new DatePickerDialog(this, this, mDate.getYear() + 1900, mDate.getMonth(), mDate.getDate());
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
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final Database.Drug drug;

		try
		{
			drug = mDao.queryForId(v.getDrugId());
		}
		catch(SQLException e)
		{
			throw new RuntimeException(e);
		}

		final int doseTime = v.getDoseTime();
		final Fraction dose = drug.getDose(doseTime);
		final Fraction newSupply = drug.getCurrentSupply().minus(dose);

		final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				if(which == AlertDialog.BUTTON_POSITIVE)
				{
					final Database.Intake intake = new Database.Intake(drug, mDate, doseTime);

					if(newSupply.compareTo(0) != -1)
						drug.setCurrentSupply(newSupply);
					else
						drug.setCurrentSupply(Fraction.ZERO);

					Database.create(mIntakeDao, intake);
					Database.update(mDao, drug);

					Toast.makeText(getApplicationContext(), "Dose intake noted.", Toast.LENGTH_SHORT).show();
				}
				else if(which == AlertDialog.BUTTON_NEUTRAL)
				{
					Intent intent = new Intent(Intent.ACTION_EDIT);
					intent.setClass(getBaseContext(), DrugEditActivity.class);
					intent.putExtra(DrugEditActivity.EXTRA_DRUG, (Serializable) drug);
					intent.putExtra(DrugEditActivity.EXTRA_FOCUS_ON_CURRENT_SUPPLY, true);

					startActivityForResult(intent, 0);
				}
			}
		};


		if(newSupply.compareTo(0) == -1 && drug.getRefillSize() != 0)
		{
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(drug.getName());
			builder.setMessage("According to the database, the current supplies are not sufficient for this dose!");
			builder.setPositiveButton("Ignore", onClickListener);
			builder.setNeutralButton("Edit drug", onClickListener);

			builder.show();

			return;
		}

		if(!dose.equals(Fraction.ZERO))
		{
			builder.setTitle(drug.getName() + ": " + drug.getDose(doseTime));

			boolean hasIntake = Database.findIntakes(mIntakeDao, drug, mDate, doseTime).size() != 0;

			if(!hasIntake)
			{
				builder.setMessage("Take the above mentioned dose now and press OK.");
				builder.setPositiveButton("OK", onClickListener);
				builder.setNegativeButton("Cancel", null);
			}
			else
			{
				builder.setMessage("You have already taken the above mentioned dose. Do you want to take it regardless?");
				builder.setPositiveButton("Yes", onClickListener);
				builder.setNegativeButton("No", null);
			}
		}
		else
		{
			builder.setTitle(drug.getName());
			builder.setMessage("No intake is scheduled at this time. Do you still want to take a dose?");
			builder.setPositiveButton("Yes", onClickListener);
			builder.setNegativeButton("No", null);

			// TODO we should ask the user how much he wants to take
		}

		builder.setIcon(Util.getDoseTimeDrawableFromDoseViewId(view.getId()));
		builder.show();
	}

	@Override
	public void onCreateEntry(Drug drug) {
		mAdapter.add(drug);
	}

	@Override
	public void onDeleteEntry(Drug drug) {
		mAdapter.remove(drug);
	}

	@Override
	public void onUpdateEntry(Drug drug) {
		mAdapter.update(drug);
	}

	@Override
	public void onCreateEntry(Intake intake) {}

	@Override
	public void onDeleteEntry(Intake intake) {}

	@Override
	public void onDatabaseDropped() {}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		setDate(mDate);
	}
	
	@Override
	public View makeView() {
		return new ListView(this);
	}
		
	private DrugAdapter makeAdapter()
	{
		List<Drug> drugs;
		
		try
		{
			drugs = mDao.queryForAll();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
		
		return new DrugAdapter(this, R.layout.dose_view, drugs);
	}
	
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
	private void setOrShiftDate(long shiftBy, Date newDate)
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
			final long shiftedTime = mDate.getTime() + shiftBy * Constants.MILLIS_PER_DAY;
			mDate.setTime(shiftedTime);

			Timer t = new Timer();

			if(shiftBy == 1)
			{
				mViewSwitcher.getInAnimation().setInterpolator(ReverseInterpolator.INSTANCE);
				mViewSwitcher.getOutAnimation().setInterpolator(ReverseInterpolator.INSTANCE);
			}
			else if(shiftBy == -1)
			{
				mViewSwitcher.getInAnimation().setInterpolator(null);
				mViewSwitcher.getOutAnimation().setInterpolator(null);
			}
			else
				throw new IllegalArgumentException();

			Log.d(TAG, "Setting interpolators took " + t + " with shiftBy=" + shiftBy);
		}

		mViewSwitcher.showNext();
		((ListView) mViewSwitcher.getCurrentView()).setAdapter(mAdapter);
		
		final SpannableString dateString = new SpannableString(mDate.toString());

		if(mDate.equals(DateTime.today()))
			dateString.setSpan(new UnderlineSpan(), 0, dateString.length(), 0);

		mTextDate.setText(dateString);

		// update the intent so our Activity is restarted with the last opened date
		setIntent(getIntent().putExtra(EXTRA_DAY, (Serializable) mDate));

		if(shiftBy == 0)
		{
			mViewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
			mViewSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
		}
		
		setProgressBarIndeterminateVisibility(false);
	}
	
	private class DrugAdapter extends ArrayAdapter<Database.Drug>
	{
		public DrugAdapter(Context context, int textViewResId, List<Database.Drug> items)
		{
			super(context, textViewResId, items);
			setNotifyOnChange(true);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = convertView;

			if(v == null)
				v = mInflater.inflate(R.layout.drug_view2, null);
			
			final Drug drug = getItem(position);

			final TextView drugName = (TextView) v.findViewById(R.id.drug_name);
			drugName.setText(drug.getName());
			drugName.setTag(TAG_ID, drug.getId());

			final ImageView drugIcon = (ImageView) v.findViewById(R.id.drug_icon);
			drugIcon.setImageResource(drug.getFormResourceId());

			final int doseViewIds[] = { R.id.morning, R.id.noon, R.id.evening, R.id.night };
			for(int doseViewId : doseViewIds)
			{
				DoseView doseView = (DoseView) v.findViewById(doseViewId);
				doseView.setInfo(mIntakeDao, mDate, drug);
			}
			
			return v;
		}
		
		/**
		 * Update a drug, based on its ID.
		 *  
		 * @param drug the drug to update. If there's an item in the adapter data
		 * 	with a matching ID, it will be replaced with this one.
		 */
		public void update(Drug drug)
		{
			int i = 0;
			
			for(; i != getCount(); ++i)
			{
				Drug d = getItem(i);
				
				if(d.getId() == drug.getId())
				{
					remove(d);
					insert(drug, i);
					return;
				}	
			}
			
			if(i == getCount())
				throw new NoSuchElementException("No such drug in adapter data: " + drug);
		}
	}

	private enum ReverseInterpolator implements Interpolator
	{
		INSTANCE;

		@Override
		public float getInterpolation(float f) {
			return Math.abs(f - 1f);
		}
	}
}
