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

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import android.annotation.TargetApi;
import android.support.v7.app.AlertDialog;
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
import android.preference.PreferenceFragment;
import android.support.v4.view.MenuItemCompat;

import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import org.joda.time.LocalDate;

import at.jclehner.androidutils.AdvancedDialogPreference;
import at.jclehner.androidutils.Reflect;
import at.jclehner.androidutils.otpm.AdvancedDialogPreferenceController;
import at.jclehner.androidutils.otpm.CheckboxPreferenceController;
import at.jclehner.androidutils.otpm.DialogPreferenceController;
import at.jclehner.androidutils.otpm.ListPreferenceWithIntController;
import at.jclehner.androidutils.otpm.OTPM;
import at.jclehner.androidutils.otpm.OTPM.CreatePreference;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.preferences.DatePreference;
import at.jclehner.rxdroid.preferences.DosePreference;
import at.jclehner.rxdroid.preferences.DrugNamePreference2;
import at.jclehner.rxdroid.preferences.FractionPreference;
import at.jclehner.rxdroid.ui.DatePickerDialog;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.SimpleBitSet;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

/**
 * Edit a drug's database entry.
 * @author Joseph Lehner
 *
 */
public class DrugEditFragment extends PreferenceFragment implements OnPreferenceClickListener
{
	private static final int MENU_DELETE = 0;

	//private static final String ARG_DRUG = "drug";

	private static final String TAG = DrugEditFragment.class.getSimpleName();
	private static final boolean LOGV = true;

	private DrugWrapper mWrapper;
	private int mDrugHash;

	// if true, we're editing an existing drug; if false, we're adding a new one
	private boolean mIsEditing;

	private boolean mFocusOnCurrentSupply = false;

	public void onBackPressed()
	{
		final Intent intent = getActivity().getIntent();
		final String action = intent.getAction();

		final Drug drug = mWrapper.get();
		final String drugName = drug.getName();

		if(drugName == null || drugName.length() == 0)
		{
			showDrugDiscardDialog();
			return;
		}

		if(Intent.ACTION_EDIT.equals(action))
		{
			if(mDrugHash != drug.hashCode())
			{
				if(LOGV) Util.dumpObjectMembers(TAG, Log.VERBOSE, drug, "drug 2");

				showSaveChangesDialog();
				return;
			}
		}
		else if(Intent.ACTION_INSERT.equals(action))
		{
			Database.create(drug, 0);
			Toast.makeText(getActivity(), getString(R.string._toast_saved), Toast.LENGTH_SHORT).show();
		}

		getActivity().finish();
	}

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		if(preference.getKey().equals("delete"))
		{
			showDrugDeleteDialog();
			return true;
		}

		return false;
	}

	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.empty);
		setHasOptionsMenu(true);

		//final ListView list = getListView();
		//if(list != null)
		//	list.setSelector(Theme.getResourceAttribute(R.attr.selectableItemBackground));
	}

	@Override
	public void onResume()
	{
		super.onResume();
		Intent intent = getActivity().getIntent();
		String action = intent.getAction();

		Drug drug = null;

		mWrapper = new DrugWrapper();
		mFocusOnCurrentSupply = false;

		if(Intent.ACTION_EDIT.equals(action))
		{
			final int drugId = intent.getIntExtra(DrugEditActivity2.EXTRA_DRUG_ID, -1);
			if(drugId == -1)
				throw new IllegalStateException("ACTION_EDIT requires EXTRA_DRUG_ID");

			drug = Drug.get(drugId);

			if(LOGV) Util.dumpObjectMembers(TAG, Log.VERBOSE, drug, "drug");

			mWrapper.set(drug);
			mDrugHash = drug.hashCode();
			mIsEditing = true;

			if(intent.getBooleanExtra(DrugEditActivity2.EXTRA_FOCUS_ON_CURRENT_SUPPLY, false))
				mFocusOnCurrentSupply = true;

			setActivityTitle(drug.getName());
		}
		else if(Intent.ACTION_INSERT.equals(action))
		{
			mIsEditing = false;
			mWrapper.set(new Drug());
			setActivityTitle(R.string._title_new_drug);
		}
		else
			throw new IllegalArgumentException("Unhandled action " + action);

		if(mWrapper.refillSize == 0)
			mWrapper.currentSupply = Fraction.ZERO;

		if(mIsEditing && mWrapper.repeatOrigin != null && DateTime.getOffsetFromMidnight(mWrapper.repeatOrigin) != 0)
		{
			if(BuildConfig.DEBUG)
			{
				Toast.makeText(getActivity(), "repeatOrigin=" + mWrapper.repeatOrigin,
						Toast.LENGTH_LONG).show();
			}

			Log.i(TAG, "Drug has invalid repeatOrigin: " + mWrapper.repeatOrigin);

			if(!fixInvalidRepeatOrigin(drug))
				return;

			mWrapper.repeatOrigin = drug.getRepeatOrigin();
			mDrugHash = drug.hashCode();
		}

		updatePreferenceHierarchy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		if(mIsEditing && !getActivity().getIntent().getBooleanExtra(DrugEditActivity2.EXTRA_DISALLOW_DELETE, false))
		{
			MenuItem item = menu.add(0, MENU_DELETE, 0, R.string._title_delete)
					.setIcon(R.drawable.ic_action_delete_white);

			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		final int itemId = item.getItemId();

		if(itemId == android.R.id.home)
		{
			// We can do this since this Activity can only be launched from
			// DrugListActivity at the moment.
			onBackPressed();
			return true;
		}
		else if(itemId == MENU_DELETE)
		{
			showDrugDeleteDialog();
			return true;
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

	private void setActivityTitle(String title) {
		((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
	}

	private void setActivityTitle(int resId) {
		((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(resId);
	}

	private boolean fixInvalidRepeatOrigin(final Drug drug)
	{
		final LocalDate dateOnly = LocalDate.fromDateFields(drug.getRepeatOrigin());

		if(fixInvalidRepeatOrigin(drug, dateOnly))
			return true;
		else if(fixInvalidRepeatOrigin(drug, drug.getNextScheduledDate(LocalDate.now())))
			return true;
		else
		{
			final DatePickerDialog dialog = new DatePickerDialog(
					getActivity(), dateOnly, new DatePickerDialog.OnDateSetListener() {
				@Override
				public void onDateSet(DatePickerDialog dialog, LocalDate date)
				{
					updateRepeatOrigin(drug, date);
					updatePreferenceHierarchy();
				}
			});
			dialog.setTitle(R.string._title_repetition_origin);
			dialog.setCancelable(false);
			dialog.show();
			return false;
		}
	}

	private boolean fixInvalidRepeatOrigin(Drug drug, LocalDate reference)
	{
		if(reference == null)
			return false;

		final Date invalidRepeatOrigin = drug.getRepeatOrigin();

		for(int i = 0; i != 2; ++i)
		{
			final LocalDate date = reference.plusDays(i);
			if(drug.hasDoseOnDate(date.toDate()))
			{
				updateRepeatOrigin(drug, date);
				Log.i(TAG, "Updated invalid repeatOrigin: " + drug.getRepeatOrigin());
				return true;
			}
		}

		try
		{
			Reflect.setFieldValue(Drug.class.getDeclaredField("repeatOrigin"), drug, invalidRepeatOrigin);
		}
		catch(NoSuchFieldException e)
		{
			throw new WrappedCheckedException(e);
		}

		return false;
	}

	private void updateRepeatOrigin(Drug drug, LocalDate repeatOrigin)
	{
		final Date scheduleBegin = drug.getLastScheduleUpdateDate();
		drug.setRepeatOrigin(repeatOrigin.toDate());
		drug.setLastScheduleUpdateDate(scheduleBegin);

		Database.update(drug);
	}

	private void updatePreferenceHierarchy()
	{
		OTPM.mapToPreferenceHierarchy(getPreferenceScreen(), mWrapper);
		getPreferenceScreen().setOnPreferenceChangeListener(mListener);

		if(!mIsEditing)
		{
			final Preference p = findPreference("active");
			if(p != null)
				p.setEnabled(false);
		}

		if(mWrapper.refillSize == 0)
		{
			Preference p = findPreference("currentSupply");
			if(p != null)
				p.setEnabled(false);

			p = findPreference("expiryDate");
			if(p != null)
				p.setEnabled(false);
		}

		getActivity().invalidateOptionsMenu();
	}

	private void showDrugDeleteDialog()
	{
		final String message = getString(R.string._title_delete_item, mWrapper.get().getName())
				+ " " + getString(R.string._msg_delete_drug);

		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());

		ab.setIcon(android.R.drawable.ic_dialog_alert);
		ab.setMessage(message);
		ab.setNegativeButton(android.R.string.no, null);
		ab.setPositiveButton(android.R.string.yes, new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Database.delete(mWrapper.get());
				Toast.makeText(getActivity(), R.string._toast_deleted, Toast.LENGTH_SHORT).show();
				getActivity().finish();
			}
		});

		ab.show();
	}

	private void showDrugDiscardDialog()
	{
		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
		ab.setMessage(R.string._msg_err_empty_drug_name);
		ab.setNegativeButton(android.R.string.cancel, null);
		ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				getActivity().finish();
			}
		});

		ab.show();
	}

	private void showSaveChangesDialog()
	{
		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
		ab.setMessage(R.string._msg_save_drug_changes);

		final DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				if(which == Dialog.BUTTON_POSITIVE)
				{
					Database.update(mWrapper.get());
					Toast.makeText(getActivity(), R.string._toast_saved, Toast.LENGTH_SHORT).show();
				}

				getActivity().finish();
			}
		};

		ab.setNegativeButton(R.string._btn_discard, l);
		ab.setPositiveButton(R.string._btn_save, l);

		ab.show();
	}

	private static class DrugWrapper
	{
		@CreatePreference
		(
			titleResId = R.string._title_drug_name,
			order = 1,
			type = DrugNamePreference2.class,
			controller = DrugNamePreferenceController.class
		)
		private String name;

		@CreatePreference
		(
			titleResId = R.string._title_morning,
			key = "morning",
			categoryResId = R.string._title_intake_schedule,
			order = 3,
			type = DosePreference.class,
			controller = AdvancedDialogPreferenceController.class
		)
		private Fraction doseMorning;

		@CreatePreference
		(
			titleResId = R.string._title_noon,
			key = "noon",
			order = 4,
			type = DosePreference.class,
			controller = AdvancedDialogPreferenceController.class
		)
		private Fraction doseNoon;

		@CreatePreference
		(
			titleResId = R.string._title_evening,
			key = "evening",
			order = 5,
			type = DosePreference.class,
			controller = AdvancedDialogPreferenceController.class
		)
		private Fraction doseEvening;

		@CreatePreference
		(
			titleResId = R.string._title_night,
			key = "night",
			endActiveCategory = true,
			order = 6,
			type = DosePreference.class,
			controller = AdvancedDialogPreferenceController.class
		)
		private Fraction doseNight;

		@CreatePreference
		(
			titleResId = R.string._title_repeat,
			order = 7,
			type = ListPreference.class,
			controller = RepeatModePreferenceController.class,
			fieldDependencies = { "repeatArg", "repeatOrigin" }
		)
		private int repeat;


		@CreatePreference
		(
			titleResId = R.string._title_end,
			order = 8,
			type = DatePreference.class,
			controller = ScheduleEndPreferenceController.class,
			reverseDependencies = { "repeat" },
			fieldDependencies = { "repeatOrigin" }

		)
		private LocalDate scheduleEnd;

		@CreatePreference
		(
			titleResId = R.string._title_on_demand,
			summary = "",
			order = 9,
			type = CheckBoxPreference.class,
			controller = CheckboxPreferenceController.class
		)
		private boolean asNeeded;


		@CreatePreference
		(
			titleResId = R.string._title_refill_size,
			categoryResId = R.string._title_supplies,
			order = 10,
			type = FractionPreference.class,
			controller = RefillSizePreferenceController.class
		)
		private int refillSize;

		@CreatePreference
		(
			titleResId = R.string._title_current_supply,
			order = 11,
			type = CurrentSupplyPreference.class,
			controller = CurrentSupplyPreferenceController.class,
			reverseDependencies = { "morning", "noon", "evening", "night", "refillSize", "repeat", "asNeeded", "scheduleEnd" },
			fieldDependencies = { "repeatArg", "repeatOrigin" }
		)
		private Fraction currentSupply;

		@CreatePreference
		(
			titleResId = R.string._title_expiry_date,
			order = 12,
			type = DatePreference.class,
			controller = ExpiryDatePreferenceController.class,
			reverseDependencies = "refillSize"
		)
		private LocalDate expiryDate;

		@CreatePreference
		(
			titleResId = R.string._title_icon,
			categoryResId = R.string._title_misc,
			order = 13,
			type = ListPreference.class,
			controller = FormPreferenceController.class
		)
		private int form;

		@CreatePreference
		(
			titleResId = R.string._title_per_drug_reminders,
			order = 14,
			type = ListPreference.class,
			controller = NotificationsPreferenceController.class
			//, reverseDependencies = "refillSize"
		)
		private boolean autoAddIntakes;

		@CreatePreference
		(
			titleResId = R.string._title_active,
			summary = "",
			order = 15,
			type = CheckBoxPreference.class,
			controller = CheckboxPreferenceController.class
		)
		private boolean active;

		private int id;

		private long repeatArg;
		private Date repeatOrigin;
		private int sortRank;
		private Patient patient;
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
		private Date lastScheduleUpdateDate;

		private Drug original;

		public void set(Drug drug)
		{
			original = drug;

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
			sortRank = drug.getSortRank();
			autoAddIntakes = drug.hasAutoDoseEvents();
			lastAutoIntakeCreationDate = drug.getLastAutoDoseEventCreationDate();
			lastScheduleUpdateDate = drug.getLastScheduleUpdateDate();
			patient = drug.getPatient();
			scheduleEnd = drug.getScheduleEndDate();
			asNeeded = drug.isAsNeeded();
			expiryDate = drug.getExpiryDate();

			name = drug.getName();
			form = drug.getIcon();
		}

		public Drug get()
		{
			Drug drug = new Drug();
			drug.setId(id);
			drug.setName(name);
			drug.setIcon(form);
			drug.setActive(active);
			drug.setComment(comment);
			drug.setCurrentSupply(currentSupply);
			drug.setRefillSize(refillSize);
			drug.setRepeatMode(repeat);
			drug.setSortRank(sortRank);
			drug.setLastAutoDoseEventCreationDate(lastAutoIntakeCreationDate);
			drug.setHasAutoDoseEvents(autoAddIntakes);
			drug.setPatient(patient);
			drug.setScheduleEndDate(scheduleEnd);
			drug.setAsNeeded(asNeeded);
			drug.setExpiryDate(expiryDate);

			final Fraction doses[] = { doseMorning, doseNoon, doseEvening, doseNight };

			for(int i = 0; i != doses.length; ++i)
				drug.setDose(Constants.DOSE_TIMES[i], doses[i]);

			drug.setRepeatArg(repeatArg);
			drug.setRepeatOrigin(repeatOrigin);

			drug.setLastScheduleUpdateDate(isScheduleEqual(drug, original) ?
					lastScheduleUpdateDate : DateTime.today());

			return drug;
		}

		private boolean isScheduleEqual(Drug drug1, Drug drug2)
		{
			if(drug1.getSchedules().size() != 0 || drug2.getSchedules().size() != 0)
				throw new UnsupportedOperationException();

			if(drug1.isAsNeeded() != drug2.isAsNeeded())
				return false;

			if(drug1.getRepeatMode() != drug2.getRepeatMode())
				return false;

			if(drug1.getRepeatArg() != drug2.getRepeatArg())
				return false;

			if(!Util.equalsIgnoresNull(drug1.getRepeatOrigin(), drug2.getRepeatOrigin()))
				return false;

			final Fraction schedule1[] = drug1.getSimpleSchedule();
			final Fraction schedule2[] = drug2.getSimpleSchedule();

			for(int i = 0; i != schedule1.length; ++i)
			{
				if(!Util.equalsIgnoresNull(schedule1[i], schedule2[i]))
					return false;
			}

			return true;
		}
	}

	private static class DrugNamePreferenceController extends AdvancedDialogPreferenceController
	{
		public DrugNamePreferenceController() {}

		@Override
		public boolean updatePreference(AdvancedDialogPreference preference, Object newValue)
		{
			try
			{
				((AppCompatActivity) preference.getContext()).getSupportActionBar().setTitle((String) newValue);
			}
			catch(ClassCastException e)
			{
				e.printStackTrace();
			}

			return super.updatePreference(preference, newValue);
		}
	}

	public static class RepeatModePreferenceController extends ListPreferenceWithIntController
	{
		private ListPreference mPref;
		private Context mContext;

		public RepeatModePreferenceController() {
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

			final NumberPicker picker = new NumberPicker(mContext);
			picker.setMinValue(2);
			picker.setMaxValue(365);
			picker.setWrapSelectorWheel(false);
			picker.setValue((int) repeatArg);
			picker.setGravity(Gravity.CENTER_HORIZONTAL);

			picker.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));


			final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(R.string._title_every_n_days);
			builder.setMessage(R.string._msg_every_n_days_distance);
			builder.setView(picker);
			builder.setCancelable(true);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					final long repeatArg = picker.getValue();
					showRepeatOriginDateDialog(Drug.REPEAT_EVERY_N_DAYS, repeatOrigin, repeatArg);
				}
			});

			builder.show();
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
					final long repeatArg = bitset.longValue();

					setFieldValue(Drug.REPEAT_WEEKDAYS);
					setFieldValue("repeatArg", repeatArg);

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
			final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {

				@Override
				public void onDateSet(DatePickerDialog dialog, LocalDate date)
				{
					setFieldValue(repeatMode);
					setFieldValue("repeatOrigin", date.toDate());
					setFieldValue("repeatArg", repeatArg);

					updateSummary();
					notifyForwardDependencies();
				}
			};



			final DatePickerDialog datePickerDialog = new DatePickerDialog(mContext,
					LocalDate.fromDateFields(repeatOrigin), onDateSetListener);

			datePickerDialog.setTitle(R.string._title_repetition_origin);
			datePickerDialog.setCancelable(false);
			datePickerDialog.setMaxDate((LocalDate) getFieldValue("scheduleEnd"));
			datePickerDialog.show();
		}

		public static String getSummary(Context context, int repeatMode, long repeatArg, Date repeatOrigin)
		{
			if(repeatMode == Drug.REPEAT_EVERY_N_DAYS)
			{
				// FIXME change to next occurence
				return context.getString(
						R.string._msg_freq_every_n_days,
						repeatArg,
						DateTime.toNativeDate(repeatOrigin)
				);
			}
			else if(repeatMode == Drug.REPEAT_WEEKDAYS)
				return getWeekdayRepeatSummary(context, repeatArg);
			else if(repeatMode == Drug.REPEAT_21_7)
			{
				return context.getString(
						R.string._msg_freq_21days_on_7days_off,
						DateTime.toNativeDate(repeatOrigin)
				);
			}
			else
				return null;
		}

		private void updateSummary()
		{
			final String summary = getSummary(mContext, getFieldValue(),
					(long) getFieldValue("repeatArg"), (Date) getFieldValue("repeatOrigin"));
			if (summary != null) {
				mPref.setSummary(summary);
			}
		}

		private static String getWeekdayRepeatSummary(Context context, long repeatArg)
		{
			final LinkedList<String> weekdays = new LinkedList<String>();

			for(int i = 0; i != 7; ++i)
			{
				if((repeatArg & (1 << i)) != 0)
					weekdays.add(Constants.SHORT_WEEK_DAY_NAMES[i]);
			}

			if(weekdays.isEmpty())
				return context.getString(R.string._summary_intake_never);

			StringBuilder sb = new StringBuilder(weekdays.get(0));

			for(int i = 1; i != weekdays.size(); ++i)
				sb.append(", " + weekdays.get(i));

			return sb.toString();
		}
	}

	@SuppressWarnings("rawtypes")
	private static class CurrentSupplyPreferenceController extends AdvancedDialogPreferenceController
	{
		private Context mContext;
		private Object mValue;

		@SuppressWarnings({ "unused" })
		public CurrentSupplyPreferenceController() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void initPreference(AdvancedDialogPreference preference, Object fieldValue)
		{
			super.initPreference(preference, fieldValue);
			mContext = preference.getContext().getApplicationContext();

			((CurrentSupplyPreference) preference).setRefillSize((Integer) getFieldValue("refillSize"));
		}

		@Override
		public boolean updatePreference(AdvancedDialogPreference preference, Object newValue)
		{
			super.updatePreference(preference, newValue);
			//preference.setSummary(getSummary());
			//preference.notifyDependencyChange(false);
			return true;
		}

		@Override
		public void updateSummary(AdvancedDialogPreference preference, Object newValue)
		{
			preference.setSummary(getSummary(newValue));
			mValue = newValue;
		}

		@Override
		public void onDependencyChange(AdvancedDialogPreference preference, String depKey, Object newPrefValue)
		{
			preference.setSummary(getSummary(mValue));
			if("refillSize".equals(depKey))
			{
				final int refillSize = (Integer) newPrefValue;
				final CurrentSupplyPreference p = (CurrentSupplyPreference) preference;

				Log.d(TAG, "onDependencyChange: newPrefValue=" + newPrefValue);

				p.setRefillSize(refillSize);
				p.setEnabled(refillSize > 0);
			}
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

			if(!drug.isAsNeeded() && !drug.hasNoDoses())
			{
				final int currentSupplyDays = Math.max(Entries.getSupplyDaysLeftForDrug(drug, null), 0);
				final LocalDate supplyEnd = LocalDate.now().plusDays(currentSupplyDays);
				final LocalDate scheduleEnd = drug.getScheduleEndDate();

				if(scheduleEnd == null || supplyEnd.isBefore(scheduleEnd))
					return mContext.getString(R.string._msg_supply, currentSupply, DateTime.toNativeDate(supplyEnd));
			}

			return currentSupply.toString();
		}
	}

	@SuppressWarnings("rawtypes")
	private static class RefillSizePreferenceController extends AdvancedDialogPreferenceController
	{
		@SuppressWarnings("unused")
		public RefillSizePreferenceController() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void initPreference(AdvancedDialogPreference preference, Object fieldValue)
		{
			super.initPreference(preference, new Fraction((Integer) fieldValue));
			((FractionPreference) preference).disableFractionInputMode(true);
		}

		@Override
		public void updateSummary(AdvancedDialogPreference preference, Object newValue)
		{
			final int value = (Integer) newValue;

			if(value != 0)
				preference.setSummary(Integer.toString(value));
			else
				preference.setSummary(R.string._summary_not_available);
		}

		@Override
		public Object toFieldType(Object prefValue) {
			return ((Fraction) prefValue).intValue();
		}
	}

	public static class FormPreferenceController extends ListPreferenceWithIntController
	{
		public FormPreferenceController() {
			super(R.array.drug_forms);
		}
	}

	public static class CheckableDatePreferenceController extends AdvancedDialogPreferenceController
	{
		@Override
		public void initPreference(AdvancedDialogPreference preference, Object fieldValue)
		{
			super.initPreference(preference, fieldValue);
			preference.setCheckable(true);

			final LocalDate minDate = LocalDate.fromDateFields(
					Settings.getDoseTimeInfo().activeDate()).plusDays(1);

			((DatePreference) preference).setMinDate(minDate);
		}

		@Override
		public void updateSummary(AdvancedDialogPreference preference, Object newValue)
		{
			if(newValue != null)
				preference.setSummary(DateTime.toNativeDate(((LocalDate) newValue).toDate()));
			else
				preference.setSummary(R.string._summary_not_available);
		}
	}

	public static class ExpiryDatePreferenceController extends CheckableDatePreferenceController
	{
		@Override
		public void onDependencyChange(AdvancedDialogPreference preference, String depKey, Object newPrefValue)
		{
			super.onDependencyChange(preference, depKey, newPrefValue);
			if("refillSize".equals(depKey))
			{
				preference.setEnabled(0 != (int) newPrefValue);
				if(!preference.isEnabled())
				{
					setFieldValue(null);
					preference.setValue(null);
				}
			}
		}
	}

	public static class ScheduleEndPreferenceController extends CheckableDatePreferenceController
	{
		@Override
		public void onDependencyChange(AdvancedDialogPreference preference, String depKey, Object newPrefValue)
		{
			super.onDependencyChange(preference, depKey, newPrefValue);
			((DatePreference) preference).setMinDate(DateTime.fromDateFields((Date) getFieldValue("repeatOrigin")));
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
			final DrugSupplyEditFragment.Dialog d =
					new DrugSupplyEditFragment.Dialog(getContext(), getDialogValue(), mRefillSize, this);

			d.setTitle(getDialogTitle());
			d.setIcon(getDialogIcon());

			return d;
		}
	}

	private static class NotificationsPreferenceController extends DialogPreferenceController<ListPreference, Boolean>
	{
		private static final int NOTIFY_ALL = 0;
		private static final int NOTIFY_SUPPLIES_ONLY = 1;
		private String[] mEntries;

		@SuppressWarnings("unused")
		public NotificationsPreferenceController() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void initPreference(ListPreference preference, Boolean fieldValue)
		{
			super.initPreference(preference, fieldValue);

			if(mEntries == null)
			{
				final Resources r = preference.getContext().getResources();
				mEntries = r.getStringArray(R.array.drug_notifications);
			}

			preference.setEntries(mEntries);
			Util.populateListPreferenceEntryValues(preference);
			preference.setValueIndex(fieldValue ? NOTIFY_SUPPLIES_ONLY : NOTIFY_ALL);
			preference.setDialogTitle(preference.getTitle());

//			preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//
//				@Override
//				public boolean onPreferenceClick(Preference preference)
//				{
//					if(Settings.containsStringSetEntry(Settings.Keys.DISPLAYED_ONCE, entry))
//
//
//
//
//					// TODO Auto-generated method stub
//					return false;
//				}
//			});
		}

		@Override
		public void updateSummary(ListPreference preference, Boolean newValue)
		{
			if(newValue)
			{
				final Context c = preference.getContext();

				preference.setSummary(mEntries[NOTIFY_SUPPLIES_ONLY] +
						"  \u2014 " + c.getString(R.string._title_auto_dose_events));
			}
			else
				preference.setSummary(mEntries[NOTIFY_ALL]);
		}

		@Override
		public Boolean toFieldType(Object prefValue)
		{
			final int i = Integer.parseInt((String) prefValue);
			return i == NOTIFY_SUPPLIES_ONLY;
		}

		@Override
		public void onDependencyChange(ListPreference preference, String depKey, Object newPrefValue)
		{
			if("refillSize".equals(depKey))
			{
				final int refillSize = (int) newPrefValue;

				if(refillSize == 0)
				{
					setFieldValue(false);
					updateSummary(preference, false);
					preference.setValueIndex(0);
				}

				preference.setEnabled(refillSize > 0);
			}
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
