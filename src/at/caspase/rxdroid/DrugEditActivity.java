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
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
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
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.preferences.DosePreference;
import at.caspase.rxdroid.preferences.DrugNamePreference;
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

public class DrugEditActivity extends PreferenceActivity implements OnPreferenceChangeListener,
		OnPreferenceClickListener, OnDateSetListener
{
	public static final String EXTRA_DRUG = "drug";
	public static final String EXTRA_FOCUS_ON_CURRENT_SUPPLY = "focus_on_current_supply";

	private static final String TAG = DrugEditActivity.class.getName();

	private static final String[] PREF_KEYS = {
			"drug_name",
			"morning",
			"noon",
			"evening",
			"night",
			"repeat",
			"drug_form",
			"current_supply",
			"refill_size",
			"is_active"
	};

	private Drug mDrug;
	private int mDrugHash = 0;

	private DrugNamePreference mDrugName;
	private DosePreference[] mDosePrefs;
	private ListPreference mFreqPreference;
	private ListPreference mDrugForm;
	private FractionPreference mCurrentSupply;
	private EditTextPreference mRefillSize;
	private CheckBoxPreference mIsActive;

	// if true, we're editing an existing drug; if false, we're adding a new one
	private boolean mIsEditing;

	@Override
	public void onBackPressed()
	{
		final Intent intent = getIntent();
		final String action = intent.getAction();

		String drugName = mDrug.getName();

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
			if(mDrugHash != mDrug.hashCode())
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string._title_save_chanes);
				builder.setIcon(android.R.drawable.ic_dialog_info);
				builder.setMessage(R.string._msg_save_drug_changes /* "Do you want to save changes made to this drug?" */);

				final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(which == DialogInterface.BUTTON_POSITIVE)
						{
							Database.update(mDrug);
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
			Database.create(mDrug, 0);
			setResult(RESULT_OK);
			Toast.makeText(getApplicationContext(), getString(R.string._toast_saved), Toast.LENGTH_SHORT).show();
		}

		finish();
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		String key = preference.getKey();

		if("drug_name".equals(key))
			mDrug.setName((String) newValue);
		else if("morning".equals(key) || "noon".equals(key) || "evening".equals(key) || "night".equals(key))
			mDrug.setDose(DosePreference.getDoseTimeFromKey(key), (Fraction) newValue);
		else if("repeat".equals(key))
		{
			final int repeat = toInt(newValue);

			if(repeat != Drug.REPEAT_DAILY && repeat != Drug.REPEAT_ON_DEMAND)
			{
				switch(repeat)
				{
					case Drug.REPEAT_EVERY_N_DAYS:
						handleEveryNDaysRepeat();
						break;

					case Drug.REPEAT_WEEKDAYS:
						handleWeekdayRepeat();
						break;

					default:
						throw new IllegalStateException("Invalid repeat value");
				}
			}
			else
			{
				mDrug.setRepeat(repeat);
				updatePreferences();
			}

			// the user might cancel a dialog in one of the handle<foobar>Repeat()
			// functions. by returning false here, we ensure that setValueIndex() is
			// only called if the repeat was actually changed
			return false;
		}
		else if("drug_form".equals(key))
			mDrug.setForm(toInt(newValue));
		else if("current_supply".equals(key))
			mDrug.setCurrentSupply((Fraction) newValue);
		else if("refill_size".equals(key))
			mDrug.setRefillSize(toInt(newValue));
		else if("is_active".equals(key))
			mDrug.setActive((Boolean) newValue);
		else
			Log.d(TAG, "onPreferenceChange: Ignoring " + key);

		updatePreferences();

		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		if(preference.getKey().equals("delete"))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(getString(R.string._title_delete_drug, mDrug.getName()));
			builder.setMessage(R.string._msg_delete_drug);

			builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Database.delete(mDrug);
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
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{

	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.drug_edit);

		mDrugName = (DrugNamePreference) findPreference("drug_name");
		mDosePrefs = new DosePreference[4];

		String[] dosePrefKeys = { "morning", "noon", "evening", "night" };
		for(int i = 0; i != dosePrefKeys.length; ++i)
			mDosePrefs[i] = (DosePreference) findPreference(dosePrefKeys[i]);

		mFreqPreference = (ListPreference) findPreference("repeat");
		mDrugForm = (ListPreference) findPreference("drug_form");
		mCurrentSupply = (FractionPreference) findPreference("current_supply");
		mRefillSize = (EditTextPreference) findPreference("refill_size");
		mIsActive = (CheckBoxPreference) findPreference("is_active");

		// mark all preferences as non-persisting!
		for(String key : PREF_KEYS)
		{
			final Preference pref = findPreference(key);
			pref.setPersistent(false);
			pref.setOnPreferenceChangeListener(this);
		}

		Util.populateListPreferenceEntryValues(mFreqPreference);
		Util.populateListPreferenceEntryValues(mDrugForm);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		Intent intent = getIntent();
		String action = intent.getAction();

		if(Intent.ACTION_EDIT.equals(action))
		{
			Serializable extra = intent.getSerializableExtra(EXTRA_DRUG);
			if(extra == null)
				throw new IllegalStateException("ACTION_EDIT requires EXTRA_DRUG");

			mIsEditing = true;
			mDrug = (Drug) extra;
			mDrugHash = mDrug.hashCode();
		}
		else if(!Intent.ACTION_INSERT.equals(action))
			throw new IllegalArgumentException("Unhandled action " + action);

		if(mDrug == null)
		{
			mIsEditing = false;
			mDrug = new Drug();
		}

		Preference deletePref = findPreference("delete");

		if(mIsEditing)
			deletePref.setOnPreferenceClickListener(this);
		else if(deletePref != null)
			getPreferenceScreen().removePreference(deletePref);

		initPreferences();
	}

	private void initPreferences()
	{
		mDrugName.setInitialName(mDrug.getName());

		for(DosePreference dosePref : mDosePrefs)
			dosePref.setDrug(mDrug);

		updatePreferences();
	}

	/**
	 * Syncs the preference values with the drug's fields.
	 */
	private void updatePreferences()
	{
		Log.d(TAG, "updatePreferences: mDrug=" + mDrug);

		mDrugName.setName(mDrug.getName());
		//mDrugName.setText(mDrug.getName());

		// intake repeat

		final int repeat = mDrug.getRepeat();
		final String summary;

		switch(repeat)
		{
			case Drug.REPEAT_DAILY:
				summary = getString(R.string._msg_freq_daily);
				break;

			case Drug.REPEAT_EVERY_N_DAYS:
				int distance = (int) mDrug.getRepeatArg();
				// FIXME change to next occurence
				String origin = DateTime.toNativeDate(mDrug.getRepeatOrigin());
				summary = getString(R.string._msg_freq_every_n_days, distance, origin);
				break;

			case Drug.REPEAT_WEEKDAYS:
				long repeatArg = mDrug.getRepeatArg();
				summary = getWeekdayRepeatSummary(repeatArg);
				break;

			case Drug.REPEAT_ON_DEMAND:
				summary = getString(R.string._title_on_demand);
				break;

			default:
				throw new IllegalStateException("Invalid repeat value " + repeat);
		}

		mFreqPreference.setSummary(summary);
		mFreqPreference.setValueIndex(repeat);

		// drug form
		int form = mDrug.getForm();
		String[] forms = getResources().getStringArray(R.array.drug_forms);
		mDrugForm.setSummary(forms[form]);
		mDrugForm.setValueIndex(form);

		// current supply
		// refill size
		final int refillSize = mDrug.getRefillSize();
		final Fraction currentSupply = mDrug.getCurrentSupply();
		if(currentSupply.compareTo(0) == 0)
		{
			if(refillSize != 0)
				mCurrentSupply.setSummary(R.string._summary_not_available);
			else
				mCurrentSupply.setSummary("0");

			mCurrentSupply.setValue(Fraction.ZERO);
		}
		else
		{
			mCurrentSupply.setSummary(currentSupply.toString());
			mCurrentSupply.setValue(currentSupply);

			if(mDrug.getRepeat() != Drug.REPEAT_ON_DEMAND)
			{
				final int currentSupplyDays = mDrug.getCurrentSupplyDays();
				if(currentSupplyDays > 0)
				{
					final Calendar end = DateTime.today();
					end.add(Calendar.DAY_OF_MONTH, currentSupplyDays);

					mCurrentSupply.setSummary(getString(R.string._msg_supply,
							currentSupply.toString(), DateTime.toNativeDate(end.getTime())));
				}
			}
		}

		if(refillSize == 0)
		{
			mRefillSize.setSummary(R.string._summary_not_available);
			mRefillSize.setText("0");
		}
		else
		{
			final String refillSizeStr = Integer.toString(mDrug.getRefillSize());
			mRefillSize.setSummary(refillSizeStr);
			mRefillSize.setText(refillSizeStr);
		}

		mCurrentSupply.setLongClickSummand(new Fraction(mDrug.getRefillSize()));

		// active?
		mIsActive.setChecked(mDrug.isActive());
	}

	private void handleEveryNDaysRepeat()
	{
		final Date repeatOrigin;
		final long repeatArg;

		if(mDrug.getRepeat() != Drug.REPEAT_EVERY_N_DAYS)
		{
			repeatOrigin = DateTime.todayDate();
			repeatArg = 2;
		}
		else
		{
			repeatOrigin = mDrug.getRepeatOrigin();
			repeatArg = mDrug.getRepeatArg();
		}

		final EditText editText = new EditText(this);
		editText.setText(Long.toString(repeatArg));
		editText.setEms(20);
		editText.setMaxLines(1);
		editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		editText.setSelectAllOnFocus(true);

		final OnDateSetListener onDateSetListener = new OnDateSetListener() {

			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
			{
				mDrug.setRepeat(Drug.REPEAT_EVERY_N_DAYS);
				mDrug.setRepeatOrigin(DateTime.date(year, monthOfYear, dayOfMonth));
				mDrug.setRepeatArg(Long.valueOf(editText.getText().toString()));
				updatePreferences();
			}
		};

		final Context context = this;

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
						new DatePickerDialog(context, onDateSetListener, year, month, day);

				datePickerDialog.setCancelable(false);
				datePickerDialog.setMessage(getString(R.string._msg_repetition_origin));
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
					editText.setError(getString(R.string._msg_drug_repeat_ge_2));
				else
					editText.setError(null);
			}
		});

		dialog.show();
		editText.performClick();
	}

	private void handleWeekdayRepeat()
	{
		// if this changes the drug's repeat, all repeat options are reset. if the
		// drug's repeat already was FREQ_WEEKDAYS, this call will not change anything
		mDrug.setRepeat(Drug.REPEAT_WEEKDAYS);

		long repeatArg = mDrug.getRepeatArg();
		final boolean[] checkedItems = SimpleBitSet.toBooleanArray(repeatArg, Constants.LONG_WEEK_DAY_NAMES.length);

		if(repeatArg == 0)
		{
			// check the current weekday if none are selected
			final int weekday = DateTime.now().get(Calendar.DAY_OF_WEEK);
			final int index = CollectionUtils.indexOf(weekday, Constants.WEEK_DAYS);
			checkedItems[index] = true;
			repeatArg |= 1 << index;
		}

		final SimpleBitSet bitSet = new SimpleBitSet(repeatArg);

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string._title_weekdays);
		builder.setMultiChoiceItems(Constants.LONG_WEEK_DAY_NAMES, checkedItems, new OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked)
			{
				bitSet.set(which, isChecked);

				final Button positiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
				positiveButton.setEnabled(bitSet.longValue() != 0);
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				mDrug.setRepeatArg(bitSet.longValue());
				updatePreferences();
			}
		});

		builder.show();
	}

	private String getWeekdayRepeatSummary(long repeatArgs)
	{
		final LinkedList<String> weekdays = new LinkedList<String>();

		for(int i = 0; i != 7; ++i)
		{
			if((repeatArgs & 1 << i) != 0)
				weekdays.add(Constants.SHORT_WEEK_DAY_NAMES[i]);
		}

		if(weekdays.isEmpty())
			return getString(R.string._summary_intake_never);

		StringBuilder sb = new StringBuilder(weekdays.get(0));

		for(int i = 1; i != weekdays.size(); ++i)
			sb.append(", " + weekdays.get(i));

		return sb.toString();
	}

	private static int toInt(Object string) {
		return Integer.parseInt((String) string, 10);
	}
}
