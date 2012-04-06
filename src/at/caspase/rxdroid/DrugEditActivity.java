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

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import at.caspase.androidutils.MyDialogPreference;
import at.caspase.androidutils.otpm.CheckboxPreferenceHelper;
import at.caspase.androidutils.otpm.ListPreferenceWithIntHelper;
import at.caspase.androidutils.otpm.MyDialogPreferenceHelper;
import at.caspase.androidutils.otpm.OTPM;
import at.caspase.androidutils.otpm.OTPM.AddPreference;
import at.caspase.androidutils.otpm.OTPM.MapToPreference;
import at.caspase.androidutils.otpm.OTPM.ObjectWrapper;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Schedule;
import at.caspase.rxdroid.preferences.DosePreference;
import at.caspase.rxdroid.preferences.DrugNamePreference2;
import at.caspase.rxdroid.preferences.FractionPreference;
import at.caspase.rxdroid.test.ObjectToPreferenceTestActivity.FormPreferenceHelper;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.SimpleBitSet;

/**
 * Edit a drug's database entry.
 * @author Joseph Lehner
 *
 */

public class DrugEditActivity extends PreferenceActivity implements OnPreferenceClickListener
{
	public static final String EXTRA_DRUG = "drug";
	public static final String EXTRA_FOCUS_ON_CURRENT_SUPPLY = "focus_on_current_supply";

	private static final String TAG = DrugEditActivity.class.getName();
	private static final boolean LOGV = true;



	private DrugWrapper mWrapper;
	private int mDrugHash;

	// if true, we're editing an existing drug; if false, we're adding a new one
	private boolean mIsEditing;

	@Override
	public void onBackPressed()
	{
		final Intent intent = getIntent();
		final String action = intent.getAction();

		final Drug drug = mWrapper.get();
		final String drugName = drug.getName();

		if(drugName == null || drugName.length() == 0)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			//builder.setTitle(R.string._title_warning);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(R.string._msg_err_empty_drug_name);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					finish();
				}
			});
			builder.show();

			return;
		}

		if(Intent.ACTION_EDIT.equals(action))
		{
			if(mDrugHash != drug.hashCode())
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string._title_save_chanes);
				builder.setIcon(android.R.drawable.ic_dialog_info);
				builder.setMessage(R.string._msg_save_drug_changes);

				final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(which == DialogInterface.BUTTON_POSITIVE)
						{
							Database.update(drug);
							setResult(RESULT_OK);
							Toast.makeText(getApplicationContext(), R.string._toast_saved, Toast.LENGTH_SHORT).show();
						}

						finish();
					}
				};

				builder.setPositiveButton(R.string._btn_save, onClickListener);
				builder.setNegativeButton(R.string._btn_discard, onClickListener);

				builder.show();
				return;
			}
		}
		else if(Intent.ACTION_INSERT.equals(action))
		{
			Database.create(drug, 0);
			setResult(RESULT_OK);
			Toast.makeText(getApplicationContext(), getString(R.string._toast_saved), Toast.LENGTH_SHORT).show();
		}

		finish();
	}

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		if(preference.getKey().equals("delete"))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(getString(R.string._title_delete_drug, mWrapper.get().getName()));
			builder.setMessage(R.string._msg_delete_drug);

			builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Database.delete(mWrapper.get());
					Toast.makeText(getApplicationContext(), R.string._toast_deleted, Toast.LENGTH_SHORT).show();
					finish();
				}
			});

			builder.setNegativeButton(android.R.string.no, null);
			builder.show();

			return true;
		}

		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.empty);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		Intent intent = getIntent();
		String action = intent.getAction();

		Drug drug = null;

		mWrapper = new DrugWrapper(getApplicationContext());

		if(Intent.ACTION_EDIT.equals(action))
		{
			Serializable extra = intent.getSerializableExtra(EXTRA_DRUG);
			if(extra == null)
				throw new IllegalStateException("ACTION_EDIT requires EXTRA_DRUG");

			drug = (Drug) extra;

			mWrapper.set(drug);
			mDrugHash = drug.hashCode();
			mIsEditing = true;

			setTitle(drug.getName());
		}
		else if(Intent.ACTION_INSERT.equals(action))
			setTitle(R.string._title_add_drug);
		else
			throw new IllegalArgumentException("Unhandled action " + action);

		if(drug == null)
		{
			mIsEditing = false;
			mWrapper.set(new Drug());
		}

		getPreferenceScreen().removeAll(); // FIXME this is a hack
		OTPM.mapToPreferenceScreen(getPreferenceScreen(), mWrapper);

		Preference deletePref = findPreference("delete");
		if(deletePref != null)
		{
			if(mIsEditing)
				deletePref.setOnPreferenceClickListener(this);
			else if(deletePref != null)
				getPreferenceScreen().removePreference(deletePref);
		}
		else
			Log.d(TAG, "No delete preference found");
	}

	private static class DrugWrapper extends ObjectWrapper<Drug>
	{
		@MapToPreference
		(
			titleResId = R.string._title_drug_name,
			order = 1,
			type = DrugNamePreference2.class,
			helper = MyDialogPreferenceHelper.class
		)
		private String name;

		@AddPreference(order = 2)
		private final Preference mDeletePreference;

		@MapToPreference
		(
			//title = "Morning",
			titleResId = R.string._Morning,
			key = "morning",
			//category = "Intake schedule",
			categoryResId = R.string._title_intake_schedule,
			order = 3,
			type = DosePreference.class,
			helper = MyDialogPreferenceHelper.class
		)
		private Fraction doseMorning;

		@MapToPreference
		(
			//title = "Noon",
			titleResId = R.string._Noon,
			key = "noon",
			order = 4,
			type = DosePreference.class,
			helper = MyDialogPreferenceHelper.class
		)
		private Fraction doseNoon;

		@MapToPreference
		(
			//title = "Evening",
			titleResId = R.string._Evening,
			key = "evening",
			order = 5,
			type = DosePreference.class,
			helper = MyDialogPreferenceHelper.class
		)
		private Fraction doseEvening;

		@MapToPreference
		(
			//title = "Night",
			titleResId = R.string._Night,
			key = "night",
			endActiveCategory = true,
			order = 6,
			type = DosePreference.class,
			helper = MyDialogPreferenceHelper.class
		)
		private Fraction doseNight;

		@MapToPreference
		(
			//title = "Repeat mode",
			titleResId = R.string._title_repeat,
			order = 7,
			type = ListPreference.class,
			helper = RepeatModePreferenceHelper.class
		)
		private int repeat;

		@MapToPreference
		(
			titleResId = R.string._title_icon,
			categoryResId = R.string._title_misc,
			order = 8,
			type = ListPreference.class,
			helper = FormPreferenceHelper.class
		)
		private int form;

		@MapToPreference
		(
			titleResId = R.string._title_refill_size,
			order = 10,
			type = FractionPreference.class,
			helper = FractionAsIntegerPreferenceHelper.class
		)
		private int refillSize;

		@MapToPreference
		(
			titleResId = R.string._title_current_supply,
			order = 11,
			type = FractionPreference.class,
			helper = CurrentSupplyPreferenceHelper.class
		)
		private Fraction currentSupply;

		@MapToPreference
		(
			titleResId = R.string._title_active,
			//endActiveCategory = true,
			order = 12,
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

		public DrugWrapper(Context context)
		{
			mDeletePreference = new Preference(context);
			mDeletePreference.setKey("delete");
			mDeletePreference.setTitle(context.getString(R.string._title_delete));
		}

		@Override
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

			name = drug.getName();
			form = drug.getForm();
		}

		@Override
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
			drug.setRepeat(repeat);

			final Fraction doses[] = { doseMorning, doseNoon, doseEvening, doseNight };

			for(int i = 0; i != doses.length; ++i)
				drug.setDose(Constants.DOSE_TIMES[i], doses[i]);

			drug.setRepeatArg(repeatArg);
			drug.setRepeatOrigin(repeatOrigin);

			return drug;
		}
	}

	private static class RepeatModePreferenceHelper extends ListPreferenceWithIntHelper
	{
		private ListPreference mPref;
		private Context mCtx;

		public RepeatModePreferenceHelper() {
			super(GlobalContext.get(), R.array.drug_repeat);
		}

		@Override
		public void initPreference(ListPreference preference, Integer fieldValue)
		{
			super.initPreference(preference, fieldValue);

			mPref = preference;
			mCtx = preference.getContext();

			//preference.setDependency("currentSupply");

			updateSummary();
		}

		@Override
		public boolean updatePreference(ListPreference preference, Object newPrefValue)
		{
			final int mode = Integer.parseInt((String) newPrefValue, 10);
			switch(mode)
			{
				case Drug.REPEAT_EVERY_N_DAYS:
					handleEveryNDaysRepeatMode();
					return false;

				case Drug.REPEAT_WEEKDAYS:
					handleWeekdaysRepeatMode();
					return false;

				case Drug.REPEAT_DAILY:
					mPref.setSummary(R.string._title_daily);
					break;

				case Drug.REPEAT_ON_DEMAND:
					mPref.setSummary(R.string._title_on_demand);
					break;

				default:
					Toast.makeText(mCtx, "Not implemented", Toast.LENGTH_LONG).show();
					return false;
			}

			return super.updatePreference(preference, newPrefValue);
		}

		private void handleEveryNDaysRepeatMode()
		{
			final Date repeatOrigin;
			final long repeatArg;

			int oldRepeatMode = getFieldValue();

			if(oldRepeatMode != Drug.REPEAT_EVERY_N_DAYS)
			{
				repeatOrigin = DateTime.todayDate();
				repeatArg = 2;
			}
			else
			{
				repeatOrigin = (Date) getFieldValue("repeatOrigin");
				repeatArg = (Long) getFieldValue("repeatArg");
			}

			final EditText editText = new EditText(mCtx);
			editText.setText(Long.toString(repeatArg));
			editText.setEms(20);
			editText.setMaxLines(1);
			editText.setInputType(InputType.TYPE_CLASS_NUMBER);
			editText.setSelectAllOnFocus(true);

			final OnDateSetListener onDateSetListener = new OnDateSetListener() {

				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
				{
					final Date repeatOrigin = DateTime.date(year, monthOfYear, dayOfMonth);
					final long repeatArg = Long.valueOf(editText.getText().toString());

					setFieldValue("repeat", Drug.REPEAT_EVERY_N_DAYS);
					setFieldValue("repeatOrigin", repeatOrigin);
					setFieldValue("repeatArg", repeatArg);

					updateSummary();
				}
			};

			final AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
			builder.setTitle(R.string._title_every_n_days);
			builder.setMessage(R.string._msg_every_n_days_distance);
			builder.setView(editText);
			builder.setCancelable(true);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					int year = 1900 + repeatOrigin.getYear();
					int month = repeatOrigin.getMonth();
					int day = repeatOrigin.getDate();

					DatePickerDialog datePickerDialog =
							new DatePickerDialog(mCtx, onDateSetListener, year, month, day);

					datePickerDialog.setCancelable(false);
					datePickerDialog.setMessage(mCtx.getString(R.string._msg_repetition_origin));
					datePickerDialog.show();
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
						editText.setError(mCtx.getString(R.string._msg_drug_repeat_ge_2));
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
				setFieldValue("repeatOrigin", DateTime.todayDate());
			}

			long repeatArg = (Long) getFieldValue("repeatArg");
			final boolean[] checkedItems = SimpleBitSet.toBooleanArray(repeatArg, Constants.LONG_WEEK_DAY_NAMES.length);

			if(repeatArg == 0)
			{
				// check the current weekday if none is selected
				final int weekday = DateTime.nowCalendar().get(Calendar.DAY_OF_WEEK);
				final int index = CollectionUtils.indexOf(weekday, Constants.WEEK_DAYS);
				checkedItems[index] = true;
				repeatArg |= 1 << index;
			}

			final SimpleBitSet bitset = new SimpleBitSet(repeatArg);

			final AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
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
					setFieldValue("repeat", Drug.REPEAT_WEEKDAYS);
					setFieldValue("repeatArg", bitset.longValue());

					updateSummary();
				}
			});

			builder.show();
		}

		private void updateSummary()
		{
			final int repeatMode = (Integer) getFieldValue("repeat");
			final long repeatArg = (Long) getFieldValue("repeatArg");
			final Date repeatOrigin = (Date) getFieldValue("repeatOrigin");

			final String summary;

			if(repeatMode == Drug.REPEAT_EVERY_N_DAYS)
			{
				// FIXME change to next occurence
				summary = mCtx.getString(
						R.string._msg_freq_every_n_days,
						repeatArg,
						DateTime.toNativeDate(repeatOrigin)
				);
			}
			else if(repeatMode == Drug.REPEAT_WEEKDAYS)
				summary = getWeekdayRepeatSummary(repeatArg);
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
				return mCtx.getString(R.string._summary_intake_never);

			StringBuilder sb = new StringBuilder(weekdays.get(0));

			for(int i = 1; i != weekdays.size(); ++i)
				sb.append(", " + weekdays.get(i));

			return sb.toString();
		}
	}

	private static class CurrentSupplyPreferenceHelper extends MyDialogPreferenceHelper
	{
		private Context mContext;

		public CurrentSupplyPreferenceHelper() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void initPreference(MyDialogPreference preference, Object fieldValue)
		{
			super.initPreference(preference, fieldValue);
			mContext = preference.getContext().getApplicationContext();
			preference.setSummary(getSummary());
			//preference.setDependency("repeat");
		}

		@Override
		public boolean updatePreference(MyDialogPreference preference, Object newValue)
		{
			super.updatePreference(preference, newValue);
			preference.setSummary(getSummary());
			return true;
		}

		private String getSummary()
		{
			final Drug drug = (Drug) mWrapper.get();
			final Fraction currentSupply = (Fraction) getFieldValue();

			if(currentSupply.isZero())
			{
				if(drug.getRefillSize() == 0)
					return mContext.getString(R.string._summary_not_available);

				return "0";
			}

			if(drug.getRepeatMode() == Drug.REPEAT_ON_DEMAND)
			{
				// TODO change?
				return currentSupply.toString();
			}

			final int currentSupplyDays = drug.getCurrentSupplyDays();
			final Date end = DateTime.add(DateTime.todayDate(), Calendar.DAY_OF_MONTH, currentSupplyDays);
			return mContext.getString(R.string._msg_supply, currentSupply, DateTime.toNativeDate(end));
		}
	}

	private static class FractionAsIntegerPreferenceHelper extends MyDialogPreferenceHelper
	{
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
}