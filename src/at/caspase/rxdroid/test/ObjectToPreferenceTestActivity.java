package at.caspase.rxdroid.test;

import java.util.Date;

import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import at.caspase.androidutils.otpm.CheckboxPreferenceHelper;
import at.caspase.androidutils.otpm.ListPreferenceWithIntHelper;
import at.caspase.androidutils.otpm.MyDialogPreferenceHelper;
import at.caspase.androidutils.otpm.OTPM;
import at.caspase.androidutils.otpm.OTPM.MapToPreference;
import at.caspase.androidutils.otpm.OTPM.ObjectWrapper;
import at.caspase.rxdroid.preferences.DosePreference;
import at.caspase.rxdroid.preferences.DrugNamePreference2;
import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.GlobalContext;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Schedule;

public class ObjectToPreferenceTestActivity extends PreferenceActivity
{
	private static final String TAG = ObjectToPreferenceTestActivity.class.getName();

	public static class FormPreferenceHelper extends ListPreferenceWithIntHelper
	{
		public FormPreferenceHelper() {
			super(GlobalContext.get(), R.array.drug_forms);
		}
	}

	public static class RepeatModePreferenceHelper extends ListPreferenceWithIntHelper
	{
		public RepeatModePreferenceHelper() {
			super(GlobalContext.get(), R.array.drug_repeat);
		}
	}


	@SuppressWarnings("unused")
	public static class DrugWrapper extends ObjectWrapper<Drug>
	{
		private Drug mDrug;

		private int id;

		@MapToPreference
		(
			title = "Drug name",
			order = 1,
			type = DrugNamePreference2.class,
			helper = MyDialogPreferenceHelper.class
		)
		private String name;

		@MapToPreference
		(
			title = "Form",
			order = 2,
			type = ListPreference.class,
			helper = FormPreferenceHelper.class
		)
		private int form;

		private int refillSize;

		private Fraction currentSupply;

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
			//title = "Active",
			titleResId = R.string._title_active,
			//summary = "Uncheck to deactivate this drug",
			//category = "Miscellaneous",
			categoryResId = R.string._title_misc,
			//endActiveCategory = true,
			order = 8,
			type = CheckBoxPreference.class,
			helper = CheckboxPreferenceHelper.class
		)
		private boolean active;

		private long repeatArg;

		private Date repeatOrigin;

		private int sortRank;

		private Schedule schedule;

		private String comment;

		private Preference mPref;



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
			//drug.setActive(active);
			//drug.setComment(comment);
			//drug.setCurrentSupply(currentSupply);
			//drug.setRepeat(repeat);
			//drug.set

			return drug;
		}
	}

	private DrugWrapper mWrapper;

	@Override
	protected void onResume()
	{
		super.onResume();

		Drug drug = Database.getAll(Drug.class).get(0);
		mWrapper = new DrugWrapper();
		mWrapper.set(drug);

		addPreferencesFromResource(R.xml.empty);
		OTPM.mapToPreferenceScreen(getPreferenceScreen(), mWrapper);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add("Dump")
				.setIcon(android.R.drawable.ic_menu_info_details)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						Log.d(TAG, "DRUG: " + mWrapper.get() + ", form=" + mWrapper.get().getForm());
						return true;
					}
				});

		return super.onCreateOptionsMenu(menu);
	}
}
