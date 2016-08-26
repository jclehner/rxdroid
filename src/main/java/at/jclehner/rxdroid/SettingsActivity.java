package at.jclehner.rxdroid;

import android.annotation.TargetApi;
import android.support.v7.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.app.Fragment;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import net.lingala.zip4j.exception.ZipException;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.db.SchedulePart;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

public class SettingsActivity extends AppCompatActivity
{
	private static final String EXTRA_PREFERENCE_SCREEN = "rxdroid:preference_screen";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_activity);

		if(savedInstanceState == null)
		{
			final int resId;

			String prefScreenResIdStr = getIntent().getStringExtra(EXTRA_PREFERENCE_SCREEN);
			if(prefScreenResIdStr != null)
			{
				if(prefScreenResIdStr.charAt(0) == '@')
					prefScreenResIdStr = prefScreenResIdStr.substring(1);

				resId = getResources().getIdentifier(prefScreenResIdStr, null, getApplicationInfo().packageName);
			}
			else
				resId = R.xml.settings;

			final SettingsFragment f = SettingsFragment.newInstance(resId);
			getFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
		}
	}

	@Override
	protected void onResume()
	{
		Components.onResumeActivity(this, 0);
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		final Fragment fragment = getFragmentManager().findFragmentById(android.R.id.content);
		fragment.onActivityResult(requestCode, resultCode, data);
	}

	public static class SettingsFragment extends PreferenceFragment implements
			SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener
	{
		private static final String TAG = SettingsFragment.class.getSimpleName();

		private static final String ARG_RESOURCE = "rxdroid:preference_resource";

		private static final String[] KEEP_DISABLED = {
			Settings.Keys.VERSION, Settings.Keys.DB_STATS
		};

		private static final String[] REGISTER_CLICK_LISTENER = {
			Settings.Keys.LICENSES,
			Settings.Keys.VERSION,
			"db_export"
		};

		private static final String[] REGISTER_CHANGE_LISTENER = {
			Settings.Keys.THEME_IS_DARK,
			Settings.Keys.NOTIFICATION_LIGHT_COLOR,
			Settings.Keys.LOW_SUPPLY_THRESHOLD,
			Settings.Keys.LANGUAGE
		};

		public static SettingsFragment newInstance(int preferenceResId)
		{
			final Bundle args = new Bundle();
			args.putInt(ARG_RESOURCE, preferenceResId);

			final SettingsFragment f = new SettingsFragment();
			f.setArguments(args);

			return f;
		}

		@TargetApi(11)
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			final PreferenceManager pm = getPreferenceManager();
			pm.setSharedPreferencesMode(MODE_MULTI_PROCESS);
			pm.setSharedPreferencesName(Settings.getDefaultSharedPreferencesName(getActivity()));

			addPreferencesFromResource(getArguments().getInt(ARG_RESOURCE));

			Settings.registerOnChangeListener(this);

			for(Preference p : getPreferences(REGISTER_CHANGE_LISTENER))
				p.setOnPreferenceChangeListener(this);

			for(Preference p : getPreferences(REGISTER_CLICK_LISTENER))
				p.setOnPreferenceClickListener(this);

			Preference p = findPreference(Settings.Keys.VERSION);
			if(p != null)
			{
				final int format = BuildConfig.DEBUG ? Version.FORMAT_FULL : Version.FORMAT_SHORT;
				final StringBuilder sb = new StringBuilder(Version.get(format));

				if(BuildConfig.DEBUG)
					sb.append(" (DEV)");

				sb.append(", DB v" + DatabaseHelper.DB_VERSION);

				if(BuildConfig.DEBUG)
				{
					try
					{
						final String apkModDate = new Date(new File(getActivity().getPackageCodePath()).lastModified()).toString();
						sb.append("\n(" + apkModDate + ")");
					}
					catch(NullPointerException e)
					{
						// eat
					}
				}

				sb.append("\n" +
						"Copyright (C) 2011-2015 Joseph Lehner\n" +
						"<joseph.c.lehner@gmail.com>");

				final String translator = getString(R.string.translator);
				if(!translator.equals("builtin"))
				{
					final Locale l = Locale.getDefault();

					sb.append("\n\n");
					sb.append(Util.capitalize(l.getDisplayLanguage(l))  + ": " + translator);
				}

				p.setSummary(sb.toString());
			}

			p = findPreference(Settings.Keys.HISTORY_SIZE);
			if(p != null)
				Util.populateListPreferenceEntryValues(p);

			p = findPreference(Settings.Keys.DONATE);
			if(p != null)
			{
				final Intent intent = new Intent(Intent.ACTION_VIEW);
				if(!Util.wasInstalledViaGooglePlay())
				{

					intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=joseph%2ec%2elehner%40gmail%2ecom" +
							"&lc=AT&item_name=RxDroid&amount=5%2e00&currency_code=EUR&button_subtype=services" +
							"&bn=PP%2dBuyNowBF%3abtn_buynowCC_LG%2egif%3aNonHosted"));
				}
				else
					intent.setData(Uri.parse("https://github.com/jclehner/rxdroid/blob/master/README.md"));

				p.setIntent(intent);
			}

			p = findPreference(Settings.Keys.DB_STATS);
			if(p != null)
			{
				final long millis = Database.getLoadingTimeMillis();
				final String str = new Formatter((Locale) null).format("%1.3fs", millis / 1000f).toString();
				p.setSummary(getString(R.string._msg_db_stats, str));
			}

			removeDisabledPreferences(getPreferenceScreen());
			setPreferenceListeners();

			if(Settings.getBoolean(Settings.Keys.USE_SAFE_MODE, false))
			{
				p = findPreference(Settings.Keys.SKIP_DOSE_DIALOG);
				if(p != null)
					p.setEnabled(false);
			}

			if(!BuildConfig.DEBUG)
			{
				p = findPreference("prefscreen_development");
				if(p != null)
					getPreferenceScreen().removePreference(p);
			}
			else
				setupDebugPreferences();
		}

		@Override
		public void onResume()
		{
			super.onResume();
			updateLowSupplyThresholdPreferenceSummary();

			((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getPreferenceScreen().getTitle());
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
		{
			if(Settings.Keys.LOW_SUPPLY_THRESHOLD.equals(key))
				updateLowSupplyThresholdPreferenceSummary();
			else if(Settings.Keys.HISTORY_SIZE.equals(key))
			{
				if(Settings.getStringAsInt(Settings.Keys.HISTORY_SIZE, -1) >= Settings.Enums.HISTORY_SIZE_6M)
				{
					final Context ctx = getActivity();
					if(ctx != null)
						Toast.makeText(ctx, R.string._toast_large_history_size, Toast.LENGTH_LONG).show();
				}
			}
			else if(Settings.Keys.USE_SAFE_MODE.equals(key))
			{
				final boolean useSafeMode = sharedPreferences.getBoolean(key, false);
				findPreference(Settings.Keys.SKIP_DOSE_DIALOG).setEnabled(!useSafeMode);
				if(useSafeMode)
					sharedPreferences.edit().putBoolean(Settings.Keys.SKIP_DOSE_DIALOG, false).commit();

				NotificationReceiver.cancelAllNotifications();
			}
			else if(Settings.Keys.LAST_MSG_HASH.equals(key)
					|| Settings.Keys.UNSNOOZE_DATE.equals(key)
					|| "refill_reminder_snooze_drugs".equals(key))
			{
				return;
			}

			NotificationReceiver.rescheduleAlarmsAndUpdateNotification(true);
		}

		@Override
		public boolean onPreferenceClick(Preference preference)
		{
			final String key = preference.getKey();

			if(Settings.Keys.LICENSES.equals(key))
			{
				showLicensesDialog();
				return true;
			}
			else if(Settings.Keys.VERSION.equals(key))
			{
				final Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.fromParts("mailto", "josephclehner+rxdroid-feedback@gmail.com", null));
				intent.putExtra(Intent.EXTRA_SUBJECT, "RxDroid");

				try
				{
					startActivity(intent);
				}
				catch(ActivityNotFoundException e)
				{
					// Happens if no mail client is installed
				}

				return true;
			}
			else if("db_export".equals(key)) {
				CsvExport.export(getActivity(), Patient.DEFAULT_PATIENT_ID);
			}

			return false;
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			final String key = preference.getKey();

			if(Settings.Keys.THEME_IS_DARK.equals(key))
			{
				Theme.clearAttributeCache();

				final Context context = RxDroid.getContext();

				RxDroid.toastLong(R.string._toast_theme_changed);

				final PackageManager pm = context.getPackageManager();
				final Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());

				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				getActivity().startActivity(intent);
			}
			else if(Settings.Keys.NOTIFICATION_LIGHT_COLOR.equals(key))
			{
				final String value = (String) newValue;
				if(!("".equals(value) || "0".equals(value)))
				{
					if(!Settings.wasDisplayedOnce("custom_led_color"))
					{
						RxDroid.toastLong(R.string._toast_custom_led_color);
						Settings.setDisplayedOnce("custom_led_color");
					}
				}
			}
			else if(Settings.Keys.LOW_SUPPLY_THRESHOLD.equals(key))
			{
				int i;

				try
				{
					i = Integer.parseInt((String) newValue, 10);
				}
				catch(Exception e)
				{
					Log.w(TAG, e);
					return false;
				}

				return i >= 0;
			}

			return true;
		}

		private boolean shouldDisplayDonatePref()
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
			{
				try
				{
					final PackageInfo info = getActivity().getPackageManager()
							.getPackageInfo(getActivity().getPackageName(), 0);

					return ((System.currentTimeMillis() - info.firstInstallTime)
							/ Constants.MILLIS_PER_DAY) > 14;
				}
				catch(PackageManager.NameNotFoundException e)
				{
					return true;
				}
			}

			return true;
		}

		private void showLicensesDialog()
		{
			String license;
			InputStream is = null;

			try
			{
				final AssetManager aMgr = getResources().getAssets();
				is = aMgr.open("licenses.html", AssetManager.ACCESS_BUFFER);

				license = Util.streamToString(is);
			}
			catch(IOException e)
			{
				Log.w(TAG, e);
				license = "Licensed under the GNU GPLv3";
			}
			finally
			{
				Util.closeQuietly(is);
			}

			final WebView wv = new WebView(getActivity());
			wv.loadDataWithBaseURL("file", license, "text/html", null, null);

			final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
			//ab.setTitle(R.string._title_licenses);
			ab.setView(wv);
			ab.setPositiveButton(android.R.string.ok, null);

			ab.show();
		}

		private void updateLowSupplyThresholdPreferenceSummary()
		{
			Preference p = findPreference(Settings.Keys.LOW_SUPPLY_THRESHOLD);
			if(p != null)
			{
				String value = Settings.getString(Settings.Keys.LOW_SUPPLY_THRESHOLD, "10");
				p.setSummary(getString(R.string._summary_min_supply_days, value));
			}
		}

		private void removeDisabledPreferences(PreferenceGroup root)
		{
			if(root == null)
				return;

			final List<Preference> toRemove = new ArrayList<Preference>();

			for(int i = 0; i != root.getPreferenceCount(); ++i)
			{
				final Preference p = root.getPreference(i);

				if(p == null || CollectionUtils.contains(KEEP_DISABLED, p.getKey()))
					continue;

				if(p instanceof PreferenceGroup)
					removeDisabledPreferences((PreferenceGroup) p);
				else if(!p.isEnabled())
					toRemove.add(p);
			}

			for(Preference p : toRemove)
				root.removePreference(p);
		}

		private void setPreferenceListeners()
		{
			final PreferenceScreen ps = getPreferenceScreen();

			for(int i = 0; i != ps.getPreferenceCount(); ++i)
			{
				final Preference p = ps.getPreference(i);
				if(p != null)
					p.setOnPreferenceChangeListener(this);
				//p.setOnPreferenceClickListener(this);
			}
		}

		private List<Preference> getPreferences(String[] keys)
		{
			final ArrayList<Preference> list = new ArrayList<Preference>(keys.length);
			for(String key : keys)
			{
				final Preference p = findPreference(key);
				if(p != null)
					list.add(p);
			}

			return list;
		}

		private void setupDebugPreferences()
		{
			Preference p = new Preference(getActivity());
			p.setTitle("Notify in 7s");
			p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					new Thread() {
						@Override
						public void run()
						{
							Util.sleepAtMost(7000);
							NotificationReceiver.rescheduleAlarmsAndUpdateNotification(false, true);
						}
					}.start();

					return true;
				}
			});

			getPreferenceScreen().addPreference(p);

			p = findPreference("test_date");
			if(p != null)
				Settings.remove("test_date");

			p = findPreference("db_create_drug_with_schedule");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						final int drugCount = Database.countAll(Drug.class);

						Fraction dose = new Fraction(1, 2);

						Schedule schedule = new Schedule();
						schedule.setDose(Schedule.TIME_MORNING, dose);
						schedule.setDose(Schedule.TIME_EVENING, dose);

						Date today = DateTime.today();

						schedule.setBegin(today);
						schedule.setEnd(DateTime.add(today, Calendar.DAY_OF_MONTH, 14));

						// first four days of the week
						SchedulePart part1 = new SchedulePart(0x78, new Fraction[]
								{ Fraction.ZERO, Fraction.ZERO, new Fraction(1, 2), Fraction.ZERO});

						// remaining three days of the week
						SchedulePart part2 = new SchedulePart(0x7, new Fraction[]
								{ Fraction.ZERO, Fraction.ZERO, new Fraction(1, 4), Fraction.ZERO});

						schedule.setScheduleParts(new SchedulePart[] { part1, part2 });

						Drug drug = new Drug();
						drug.setName("Drug #" + (drugCount + 1));
						drug.addSchedule(schedule);
						drug.setRepeatMode(Drug.REPEAT_CUSTOM);
						drug.setActive(true);

						Database.create(drug);
						Database.create(schedule);
						Database.create(part1);
						Database.create(part2);

						return true;
					}
				});
			}

			p = findPreference("key_debug_drug_with_missed_doses");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						Fraction dose = new Fraction(1, 2);

						Drug drug = new Drug();
						drug.setName("Missing No");
						drug.setDose(Schedule.TIME_MORNING, dose);
						drug.setRefillSize(30);
						drug.setCurrentSupply(new Fraction(23, 1, 2));
						drug.setRepeatMode(Drug.REPEAT_EVERY_N_DAYS);
						drug.setRepeatArg(3);
						drug.setRepeatOrigin(DateTime.add(DateTime.today(), Calendar.DAY_OF_MONTH, -2));
						drug.setLastScheduleUpdateDate(drug.getRepeatOrigin());

						Database.create(drug);

						return true;
					}
				});
			}

			p = findPreference("key_add_drug_past_schedule_end_date");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						final Drug drug = new Drug();
						final LocalDate end = LocalDate.now().minusDays(2);

						drug.setName("PastEndDate #" + Database.countAll(Drug.class));
						drug.setScheduleEndDate(end);
						drug.setDose(Schedule.TIME_MORNING, new Fraction(1, 2));
						drug.setLastScheduleUpdateDate(end.minusDays(9).toDate());

						Database.create(drug);

						return true;
					}
				});
			}

			p = findPreference("key_debug_delete_drugs");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
				{
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						final List<Drug> drugs = Database.getAll(Drug.class);
						final CharSequence[] names = new CharSequence[drugs.size()];
						final boolean[] checked = new boolean[drugs.size()];

						for(int i = 0; i != drugs.size(); ++i)
						{
							names[i] = drugs.get(i).getName();
							checked[i] = false;
						}

						final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
						ab.setNegativeButton(android.R.string.cancel, null);
						ab.setTitle("Delete drugs");
						ab.setMultiChoiceItems(names, checked, new DialogInterface.OnMultiChoiceClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which, boolean isChecked)
							{
								checked[which] = isChecked;
							}
						});
						ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								for(int i = 0; i != drugs.size(); ++i)
								{
									if(checked[i])
										Database.delete(drugs.get(i), Database.FLAG_DONT_NOTIFY_LISTENERS);
								}
							}
						});

						ab.show();

						return true;
					}
				});
			}

			p = findPreference("db_create_drug_with_many_dose_events");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						Fraction dose = new Fraction(1, 2);

						Drug drug = new Drug();
						drug.setName("Megabite");
						drug.setDose(Schedule.TIME_MORNING, dose);
						drug.setRefillSize(30);
						drug.setCurrentSupply(new Fraction(23, 1, 2));

						Database.create(drug);

						Date date;

						for(int i = 0; i != 100; ++i)
						{
							date = DateTime.add(DateTime.today(), Calendar.DAY_OF_MONTH, -i);
							Database.create(new DoseEvent(drug, date, Schedule.TIME_MORNING, dose), Database.FLAG_DONT_NOTIFY_LISTENERS);
						}

						return true;
					}
				});
			}

			p = findPreference("key_debug_add_5_drugs");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						for(int i = 0; i != 5; ++i)
						{
							Drug drug = new Drug();
							drug.setName("Drug #" + Database.countAll(Drug.class));
							drug.setDose(Schedule.TIME_MORNING, new Fraction(1, 2));
							drug.setDose(Schedule.TIME_EVENING, new Fraction(1));
							drug.setRepeatMode(Drug.REPEAT_DAILY);
							drug.setActive(true);

							try
							{
								Database.create(drug);
							}
							catch(Exception e)
							{
								Log.w(TAG, e);
							}
						}

						return true;
					}
				});
			}

			p = findPreference("key_debug_crash_app");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						RxDroid.runInMainThread(new Runnable() {

							@Override
							public void run()
							{
								throw new RuntimeException("Crash requested by user");
							}
						});
						return true;
					}
				});
			}

			p = findPreference("key_debug_tablet_layout");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
	//					Intent intent = new Intent(getApplicationContext(), LayoutTestActivity.class);
	//					intent.putExtra(LayoutTestActivity.EXTRA_LAYOUT_RES_ID, R.layout.mockup_activity_druglist);
	//					startActivity(intent);
						return true;
					}
				});
			}

			p = findPreference("boot_info");
			if(p != null)
			{
				SpannableString summary = new SpannableString(
						"boot timestamp  : " + RxDroid.getBootTimestamp() + "\n" +
						"BOOT_COMPLETED  : " + Settings.getLong(Settings.Keys.BOOT_COMPLETED_TIMESTAMP, 0) + "\n" +
						"update timestamp: " + RxDroid.getLastUpdateTimestamp()
				);
				Util.applyStyle(summary, new TypefaceSpan("monospace"));
				p.setSummary(summary);
			}

			p = findPreference("reset_refill_reminder_date");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						Settings.putDate(Settings.Keys.UNSNOOZE_DATE, null);
						return true;
					}
				});
			}

			p = findPreference("key_debug_create_backup");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
				{
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						try
						{
							Backup.createBackup(null, "foobar");
						}
						catch(ZipException e)
						{
							Log.w(TAG, e);
							Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
						}
						return true;
					}
				});
			}

			p = findPreference("dump_build");
			if(p != null)
			{
				p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
				{
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						try
						{
							final StringBuilder sb = new StringBuilder();
							final String[] classes = { "android.os.Build", "android.os.Build$VERSION" };

							for(String className : classes)
							{
								Class<?> clazz = Class.forName(className);

								sb.append(clazz.getName() + "\n");
								for(Field f : clazz.getDeclaredFields())
								{
									int m = f.getModifiers();

									if(Modifier.isStatic(m) && Modifier.isPublic(m) &&Modifier.isFinal(m))
									{
										sb.append("  " + f.getName() + ": " + f.get(null) + "\n");
									}
								}
								sb.append("\n");
							}

							Log.d(TAG, sb.toString());
						}
						catch(ClassNotFoundException e)
						{
							Log.w(TAG, e);
						}
						catch(IllegalAccessException e)
						{
							Log.w(TAG, e);
						}

						return true;
					}
				});
			}

			final String[] keys = {
					Settings.Keys.UNSNOOZE_DATE,
					"refill_reminder_snooze_drugs"
			};

			for(String key : keys)
			{
				p = findPreference(key);
				if(p == null)
					continue;

				p.setSummary(p.getSharedPreferences().getString(key, "(null)"));
			}
		}
	}
}
