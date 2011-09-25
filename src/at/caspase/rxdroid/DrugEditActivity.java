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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.preferences.DosePreference;
import at.caspase.rxdroid.preferences.DrugNamePreference;
import at.caspase.rxdroid.preferences.FractionPreference;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

/**
 * Edit a drug's database entry.
 * @author Joseph Lehner
 *
 */

public class DrugEditActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener
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
							Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
						}

						finish();
					}
				};

				builder.setPositiveButton("Save", onClickListener);
				builder.setNegativeButton("Discard", onClickListener);

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
		boolean postponePrefUpdate = false;

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
				postponePrefUpdate = true;

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setCancelable(false);

				switch(frequency)
				{
					case Drug.FREQ_EVERY_OTHER_DAY:
						initFrequencyEveryOtherDayDialog(builder);
						break;

					case Drug.FREQ_WEEKLY:
						initFrequencyWeeklyDialog(builder);
						break;

					default:
						throw new IllegalStateException("Invalid frequency value");
				}

				builder.show();
			}

			mDrug.setFrequency(toInt(newValue));
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

		if(!postponePrefUpdate)
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

		initializePreferences();
	}

	private void initializePreferences()
	{
		mDrugName.setInitialName(mDrug.getName());

		for(DosePreference dosePref : mDosePrefs)
			dosePref.setDrug(mDrug);

		mFreqPreference.setValueIndex(mDrug.getFrequency());
		mCurrentSupply.setValue(mDrug.getCurrentSupply());
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

			case Drug.FREQ_EVERY_OTHER_DAY:
				Date next = DateTime.tomorrow();
				if(!mDrug.hasDoseOnDate(next))
					next = new Date(next.getTime() + at.caspase.rxdroid.util.Constants.MILLIS_PER_DAY);
				summary = getString(R.string._msg_freq_every_other, next);
				break;

			case Drug.FREQ_WEEKLY:
				int dayOfWeek = (int) mDrug.getFrequencyArg();
				summary = getString(R.string._msg_freq_weekly, DateUtils.getDayOfWeekString(dayOfWeek, DateUtils.LENGTH_LONG));
				break;

			default:
				throw new IllegalStateException("Invalid frequency value");
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

	private void initFrequencyEveryOtherDayDialog(AlertDialog.Builder builder)
	{
		builder.setTitle("Select a starting day");
		builder.setItems(R.array.frequency_every_other_day, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Date today = DateTime.today();

				if(which == 0)
					mDrug.setFrequencyArg(today.getTime());
				else
					mDrug.setFrequencyArg(today.getTime() + Constants.MILLIS_PER_DAY);

				updatePreferences();
			}
		});
	}

	private void initFrequencyWeeklyDialog(AlertDialog.Builder builder)
	{
		builder.setItems(Constants.WEEK_DAY_NAMES, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				mDrug.setFrequencyArg(Constants.WEEK_DAYS[which]);
				updatePreferences();
			}
		});
	}

	private static int toInt(Object string) {
		return Integer.parseInt((String) string, 10);
	}
}
