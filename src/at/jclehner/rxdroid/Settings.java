/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
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

package at.jclehner.rxdroid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.preferences.TimePeriodPreference.TimePeriod;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public final class Settings
{
	public static class Keys
	{
		@Deprecated
		public static final String USE_LED = key(R.string.key_use_led);
		@Deprecated
		public static final String USE_SOUND = key(R.string.key_use_sound);
		public static final String USE_VIBRATOR = key(R.string.key_use_vibrator);
		public static final String LOCKSCREEN_TIMEOUT = key(R.string.key_lockscreen_timeout);
		public static final String SCRAMBLE_NAMES = key(R.string.key_scramble_names);
		public static final String PIN = key(R.string.key_pin);
		public static final String LOW_SUPPLY_THRESHOLD = key(R.string.key_low_supply_threshold);
		public static final String ALARM_REPEAT = key(R.string.key_alarm_mode);
		public static final String SHOW_SUPPLY_MONITORS = key(R.string.key_show_supply_monitors);
		public static final String LAST_MSG_HASH = key(R.string.key_last_msg_hash);
		public static final String VERSION = key(R.string.key_version);
		public static final String LICENSES = key(R.string.key_licenses);
		public static final String HISTORY_SIZE = key(R.string.key_history_size);
		public static final String THEME_IS_DARK = key(R.string.key_theme_is_dark);
		public static final String NOTIFICATION_SOUND = key(R.string.key_notification_sound);
		public static final String ENABLE_LANDSCAPE = key(R.string.key_enable_landscape_mode);
		public static final String DONATE = key(R.string.key_donate);
		public static final String REPEAT_ALARM = key(R.string.key_repeat_alarm);
		public static final String DB_STATS = key(R.string.key_db_stats);
		public static final String COMPACT_ACTION_BAR = key(R.string.key_compact_action_bar);
		public static final String NOTIFICATION_LIGHT_COLOR = key(R.string.key_notification_light_color);
		public static final String QUIET_HOURS = key(R.string.key_quiet_hours);

		@Deprecated
		public static final String DISPLAYED_HELP_SUFFIXES = "displayed_help_suffixes";
		public static final String DISPLAYED_ONCE = "displayed_once";
		public static final String IS_FIRST_LAUNCH = "is_first_launch";
	}

	public static class Enums
	{
		public static final int HISTORY_SIZE_1M = 0;
		public static final int HISTORY_SIZE_2M = 1;
		public static final int HISTORY_SIZE_6M = 2;
		public static final int HISTORY_SIZE_1Y = 3;
		public static final int HISTORY_SIZE_UNLIMITED = 4;
	}

	public static class OnceIds
	{
		public static final String DRAG_DROP_SORTING = "drag_drop_sorting";
		public static final String MISSING_TRANSLATION = "missing_translation";
	}

	public static class Defaults
	{
		public static final boolean ENABLE_LANDSCAPE = booleanResource(R.bool.pref_default_landscape_enabled);
		public static final boolean COMPACT_ACTION_BAR = booleanResource(R.bool.pref_default_compact_action_bar);
	}

	private static final String TAG = Settings.class.getSimpleName();
	private static final boolean LOGV = false;

	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private static final String KEYS[] = { "time_morning", "time_noon", "time_evening", "time_night" };

	private static SharedPreferences sSharedPrefs = null;

	public static synchronized void init()
	{
		if(sSharedPrefs == null)
		{
			sSharedPrefs = PreferenceManager.getDefaultSharedPreferences(RxDroid.getContext());
			migrateSettings();
		}
	}

	public static void setChecked(String key, boolean checked) {
		putBoolean(getKeyForCheckedStatus(key), checked);
	}

	public static boolean isChecked(String key, boolean defaultChecked) {
		return getBoolean(getKeyForCheckedStatus(key), defaultChecked);
	}

	public static Set<String> getStringSet(String key) {
		return stringToStringSet(sSharedPrefs.getString(key, null));
	}

	public static void putStringSet(String key, Set<String> set) {
		sSharedPrefs.edit().putString(key, stringSetToString(set)).commit();
	}

	public static void putStringSetEntry(String key, String entry)
	{
		final Set<String> set = getStringSet(key);
		set.add(entry);
		putStringSet(key, set);
	}

	public static boolean containsStringSetEntry(String key, String entry) {
		return getStringSet(key).contains(entry);
	}

	public static String getString(String key, String defValue) {
		return sSharedPrefs.getString(key, defValue);
	}

	public static String getString(String key) {
		return getString(key, null);
	}

	public static void putString(String key, String value) {
		sSharedPrefs.edit().putString(key, value).commit();
	}

	public static Date getDate(String key)
	{
		final String str = getString(key, null);
		if(str == null)
			return null;

		try
		{
			SimpleDateFormat sdf = PerThreadInstance.get(SimpleDateFormat.class, DATE_FORMAT);
			return sdf.parse(str);
		}
		catch(ParseException e)
		{
			throw new WrappedCheckedException(e);
		}
	}

	public static void putDate(String key, Date date)
	{
		SimpleDateFormat sdf = PerThreadInstance.get(SimpleDateFormat.class, DATE_FORMAT);
		putString(key, sdf.format(date));
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		return sSharedPrefs.getBoolean(key, defaultValue);
	}

	public static void putBoolean(String key, boolean value) {
		sSharedPrefs.edit().putBoolean(key, value).commit();
	}

	public static int getInt(String key, int defValue) {
		return sSharedPrefs.getInt(key, defValue);
	}

	public static int getInt(String key) {
		return getInt(key, 0);
	}

	public static void putInt(String key, int value) {
		sSharedPrefs.edit().putInt(key, value).commit();
	}

	public static boolean isPastMaxHistoryAge(Date reference, Date date)
	{
		if(date == null)
			return false;

		final int index = getIntFromList(Keys.HISTORY_SIZE, 2);

		final int field;
		final int value;

		switch(index)
		{
			case 0:
				field = Calendar.MONTH;
				value = 1;
				break;

			case 1:
				field = Calendar.MONTH;
				value = 2;
				break;

			case 2:
				field = Calendar.MONTH;
				value = 6;
				break;

			case 3:
				field = Calendar.YEAR;
				value = 1;
				break;

			default:
				return false;
		}

		reference = DateTime.add(reference, field, -value);
		return date.before(reference);
	}

	public static long getMillisUntilDoseTimeBegin(Calendar time, int doseTime) {
		return getMillisUntilDoseTimeBeginOrEnd(time, doseTime, FLAG_GET_MILLIS_UNTIL_BEGIN);
	}

	public static long getMillisUntilDoseTimeEnd(Calendar time, int doseTime) {
		return getMillisUntilDoseTimeBeginOrEnd(time, doseTime, 0);
	}

	public static long getDoseTimeBeginOffset(int doseTime) {
		return getDoseTimeBegin(doseTime).getMillisFromMidnight();
	}

	public static long getDoseTimeEndOffset(int doseTime) {
		return getDoseTimeEnd(doseTime).getMillisFromMidnight();
	}

	public static long getTrueDoseTimeEndOffset(int doseTime)
	{
		final long doseTimeBeginOffset = getDoseTimeBeginOffset(doseTime);
		long doseTimeEndOffset = getDoseTimeEndOffset(doseTime);

		if(doseTimeEndOffset < doseTimeBeginOffset)
			doseTimeEndOffset += Constants.MILLIS_PER_DAY;

		return doseTimeEndOffset;
	}

	public static boolean hasWrappingDoseTimeNight() {
		return getDoseTimeEndOffset(Drug.TIME_NIGHT) != getTrueDoseTimeEndOffset(Drug.TIME_NIGHT);
	}

	public static DumbTime getDoseTimeBegin(int doseTime)
	{
		final TimePeriod p = getTimePeriodPreference(doseTime);
		return p == null ? null : p.begin();
	}

	public static DumbTime getDoseTimeEnd(int doseTime)
	{
		final TimePeriod p = getTimePeriodPreference(doseTime);
		return p == null ? null : p.end();
	}

	public static TimePeriod getTimePeriodPreference(int doseTime)
	{
		final String key = KEYS[doseTime];

		String value = sSharedPrefs.getString(key, null);
		if(value == null)
		{
			final Context context = RxDroid.getContext();
			int resId = context.getResources().
				getIdentifier("at.jclehner.rxdroid:string/pref_default_" + key, null, null);

			if(resId == 0 || (value = context.getString(resId)) == null)
				throw new IllegalStateException("No default value for time preference " + key + " in strings.xml");

			if(LOGV) Log.i(TAG, "Persisting preference: " + key + "=" + value);
			putString(key, value);
		}

		return TimePeriod.fromString(value);
	}

	public static class DoseTimeInfo
	{
		private static final ThreadLocal<DoseTimeInfo> INSTANCES = new ThreadLocal<Settings.DoseTimeInfo>() {

			@Override
			protected DoseTimeInfo initialValue()
			{
				return new DoseTimeInfo();
			}

		};

		public Calendar currentTime() {
			return mCurrentTime;
		}

		public Date activeDate() {
			return mActiveDate;
		}

		public Date nextDoseTimeDate() {
			return mNextDoseTimeDate;
		}

		public int activeDoseTime() {
			return mActiveDoseTime;
		}

		public int nextDoseTime() {
			return mNextDoseTime;
		}

		private Calendar mCurrentTime;
		private Date mActiveDate;
		private Date mNextDoseTimeDate;
		private int mActiveDoseTime;
		private int mNextDoseTime;

		private DoseTimeInfo() {}
	}

	public static DoseTimeInfo getDoseTimeInfo() {
		return getDoseTimeInfo(DateTime.nowCalendar());
	}

	public static DoseTimeInfo getDoseTimeInfo(Calendar currentTime)
	{
		final DoseTimeInfo dtInfo = DoseTimeInfo.INSTANCES.get();

		dtInfo.mCurrentTime = currentTime;
		dtInfo.mActiveDate = getActiveDate(dtInfo.mCurrentTime);
		dtInfo.mActiveDoseTime = getActiveDoseTime(dtInfo.mCurrentTime);
		dtInfo.mNextDoseTime = getNextDoseTime(dtInfo.mCurrentTime);
		dtInfo.mNextDoseTimeDate = dtInfo.mActiveDate;

		if(dtInfo.mNextDoseTime == Schedule.TIME_MORNING)
		{
			final boolean useNextDay;

			if(dtInfo.mActiveDoseTime == Schedule.TIME_INVALID)
			{
				// If we have a wrapping TIME_NIGHT and there is no active
				// dose time, the time must be later than TIME_NIGHT's end,
				// which will always be the date of the next TIME_MORNING.
				//
				// If TIME_NIGHT is not wrapping, we must check, whether we're
				// after TIME_NIGHT's end but before midnight.

				if(hasWrappingDoseTimeNight())
					useNextDay = false;
				else
				{
					final long offsetFromMidnight = DateTime.getOffsetFromMidnight(currentTime);
					final long endOfNightOffset = getDoseTimeEndOffset(Schedule.TIME_NIGHT);

					useNextDay = offsetFromMidnight > endOfNightOffset;
				}
			}
			else if(dtInfo.mActiveDoseTime == Schedule.TIME_NIGHT)
				useNextDay = true;

			else
			{
				Log.w(TAG, "W00t? This was unexpected...");
				useNextDay = false;
			}

			if(useNextDay)
				dtInfo.mNextDoseTimeDate = DateTime.add(dtInfo.mNextDoseTimeDate, Calendar.DAY_OF_MONTH, 1);
		}

		return dtInfo;
	}

	public static Date getActiveDate(Calendar time)
	{
		final Calendar activeDate = DateTime.getDatePartMutable(time);
		final int activeDoseTime = getActiveDoseTime(time);

		if(activeDoseTime == Drug.TIME_NIGHT && hasWrappingDoseTimeNight())
		{
			final DumbTime end = new DumbTime(getDoseTimeEndOffset(Drug.TIME_NIGHT));
			if(DateTime.isWithinRange(time, Constants.MIDNIGHT, end))
				activeDate.add(Calendar.DAY_OF_MONTH, -1);
		}

		return activeDate.getTime();
	}

	public static boolean isBeforeDoseTimeNightWrap(DoseTimeInfo dtInfo)
	{
		if(!hasWrappingDoseTimeNight())
			throw new IllegalStateException("!hasWrappingDoseTimeNight()");

		if(dtInfo.mActiveDoseTime != Schedule.TIME_NIGHT)
			throw new IllegalStateException("dtInfo.activeDoseTime != Schedule.TIME_NIGHT");

		final long endOfNightOffset = getDoseTimeEndOffset(Schedule.TIME_NIGHT);
		final long currentTimeOffset = DateTime.getOffsetFromMidnight(dtInfo.mCurrentTime);

		Log.d(TAG, "endOfNightOffset=" + endOfNightOffset);
		Log.d(TAG, "currentTimeOffset=" + currentTimeOffset);

		return currentTimeOffset > endOfNightOffset;
	}

	@SuppressWarnings("deprecation")
	public static Date getActiveDate() {
		return getActiveDate(DateTime.nowCalendarMutable());
	}

	public static int getActiveDoseTime(Calendar time)
	{
		for(int doseTime : Constants.DOSE_TIMES)
		{
			if(DateTime.isWithinRange(time, getDoseTimeBegin(doseTime), getDoseTimeEnd(doseTime)))
				return doseTime;
		}

		return Schedule.TIME_INVALID;
	}

	public static int getNextDoseTime(Calendar time) {
		return getNextDoseTime(time, false);
	}

	public static int getNextDoseTime(Calendar time, boolean useNextDayOffsets)
	{
		int retDoseTime = -1;
		long smallestDiff = 0;

		//Log.d(TAG, "getNextDoseTime: time=" + time);

		for(int doseTime : Constants.DOSE_TIMES)
		{
			long diff = getMillisUntilDoseTimeBegin(time, doseTime);
			if(useNextDayOffsets)
				diff += Constants.MILLIS_PER_DAY;

			//Log.d(TAG, "  diff " + diff + " for doseTime=" + doseTime);

			if(diff > 0 && (smallestDiff == 0 || diff < smallestDiff))
			{
				smallestDiff = diff;
				retDoseTime = doseTime;
			}
		}

		if(retDoseTime == -1)
		{
			if(!useNextDayOffsets)
				return getNextDoseTime(time, true);

			throw new IllegalStateException("retDoseTime == -1");
		}

		//Log.d(TAG, "  retDoseTime=" + retDoseTime);
		return retDoseTime;
	}

	public static int getIntFromList(String key, int defValue)
	{
		String valueStr = getString(key, null);
		return valueStr != null ? Integer.parseInt(valueStr, 10) : defValue;
	}

	public static int getStringAsInt(String key, int defValue)
	{
		final String value = getString(key);
		if(value != null)
			return Integer.parseInt(value);

		return defValue;
	}

	public static void maybeLockInPortraitMode(Activity activity)
	{
		if(!Settings.getBoolean(Keys.ENABLE_LANDSCAPE, Defaults.ENABLE_LANDSCAPE))
		{
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		else
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	public static boolean wasDisplayedOnce(String onceId) {
		return containsStringSetEntry(Keys.DISPLAYED_ONCE, onceId);
	}

	public static void setDisplayedOnce(String onceId) {
		putStringSetEntry(Keys.DISPLAYED_ONCE, onceId);
	}

	private static String key(int resId) {
		return RxDroid.getContext().getString(resId);
	}

	private static boolean booleanResource(int resId) {
		return RxDroid.getContext().getResources().getBoolean(resId);
	}

	private static final int FLAG_GET_MILLIS_UNTIL_BEGIN = 1;
	private static final int FLAG_DONT_CORRECT_TIME = 2;

	private static long getMillisUntilDoseTimeBeginOrEnd(Calendar time, int doseTime, int flags)
	{
		final long doseTimeOffsetMillis = (flags & FLAG_GET_MILLIS_UNTIL_BEGIN) != 0 ?
					getDoseTimeBeginOffset(doseTime) : getDoseTimeEndOffset(doseTime);

		/*final long doseTimeOffsetMillis;
		if((flags & FLAG_GET_MILLIS_UNTIL_BEGIN) != 0)
			doseTimeOffsetMillis = getDoseTimeBeginOffset(doseTime);
		else
			doseTimeOffsetMillis = getDoseTimeEndOffset(doseTime);*/

		final DumbTime doseTimeOffset = new DumbTime(doseTimeOffsetMillis);
		final Calendar target = DateTime.getDatePartMutable(time);

		// simply adding the millisecond offset is tempting, but leads to errors
		// when the DST begins/ends in this interval

		target.set(Calendar.HOUR_OF_DAY, doseTimeOffset.getHours());
		target.set(Calendar.MINUTE, doseTimeOffset.getMinutes());
		target.set(Calendar.SECOND, doseTimeOffset.getSeconds());

		if(target.getTimeInMillis() < time.getTimeInMillis() && (flags & FLAG_DONT_CORRECT_TIME) == 0)
			target.add(Calendar.DAY_OF_MONTH, 1);

		return target.getTimeInMillis() - time.getTimeInMillis();
	}

	private static String getKeyForCheckedStatus(String key) {
		return "__" + key + "_is_checked__";
	}

	private static void remove(String key)
	{
		removeInternal(key);
		removeInternal(getKeyForCheckedStatus(key));
	}

	private static void removeInternal(String key) {
		sSharedPrefs.edit().remove(key).commit();
	}

	private static boolean contains(String key) {
		return sSharedPrefs.contains(key);
	}

	private static void migrateSettings()
	{
		if(contains("displayed_info_ids"))
		{
			Log.d(TAG, "init: migrating DISPLAYED_INFO_IDS");

			putString(Keys.DISPLAYED_ONCE, getString("displayed_info_ids"));
			remove("displayed_info_ids");
		}

		if(contains(Keys.USE_SOUND))
		{
			Log.d(TAG, "init: migrating USE_SOUND");

			/**
			 * USE_SOUND:
			 *
			 * true -> no change needed
			 * false -> set sound to "" (=silent)
			 *
			 */

			if(!getBoolean(Keys.USE_SOUND, true))
				putString(Keys.NOTIFICATION_SOUND, "");

			remove(Keys.USE_SOUND);
		}

		if(contains(Keys.USE_LED))
		{
			Log.d(TAG, "init: migrating USE_LED");

			/**
			 * USE_LED -> NOTIFICATION_LIGHT_COLOR:
			 *
			 * true -> no change needed; "" means default color
			 * false -> "0"
			 */
			if(!getBoolean(Keys.USE_LED, true))
				putString(Keys.NOTIFICATION_LIGHT_COLOR, "0");
			else
				putString(Keys.NOTIFICATION_LIGHT_COLOR, "");

			remove(Keys.USE_LED);
		}
	}

	// converts the string set [ "foo", "bar", "foobar", "barz" ] to the following string:
	// 4:3:foo3:bar6:foobar4:barz

	private static String stringSetToString(Set<String> set)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(set.size() + ":");

		for(String str : set)
			sb.append(str.length() + ":" + str);

		return sb.toString();
	}

	private static Set<String> stringToStringSet(String str)
	{
		final HashSet<String> stringSet = new HashSet<String>();

		if(str == null || str.length() == 0)
			return stringSet;

		SizePrefix info = getSizePrefix(str, 0);
		if(info.size == 0)
			return stringSet;

		final int size = info.size;

		int end = info.firstCharPos;

		for(int i = 0; i != size; ++i)
		{
			info = getSizePrefix(str, end);
			end = info.firstCharPos + info.size;
			stringSet.add(str.substring(info.firstCharPos, end));
		}

		return stringSet;
	}

	private static class SizePrefix
	{
		int size;
		int firstCharPos;
	}

	private static SizePrefix getSizePrefix(String str, int pos)
	{
		final StringBuilder sb = new StringBuilder();
		int i = pos;

		//Log.d(TAG, "getSizePrefix: pos=" + pos);
		//Log.d(TAG, "  str=" + str.substring(pos, pos + 5) + "...");

		for(; i != str.length() && str.charAt(i) != ':'; ++i)
		{
			final char ch = str.charAt(i);
			if(!Character.isDigit(ch))
				throw new IllegalArgumentException("Unexpected non-digit char at pos=" + i);

			sb.append(ch);
		}

		if(sb.length() == 0)
			throw new IllegalArgumentException("Unexpected token at pos=" + pos + " (" + str + ")");

		final SizePrefix prefix = new SizePrefix();
		prefix.size = Integer.parseInt(sb.toString());
		prefix.firstCharPos = i + 1;
		return prefix;
	}

	private Settings() {}
}
