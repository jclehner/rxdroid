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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
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
			"frequency",
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

		if(drugName == null || drugName.isEmpty())
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string._title_error);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(R.string._msg_err_empty_drug_name);
			builder.setPositiveButton(android.R.string.ok, null);
			builder.show();

			return;
		}

		if(Intent.ACTION_EDIT.equals(action))
		{
			if(mDrugHash != mDrug.hashCode())
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Save changes");
				builder.setIcon(android.R.drawable.ic_dialog_info);
				builder.setMessage("Do you want to save changes made to this drug?");

				final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(which == AlertDialog.BUTTON_POSITIVE)
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
			Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
		}

		finish();
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		String key = preference.getKey();
		
		if("drug_name".equals(key))
		{
			Log.d(TAG, "onPreferenceChange: drugName=" + newValue);
			mDrug.setName((String) newValue);
		}
		else if("morning".equals(key) || "noon".equals(key) || "evening".equals(key) || "night".equals(key))
			mDrug.setDose(DosePreference.getDoseTimeFromKey(key), (Fraction) newValue);
		else if("frequency".equals(key))
		{
			int frequency = toInt(newValue);

			if(frequency != Drug.FREQ_DAILY)
			{
				switch(frequency)
				{
					case Drug.FREQ_EVERY_N_DAYS:
						handleEveryNDaysFrequency();
						break;
						
					case Drug.FREQ_WEEKDAYS:
						handleWeekdayFrequency();
						break;		

					default:
						throw new IllegalStateException("Invalid frequency value");
				}
			}
			else
			{
				mDrug.setFrequency(Drug.FREQ_DAILY);
				updatePreferences();
			}
			
			return true;
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
			builder.setTitle(mDrug.getName());
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

		mFreqPreference = (ListPreference) findPreference("frequency");
		mDrugForm = (ListPreference) findPreference("drug_form");
		mCurrentSupply = (FractionPreference) findPreference("current_supply");
		mRefillSize = (EditTextPreference) findPreference("refill_size");
		mIsActive = (CheckBoxPreference) findPreference("is_active");
		
		// mark all preferences as non-persisting!
		for(String key : PREF_KEYS)
		{
			Preference pref = findPreference(key);
			pref.setPersistent(false);
			pref.setOnPreferenceChangeListener(this);
		}

		populateEntryValues("frequency");
		populateEntryValues("drug_form");
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

		mFreqPreference.setValueIndex(mDrug.getFrequency());
		mCurrentSupply.setValue(mDrug.getCurrentSupply());
		mCurrentSupply.setLongClickSummand(new Fraction(mDrug.getRefillSize()));
		mRefillSize.setText(Integer.toString(mDrug.getRefillSize()));
		mIsActive.setChecked(mDrug.isActive());

		updatePreferences();
	}

	/**
	 * Syncs the preference values with the drug's fields.
	 */
	private void updatePreferences()
	{
		mDrugName.setName(mDrug.getName());

		// intake frequency

		int frequency = mDrug.getFrequency();
		String summary = null;

		switch(frequency)
		{
			case Drug.FREQ_DAILY:
				summary = getString(R.string._msg_freq_daily);
				break;
				
			case Drug.FREQ_EVERY_N_DAYS:
				int distance = (int) mDrug.getFrequencyArg();
				// FIXME change to next occurence
				String origin = DateTime.toNativeDate(mDrug.getFrequencyOrigin());			
				summary = getString(R.string._msg_freq_every_n_days, distance, origin);
				break;
				
			case Drug.FREQ_WEEKDAYS:
				long frequencyArg = mDrug.getFrequencyArg();				
				summary = getWeekdayFrequencySummary(frequencyArg);
				break;

			default:
				throw new IllegalStateException("Invalid frequency value " + frequency);
		}

		mFreqPreference.setSummary(summary);
		mFreqPreference.setValueIndex(frequency);

		// drug form
		int form = mDrug.getForm();
		String[] forms = getResources().getStringArray(R.array.drug_forms);
		mDrugForm.setSummary(forms[form]);
		mDrugForm.setValueIndex(form);

		// current supply
		Fraction currentSupply = mDrug.getCurrentSupply();
		mCurrentSupply.setSummary(currentSupply.toString());
		mCurrentSupply.setValue(currentSupply);

		// refill size
		String refillSize = Integer.toString(mDrug.getRefillSize());
		mRefillSize.setSummary(refillSize);
		mRefillSize.setText(refillSize);

		// active?
		mIsActive.setChecked(mDrug.isActive());
	}

	private void populateEntryValues(String preferenceKey)
	{
		ListPreference pref = (ListPreference) findPreference(preferenceKey);
		int entryCount = pref.getEntries().length;

		String[] values = new String[entryCount];
		for(int i = 0; i != entryCount; ++i)
			values[i] = Integer.toString(i);

		pref.setEntryValues(values);
	}
	
	private void handleEveryNDaysFrequency()
	{
		final Date frequencyOrigin;
		final long frequencyArg;
		
		if(mDrug.getFrequency() != Drug.FREQ_EVERY_N_DAYS)
		{
			frequencyOrigin = DateTime.today().getTime();
			frequencyArg = 2;
		}
		else
		{
			frequencyOrigin = mDrug.getFrequencyOrigin();
			frequencyArg = mDrug.getFrequencyArg();
		}		
		
		final EditText editText = new EditText(this);
		editText.setText(Long.toString(frequencyArg));
		editText.setEms(20);
		editText.setMaxLines(1);
		editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		editText.setSelectAllOnFocus(true);
		
		final OnDateSetListener onDateSetListener = new OnDateSetListener() {
			
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
			{	
				mDrug.setFrequency(Drug.FREQ_EVERY_N_DAYS);
				mDrug.setFrequencyOrigin(DateTime.date(year, monthOfYear, dayOfMonth).getTime());
				mDrug.setFrequencyArg(Long.valueOf(editText.getText().toString()));
				updatePreferences();
			}
		};
		
		final Context context = this;
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Every N days");
		builder.setMessage("How many days between each occurence?");		
		builder.setView(editText);
		builder.setCancelable(true);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{				
				int year = 1900 + frequencyOrigin.getYear();
				int month = frequencyOrigin.getMonth();
				int day = frequencyOrigin.getDate();
				
				DatePickerDialog datePickerDialog = 
						new DatePickerDialog(context, onDateSetListener, year, month, day);
				
				datePickerDialog.setCancelable(false);
				datePickerDialog.setMessage("Pick a starting date.");
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
				dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(enabled);	
				
				if(!enabled)
					editText.setError(getString(R.string._msg_drug_frequency_gt_2));	
				else
					editText.setError(null);			
			}
		});
		
		dialog.show();	
	}
	
	private void handleWeekdayFrequency()
	{
		final boolean[] checkedItems;
		long frequencyArg = 0;
		
		if(mDrug.getFrequency() != Drug.FREQ_WEEKDAYS)
		{
			mDrug.setFrequency(Drug.FREQ_WEEKDAYS);
			checkedItems = new boolean[Constants.LONG_WEEK_DAY_NAMES.length];
		}
		else
		{
			frequencyArg = mDrug.getFrequencyArg();
			checkedItems = SimpleBitSet.toBooleanArray(frequencyArg, Constants.LONG_WEEK_DAY_NAMES.length);
			
			if(frequencyArg == 0)
			{
				// check the current weekday if none are selected
				final int weekday = DateTime.now().get(Calendar.DAY_OF_WEEK);
				final int index = CollectionUtils.indexOf(weekday, Constants.WEEK_DAYS);				
				checkedItems[index] = true;
				frequencyArg |= 1 << index;
			}
		}	
		
		final SimpleBitSet bitSet = new SimpleBitSet(frequencyArg);
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Weekydays");
		builder.setMultiChoiceItems(Constants.LONG_WEEK_DAY_NAMES, checkedItems, new OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked)
			{
				bitSet.set(which, isChecked);
				
				final Button positiveButton = ((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
				positiveButton.setEnabled(bitSet.longValue() != 0);
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				mDrug.setFrequencyArg(bitSet.longValue());
				updatePreferences();
			}
		});
		
		builder.show();
	}
	
	private String getWeekdayFrequencySummary(long frequencyArgs)
	{
		final LinkedList<String> weekdays = new LinkedList<String>();
		
		for(int i = 0; i != 7; ++i)
		{
			if((frequencyArgs & (1 << i)) != 0)
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
