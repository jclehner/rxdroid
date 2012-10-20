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

package at.caspase.rxdroid;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import at.caspase.androidutils.MyDialogPreference;
import at.caspase.androidutils.otpm.CheckboxPreferenceHelper;
import at.caspase.androidutils.otpm.ListPreferenceWithIntHelper;
import at.caspase.androidutils.otpm.MyDialogPreferenceHelper;
import at.caspase.androidutils.otpm.OTPM;
import at.caspase.androidutils.otpm.PreferenceHelper;
import at.caspase.androidutils.otpm.OTPM.AddPreference;
import at.caspase.androidutils.otpm.OTPM.CreatePreference;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.db.Schedule;
import at.caspase.rxdroid.preferences.DosePreference;
import at.caspase.rxdroid.preferences.DrugNamePreference2;
import at.caspase.rxdroid.preferences.FractionPreference;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.SimpleBitSet;
import at.caspase.rxdroid.util.Util;

/**
 * Edit a drug's database entry.
 * @author Joseph Lehner
 *
 */

@SuppressWarnings("deprecation")
public class DrugEditActivity extends PreferenceActivity implements OnPreferenceClickListener
{
	public static final String EXTRA_DRUG = "drug";
	public static final String EXTRA_FOCUS_ON_CURRENT_SUPPLY = "focus_on_current_supply";

	private static final String ARG_DRUG = "drug";

	private static final String TAG = DrugEditActivity.class.getName();
	private static final boolean LOGV = false;

	private DrugWrapper mWrapper;
	private int mDrugHash;

	// if true, we're editing an existing drug; if false, we're adding a new one
	private boolean mIsEditing;

	private boolean mFocusOnCurrentSupply = false;

	@Override
	public void onBackPressed()
	{
		final Intent intent = getIntent();
		final String action = intent.getAction();

		final Drug drug = mWrapper.get();
		final String drugName = drug.getName();

		if(drugName == null || drugName.length() == 0)
		{
			showDialog(R.id.drug_discard_dialog);
			return;
		}

		if(Intent.ACTION_EDIT.equals(action))
		{
			if(mDrugHash != drug.hashCode())
			{
				if(LOGV) Util.dumpObjectMembers(TAG, Log.VERBOSE, drug, "drug 2");

				Bundle args = new Bundle();
				args.putSerializable(ARG_DRUG, drug);

				showDialog(R.id.drug_save_changes_dialog, args);
				return;
			}
		}
		else if(Intent.ACTION_INSERT.equals(action))
		{
			Database.create(drug, 0);
			Toast.makeText(getApplicationContext(), getString(R.string._toast_saved), Toast.LENGTH_SHORT).show();
		}

		finish();
	}

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		if(preference.getKey().equals("delete"))
		{
			showDialog(R.id.drug_delete_dialog);

			return true;
		}

		return false;
	}

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setTheme(Theme.get());
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.empty);

		if(!Version.SDK_IS_PRE_HONEYCOMB)
		{
			final ActionBar ab = getActionBar();
			ab.setDisplayShowHomeEnabled(true);
			ab.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		Database.init();
		RxDroid.setIsVisible(this, true);

		Intent intent = getIntent();
		String action = intent.getAction();

		Drug drug = null;

		mWrapper = new DrugWrapper(this);
		mFocusOnCurrentSupply = false;

		if(Intent.ACTION_EDIT.equals(action))
		{
			Serializable extra = intent.getSerializableExtra(EXTRA_DRUG);
			if(extra == null)
				throw new IllegalStateException("ACTION_EDIT requires EXTRA_DRUG");

			drug = (Drug) extra;

			if(LOGV) Util.dumpObjectMembers(TAG, Log.VERBOSE, drug, "drug");

			mWrapper.set(drug);
			mDrugHash = drug.hashCode();
			mIsEditing = true;

			if(intent.getBooleanExtra(EXTRA_FOCUS_ON_CURRENT_SUPPLY, false))
				mFocusOnCurrentSupply = true;

			setTitle(drug.getName());
		}
		else if(Intent.ACTION_INSERT.equals(action))
		{
			mIsEditing = false;
			mWrapper.set(new Drug());
		}
		else
			throw new IllegalArgumentException("Unhandled action " + action);


		OTPM.mapToPreferenceHierarchy(getPreferenceScreen(), mWrapper);
		getPreferenceScreen().setOnPreferenceChangeListener(mListener);

		Preference deletePref = findPreference("delete");
		if(deletePref != null)
		{
			if(mIsEditing)
				deletePref.setOnPreferenceClickListener(this);
			else if(deletePref != null)
				getPreferenceScreen().removePreference(deletePref);
		}

		if(mFocusOnCurrentSupply)
		{
			Log.i(TAG, "Will focus on current supply preference");

			final PreferenceScreen ps = getPreferenceScreen();
			final Preference p = ps.findPreference("currentSupply");


			if(p != null)
			{
				final int index = p.getOrder();
				ps.onItemClick(null, null, index, 0);
			}
			else
				Log.w(TAG, "Couldn't focus on current supply preference");
		}
	}

	@Override
	protected void onPause()
	{
		super.onStop();
		RxDroid.setIsVisible(this, false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(!Version.SDK_IS_PRE_HONEYCOMB)
		{
			if(item.getItemId() == android.R.id.home)
			{
				// We can do this since this Activity can only be launched from
				// DrugListActivity at the moment.
				onBackPressed();
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		// This activity will not be restarted when the screen orientation changes, otherwise
		// the OTPM stuff in onCreate() would reinitialize the Preferences in the hierarchy,
		// thus not restoring their original state.
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return onCreateDialog(id, null);
	}

	@Override
	protected Dialog onCreateDialog(int id, final Bundle args)
	{
		if(id == R.id.drug_delete_dialog)
		{
			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setIcon(android.R.drawable.ic_dialog_alert);
			ab.setTitle(getString(R.string._title_delete_drug, mWrapper.get().getName()));
			ab.setMessage(R.string._msg_delete_drug);

			ab.setPositiveButton(android.R.string.yes, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Database.delete(mWrapper.get());
					Toast.makeText(getApplicationContext(), R.string._toast_deleted, Toast.LENGTH_SHORT).show();
					finish();
				}
			});

			ab.setNegativeButton(android.R.string.no, null);
			return ab.create();
		}
		else if(id == R.id.drug_discard_dialog)
		{
			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			//builder.setTitle(R.string._title_warning);
			ab.setIcon(android.R.drawable.ic_dialog_alert);
			ab.setTitle(R.string._msg_err_empty_drug_name);
			ab.setNegativeButton(android.R.string.cancel, null);
			ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					finish();
				}
			});

			return ab.create();
		}
		else if(id == R.id.drug_save_changes_dialog)
		{
			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setTitle(R.string._title_save_chanes);
			ab.setIcon(android.R.drawable.ic_dialog_info);
			ab.setMessage(R.string._msg_save_drug_changes);

			final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if(which == DialogInterface.BUTTON_POSITIVE)
					{
						Database.update((Drug) args.getSerializable(ARG_DRUG));
						Toast.makeText(getApplicationContext(), R.string._toast_saved, Toast.LENGTH_SHORT).show();
					}

					finish();
				}
			};

			ab.setPositiveButton(R.string._btn_save, onClickListener);
			ab.setNegativeButton(R.string._btn_discard, onClickListener);

			return ab.create();
		}

		return super.onCreateDialog(id, args);
	}

	private static class DrugWrapper
	{
		@CreatePreference
		(
			titleResId = R.string._title_drug_name,
			order = 1,
			type = DrugNamePreference2.class,
			helper = MyDialogPreferenceHelper.class
		)
		private String name;

		@AddPreference(order = 2)
		private final Preference mDeletePreference;

		@CreatePreference
		(
			titleResId = R.string._title_morning,
			key = "morning",
			categoryResId = R.string._title_intake_schedule,
			order = 3,
			type = DosePreference.class,
			helper = MyDialogPreferenceHelper.class
		)
		private Fraction doseMorning;

		@CreatePreference
		(
			titleResId = R.string._title_noon,
			key = "noon",
			order = 4,
			type = DosePreference.class,
			helper = MyDialogPreferenceHelper.class
		)
		private Fraction doseNoon;

		@CreatePreference
		(
			titleResId = R.string._title_evening,
			key = "evening",
			order = 5,
			type = DosePreference.class,
			helper = MyDialogPreferenceHelper.class
		)
		private Fraction doseEvening;

		@CreatePreference
		(
			titleResId = R.string._title_night,
			key = "night",
			endActiveCategory = true,
			order = 6,
			type = DosePreference.class,
			helper = MyDialogPreferenceHelper.class
		)
		private Fraction doseNight;

		@CreatePreference
		(
			titleResId = R.string._title_repeat,
			order = 7,
			type = ListPreference.class,
			helper = RepeatModePreferenceHelper.class,
			fieldDependencies = { "repeatArg", "repeatOrigin" }
		)
		private int repeat;

		@CreatePreference
		(
			titleResId = R.string._title_icon,
			categoryResId = R.string._title_misc,
			order = 8,
			type = ListPreference.class,
			helper = FormPreferenceHelper.class
		)
		private int form;

		@CreatePreference
		(
			titleResId = R.string._title_refill_size,
			order = 10,
			type = FractionPreference.class,
			helper = FractionAsIntegerPreferenceHelper.class
		)
		private int refillSize;

		@CreatePreference
		(
			titleResId = R.string._title_current_supply,
			order = 11,
			type = CurrentSupplyPreference.class,
			helper = CurrentSupplyPreferenceHelper.class,
			reverseDependencies = { "morning", "noon", "evening", "night", "refillSize", "repeat"},
			fieldDependencies = { "repeatArg", "repeatOrigin" }
		)
		private Fraction currentSupply;

//		@CreatePreference
//		(
//			titleResId = R.string._title_supply_monitor,
//			summaryResId = R.string._summary_supply_monitor,
//			order = 12,
//			type = CheckBoxPreference.class,
//			helper = CheckboxPreferenceHelper.class
//		)

		@CreatePreference
		(
			titleResId = R.string._title_prefscreen_notifications,
			order = 12,
			type = ListPreference.class,
			helper = NotificationsPreferenceHelper.class
		)
		private boolean autoAddIntakes;

		@CreatePreference
		(
			titleResId = R.string._title_active,
			summary = "",
			order = 13,
			type = CheckBoxPreference.class,
			helper = CheckboxPreferenceHelper.class
		)
		private boolean active;

		private int id;

		private long repeatArg;
		private Date repeatOrigin;
		private int sortRank;
		private Schedule schedule;
		private String comment;

		/*@CreatePreference
		(
			title = "lastAutoIntakeCreationDate",
			order = 14,
			type = Preference.class,
			helper = ReadonlyPreferenceHelper.class,
			reverseDependencies = "autoAddIntakes"
		)*/
		private Date lastAutoIntakeCreationDate;

		public DrugWrapper(Context context)
		{
			mDeletePreference = new Preference(context);
			mDeletePreference.setKey("delete");
			mDeletePreference.setTitle(context.getString(R.string._title_delete));
		}

		public void set(Drug drug)
		{
			id = drug.getId();
			active = drug.isActive();
			comment = drug.getComment();
			currentSupply = drug.getCurrentSupply();
			doseMorning = drug.getDose(Drug.TIME_MORNING);
			doseNoon = drug.getDose(Drug.TIME_NOON);
			doseEvening = drug.getDose(Drug.TIME_EVENING);
			doseNight = drug.getDose(Drug.TIME_NIGHT);
			refillSize = drug.getRefillSize();
			repeat = drug.getRepeatMode();
			repeatArg = drug.getRepeatArg();
			repeatOrigin = drug.getRepeatOrigin();
			schedule = drug.getSchedule();
			sortRank = drug.getSortRank();
			autoAddIntakes = drug.isAutoAddIntakesEnabled();
			lastAutoIntakeCreationDate = drug.getLastAutoIntakeCreationDate();

			name = drug.getName();
			form = drug.getIcon();

			if(LOGV) Log.v(TAG, "DrugWrapper.set: repeatOrigin=" + repeatOrigin);
		}

		public Drug get()
		{
			Drug drug = new Drug();
			drug.setId(id);
			drug.setName(name);
			drug.setForm(form);
			drug.setActive(active);
			drug.setComment(comment);
			drug.setCurrentSupply(currentSupply);
			drug.setRefillSize(refillSize);
			drug.setRepeatMode(repeat);
			drug.setSortRank(sortRank);
			drug.setSchedule(schedule);
			drug.setLastAutoIntakeCreationDate(lastAutoIntakeCreationDate);
			drug.setAutoAddIntakesEnabled(autoAddIntakes);

			final Fraction doses[] = { doseMorning, doseNoon, doseEvening, doseNight };

			for(int i = 0; i != doses.length; ++i)
				drug.setDose(Constants.DOSE_TIMES[i], doses[i]);

			drug.setRepeatArg(repeatArg);
			drug.setRepeatOrigin(repeatOrigin);

			if(LOGV) Log.v(TAG, "DrugWrapper.get: repeatOrigin=" + repeatOrigin);

			return drug;
		}
	}

	private static class RepeatModePreferenceHelper extends ListPreferenceWithIntHelper
	{
		private ListPreference mPref;
		private Context mContext;

		@SuppressWarnings("unused")
		public RepeatModePreferenceHelper() {
			super(R.array.drug_repeat);
		}

		@Override
		public void initPreference(ListPreference preference, Integer fieldValue)
		{
			super.initPreference(preference, fieldValue);

			mPref = preference;
			mContext = preference.getContext();

			//preference.setDependency("currentSupply");

			updateSummary();
		}

		@Override
		public Integer toFieldType(Object prefValue) {
			return Integer.valueOf((String) prefValue, 10);
		}

		@Override
		public boolean updatePreference(ListPreference preference, Integer newValue)
		{
			Log.v(TAG, "RepeatModePreferenceHelper.updatePreference: ");

			switch(newValue)
			{
				case Drug.REPEAT_EVERY_N_DAYS:
					handleEveryNDaysRepeatMode();
					return false;

				case Drug.REPEAT_WEEKDAYS:
					handleWeekdaysRepeatMode();
					return false;

				case Drug.REPEAT_21_7:
					handle21_7RepeatMode();
					return false;

				case Drug.REPEAT_DAILY:
				case Drug.REPEAT_ON_DEMAND:
					return super.updatePreference(preference, newValue);

				default:
					Toast.makeText(mContext, "Not implemented", Toast.LENGTH_LONG).show();
					return false;
			}
		}

		@Override
		public void updateSummary(ListPreference preference, Integer newValue)
		{
			switch(newValue)
			{
				case Drug.REPEAT_DAILY:
					preference.setSummary(R.string._title_daily);
					break;

				case Drug.REPEAT_ON_DEMAND:
					preference.setSummary(R.string._title_on_demand);
					break;

				case Drug.REPEAT_EVERY_N_DAYS:
				case Drug.REPEAT_WEEKDAYS:
				case Drug.REPEAT_21_7:
					// summary is updated from the handle<repeat mode>() functions
					break;

				default:
					super.updateSummary(preference, newValue);
			}
		}

		private void handleEveryNDaysRepeatMode()
		{
			final Date repeatOrigin;
			final long repeatArg;

			int oldRepeatMode = getFieldValue();

			if(oldRepeatMode != Drug.REPEAT_EVERY_N_DAYS)
			{
				repeatOrigin = DateTime.today();
				repeatArg = 2;
			}
			else
			{
				repeatOrigin = (Date) getFieldValue("repeatOrigin");
				repeatArg = (Long) getFieldValue("repeatArg");
			}

			final EditText editText = new EditText(mContext);
			editText.setText(Long.toString(repeatArg));
			editText.setEms(20);
			editText.setMaxLines(1);
			editText.setInputType(InputType.TYPE_CLASS_NUMBER);
			editText.setSelectAllOnFocus(true);

			final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(R.string._title_every_n_days);
			builder.setMessage(R.string._msg_every_n_days_distance);
			builder.setView(editText);
			builder.setCancelable(true);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					final long repeatArg = Long.valueOf(editText.getText().toString());
					showRepeatOriginDateDialog(Drug.REPEAT_EVERY_N_DAYS, repeatOrigin, repeatArg);
				}
			});

			final AlertDialog dialog = builder.create();

			editText.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void afterTextChanged(Editable s)
				{
					final long value;

					if(s.length() == 0)
						value = 0;
					else
						value = Long.valueOf(s.toString());

					final boolean enabled = value >= 2;
					dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);

					if(!enabled)
						editText.setError(mContext.getString(R.string._msg_drug_repeat_ge_2));
					else
						editText.setError(null);
				}
			});

			dialog.show();
			editText.performClick();
		}

		private void handleWeekdaysRepeatMode()
		{
			if(getFieldValue() != Drug.REPEAT_WEEKDAYS)
			{
				setFieldValue("repeatArg", 0);
				setFieldValue("repeatOrigin", DateTime.today());
			}

			long repeatArg = (Long) getFieldValue("repeatArg");
			final boolean[] checkedItems = SimpleBitSet.toBooleanArray(repeatArg, Constants.LONG_WEEK_DAY_NAMES.length);

			if(repeatArg == 0)
			{
				// check the current weekday if none is selected
				final int weekday = DateTime.nowCalendarMutable().get(Calendar.DAY_OF_WEEK);
				final int index = CollectionUtils.indexOf(weekday, Constants.WEEK_DAYS);
				checkedItems[index] = true;
				repeatArg |= 1 << index;
			}

			final SimpleBitSet bitset = new SimpleBitSet(repeatArg);

			final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(R.string._title_weekdays);
			builder.setMultiChoiceItems(Constants.LONG_WEEK_DAY_NAMES, checkedItems, new OnMultiChoiceClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked)
				{
					bitset.set(which, isChecked);

					final Button positiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
					positiveButton.setEnabled(bitset.longValue() != 0);
				}
			});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					setFieldValue(Drug.REPEAT_WEEKDAYS);
					setFieldValue("repeatArg", bitset.longValue());

					updateSummary();
					notifyForwardDependencies();
				}
			});

			builder.show();
		}

		private void handle21_7RepeatMode()
		{
			if(getFieldValue() != Drug.REPEAT_21_7)
				setFieldValue("repeatOrigin", DateTime.today());

			showRepeatOriginDateDialog(Drug.REPEAT_21_7, (Date) getFieldValue("repeatOrigin"), 0);
		}

		private void showRepeatOriginDateDialog(final int repeatMode, Date repeatOrigin, final long repeatArg)
		{
			final OnDateSetListener onDateSetListener = new OnDateSetListener() {

				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
				{
					final Date newRepeatOrigin = DateTime.date(year, monthOfYear, dayOfMonth);

					setFieldValue(repeatMode);
					setFieldValue("repeatOrigin", newRepeatOrigin);
					setFieldValue("repeatArg", repeatArg);

					updateSummary();
					notifyForwardDependencies();
				}
			};



			final Calendar cal = DateTime.calendarFromDate(repeatOrigin);
			final int year = cal.get(Calendar.YEAR);
			final int month = cal.get(Calendar.MONTH);
			final int day = cal.get(Calendar.DAY_OF_MONTH);

			final DatePickerDialog datePickerDialog = new DatePickerDialog(mContext, onDateSetListener, year, month, day);
			datePickerDialog.setCancelable(false);
			datePickerDialog.setTitle(R.string._title_repetition_origin);
			datePickerDialog.show();
		}

		private void updateSummary()
		{
			//final int repeatMode = (Integer) getFieldValue("repeat");
			final int repeatMode = getFieldValue();
			final long repeatArg = (Long) getFieldValue("repeatArg");
			final Date repeatOrigin = (Date) getFieldValue("repeatOrigin");

			final String summary;

			if(repeatMode == Drug.REPEAT_EVERY_N_DAYS)
			{
				// FIXME change to next occurence
				summary = mContext.getString(
						R.string._msg_freq_every_n_days,
						repeatArg,
						DateTime.toNativeDate(repeatOrigin)
				);
			}
			else if(repeatMode == Drug.REPEAT_WEEKDAYS)
				summary = getWeekdayRepeatSummary(repeatArg);
			else if(repeatMode == Drug.REPEAT_21_7)
			{
				summary = mContext.getString(
						R.string._msg_freq_21days_on_7days_off,
						DateTime.toNativeDate(repeatOrigin)
				);
			}
			else
				summary = null;

			if(summary != null)
				mPref.setSummary(summary);
		}

		private String getWeekdayRepeatSummary(long repeatArg)
		{
			final LinkedList<String> weekdays = new LinkedList<String>();

			for(int i = 0; i != 7; ++i)
			{
				if((repeatArg & (1 << i)) != 0)
					weekdays.add(Constants.SHORT_WEEK_DAY_NAMES[i]);
			}

			if(weekdays.isEmpty())
				return mContext.getString(R.string._summary_intake_never);

			StringBuilder sb = new StringBuilder(weekdays.get(0));

			for(int i = 1; i != weekdays.size(); ++i)
				sb.append(", " + weekdays.get(i));

			return sb.toString();
		}
	}

	@SuppressWarnings("rawtypes")
	private static class CurrentSupplyPreferenceHelper extends MyDialogPreferenceHelper
	{
		private Context mContext;
		private Object mValue;

		@SuppressWarnings({ "unused" })
		public CurrentSupplyPreferenceHelper() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void initPreference(MyDialogPreference preference, Object fieldValue)
		{
			super.initPreference(preference, fieldValue);
			mContext = preference.getContext().getApplicationContext();

			((CurrentSupplyPreference) preference).setRefillSize((Integer) getFieldValue("refillSize"));
		}

		@Override
		public boolean updatePreference(MyDialogPreference preference, Object newValue)
		{
			super.updatePreference(preference, newValue);
			//preference.setSummary(getSummary());
			//preference.notifyDependencyChange(false);
			return true;
		}

		@Override
		public void updateSummary(MyDialogPreference preference, Object newValue)
		{
			preference.setSummary(getSummary(newValue));
			mValue = newValue;
		}

		@Override
		public void onDependencyChange(MyDialogPreference preference, String depKey)
		{
			preference.setSummary(getSummary(mValue));
			if("refillSize".equals(depKey))
				((CurrentSupplyPreference) preference).setRefillSize((Integer) getFieldValue("refillSize"));
		}

		private String getSummary(Object value)
		{
			final Drug drug = ((DrugWrapper) mObject).get();
			final Fraction currentSupply = (Fraction) value;

			if(currentSupply.isZero())
			{
				if(drug.getRefillSize() == 0)
					return mContext.getString(R.string._summary_not_available);

				return "0";
			}

			if(drug.getRepeatMode() == Drug.REPEAT_ON_DEMAND || drug.hasNoDoses())
			{
				// TODO change?
				return currentSupply.toString();
			}

			final int currentSupplyDays = Math.max(Entries.getSupplyDaysLeftForDrug(drug, null), 0);
			final Date end = DateTime.add(DateTime.today(), Calendar.DAY_OF_MONTH, currentSupplyDays);
			return mContext.getString(R.string._msg_supply, currentSupply, DateTime.toNativeDate(end));
		}
	}

	@SuppressWarnings("rawtypes")
	private static class FractionAsIntegerPreferenceHelper extends MyDialogPreferenceHelper
	{
		@SuppressWarnings("unused")
		public FractionAsIntegerPreferenceHelper() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void initPreference(MyDialogPreference preference, Object fieldValue)
		{
			super.initPreference(preference, new Fraction((Integer) fieldValue));
			((FractionPreference) preference).disableFractionInputMode(true);
		}

		@Override
		public boolean updatePreference(MyDialogPreference preference, Object newPrefValue)
		{
			setFieldValue(((Fraction) newPrefValue).intValue());
			preference.setSummary(newPrefValue.toString());
			return true;
		}
	}

	public static class FormPreferenceHelper extends ListPreferenceWithIntHelper
	{
		public FormPreferenceHelper() {
			super(R.array.drug_forms);
		}
	}

	private static class CurrentSupplyPreference extends FractionPreference
	{
		private int mRefillSize;

		public CurrentSupplyPreference(Context context) {
			super(context);
		}

		public void setRefillSize(int refillSize) {
			mRefillSize = refillSize;
		}

		@Override
		protected Dialog onGetCustomDialog()
		{
			final FractionInputDialog dialog = (FractionInputDialog) super.onGetCustomDialog();

			if(mRefillSize != 0)
			{
				//final ViewStub stub = (ViewStub) dialog.findViewById(R.id.stub);
				//stub.setLayoutResource(R.layout.current_supply_button);

				final ViewStub stub = dialog.getFooterStub();
				stub.setLayoutResource(R.layout.current_supply_button);
				//final View inflated = stub.inflate();

				final Button btn = (Button) stub.inflate().findViewById(R.id.btn_current_supply);
				btn.setText("+" + Integer.toString(mRefillSize));
				btn.setOnClickListener(mListener);
			}

			return dialog;
		}

		private View.OnClickListener mListener = new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				final FractionInputDialog dialog = (FractionInputDialog) getDialog();
				if(dialog != null)
				{
					dialog.setValue(dialog.getValue().plus(mRefillSize));
					v.setEnabled(false);
				}
			}
		};
	}

	private static class NotificationsPreferenceHelper extends PreferenceHelper<ListPreference, Boolean>
	{
		private static final int NOTIFY_ALL = 0;
		private static final int NOTIFY_SUPPLIES_ONLY = 1;
		private String[] mEntries;

		public NotificationsPreferenceHelper() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void initPreference(ListPreference preference, Boolean fieldValue)
		{
			if(mEntries == null)
			{
				final Resources r = preference.getContext().getResources();
				mEntries = r.getStringArray(R.array.drug_notifications);
			}

			preference.setEntries(mEntries);
			Util.populateListPreferenceEntryValues(preference);
			preference.setValueIndex(fieldValue ? NOTIFY_SUPPLIES_ONLY : NOTIFY_ALL);
			preference.setDialogTitle(preference.getTitle());
		}

		@Override
		public void updateSummary(ListPreference preference, Boolean newValue)
		{
			preference.setSummary(mEntries[newValue ? NOTIFY_SUPPLIES_ONLY : NOTIFY_ALL]);
		}

		@Override
		public Boolean toFieldType(Object prefValue)
		{
			final int i = Integer.parseInt((String) prefValue);
			return i == NOTIFY_SUPPLIES_ONLY;
		}
	}

	private final OnPreferenceChangeListener mListener = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			Log.v(TAG, "onPreferenceChange: " + preference.getKey() + " => " + newValue);
			return false;
		}
	};
}