/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid.test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.util.Log;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.Settings.DoseTimeInfo;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.util.DateTime;

/**
 * Tests for the time handling methods from the Preferences class.
 * <p>
 * For the purpose of this test, the dose times are guaranteed to be the
 * following:
 *
 * <pre>
 * Morning: 06:00-10:00
 * Evening: 18:00-21:00
 * Night  : 23:00-01:30
 * </pre>
 *
 * The times not listed above cannot be relied upon and should not be used.
 *
 * @author Joseph Lehner
 *
 */
public class PreferencesTest extends AndroidTestCase
{
	private static final String TAG = PreferencesTest.class.getSimpleName();

	private SharedPreferences mPrefs;
	private Map<String, String> mPrefBackup;

	public void testGetActiveDoseTimeAndGetNextDoseTime()
	{
		final int[][] testCases = {
				{      18, 00, Drug.TIME_EVENING, Drug.TIME_NIGHT },
				{      21, 00, Drug.TIME_INVALID, Drug.TIME_NIGHT },
				{      23, 00, Drug.TIME_NIGHT, Drug.TIME_MORNING },
				{ 24 + 00, 00, Drug.TIME_NIGHT, Drug.TIME_MORNING },
				{ 24 + 00, 30, Drug.TIME_NIGHT, Drug.TIME_MORNING },
				{ 24 + 01, 30, Drug.TIME_INVALID, Drug.TIME_MORNING }
		};

		for(int i = 0; i != testCases.length; ++i)
		{
			final int hours        = testCases[i][0];
			final int minutes      = testCases[i][1];
			final int doseTime     = testCases[i][2];
			final int nextDoseTime = testCases[i][3];

			final Calendar date = DateTime.todayCalendarMutable();
			final Calendar time = DateTime.todayCalendarMutable();

			time.set(Calendar.HOUR_OF_DAY, hours);
			time.set(Calendar.MINUTE, minutes);

			Log.d(TAG, "testGetActiveDoseTimeAndGetNextDoseTime:");
			//Log.d(TAG, "  date/time   : " + date + ", " + time);
			Log.d(TAG, "  doseTime    : " + doseTime);
			Log.d(TAG, "  nextDoseTime: " + nextDoseTime + "\n");

			assertEquals(doseTime, Settings.getActiveDoseTime(time));
			Log.d(TAG, "  [OK] getActiveDoseTime");
			assertEquals(nextDoseTime, Settings.getNextDoseTime(time));
			Log.d(TAG, "  [OK] getNextDoseTime");
			Log.d(TAG, "-----------------------------");
		}
	}

	public void testTimeOffsets()
	{
		final long[][] testCases = {
				{      18, 00, -1, 10800000 }, // (21:00 - 18:00) = 180min in millis
				{      21, 00, 7200000, -1 }, // (23:00 - 21:00) = 120min in millis
				{      23, 00, 0, 9000000 }, // ((24 + 01:30) - 23:00 = 150min in millis
				{ 24 + 00, 00, -1, 5400000 }, // (01:30 - 00:00) = 90min in millis
				{ 24 + 00, 30, -1, 3600000 }, // (01:30 - 00:30) = 60min in millis
				{ 24 + 01, 30, 16200000, -1 } // (06:00 - 01:30) = 270min in millis
		};

		testTimeOffsets(DateTime.todayCalendarMutable(), testCases);
	}

	public void testTimeOffsetsWithDst()
	{
		final long[][] testCases = {
			{ 00, 00, -1, 5400000 }, // (01:30 - 00:00) = 90min in millis
			{ 00, 30, -1, 3600000 }, // (01:30 - 00:30) = 60min in millis
			{ 01, 30, 16200000 + 3600000, -1 } // (06:00 - 01:30) = 270 + 90min due to DST switch
		};

		final TimeZone tzGmt = TimeZone.getTimeZone("GMT");
		final TimeZone tzCet = TimeZone.getTimeZone("Europe/Paris");

		if(tzCet.hasSameRules(tzGmt))
		{
			Log.w(TAG, "Could not TimeZone instance for CET/CEST");
			return;
		}

		//
		//      java.util.Calendar's October is 9 (January is 0)
		//                                     |
		//                                     v
		Calendar date = DateTime.calendar(2011, 10 - 1, 30); // DST ends on this day in CEST
		date.setTimeZone(tzCet);

		testTimeOffsets(date, testCases);
	}

	private void testTimeOffsets(Calendar date, long[][] testCases)
	{
		for(int i = 0; i != testCases.length; ++i)
		{
			final long hours            = testCases[i][0];
			final long minutes          = testCases[i][1];
			final long millisUntilBegin = testCases[i][2];
			final long millisUntilEnd   = testCases[i][3];

			final Calendar time = (Calendar) date.clone();

			time.setTimeZone(date.getTimeZone());
			time.set(Calendar.HOUR_OF_DAY, (int) hours);
			time.set(Calendar.MINUTE, (int) minutes);

			final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo(time);
			final int activeOrNextDoseTime;

			if(dtInfo.activeDoseTime() == Schedule.TIME_INVALID)
				activeOrNextDoseTime = dtInfo.nextDoseTime();
			else
				activeOrNextDoseTime = dtInfo.activeDoseTime();

			Log.d(TAG, "testTimeOffsets:");
			Log.d(TAG, "  date/time           : " + DateTime.toString(date));
			Log.d(TAG, "  hours               : " + hours);
			Log.d(TAG, "  minutes             : " + minutes);
			Log.d(TAG, "  millisUntilBegin    : " + millisUntilBegin);
			Log.d(TAG, "  millisUntilEnd      : " + millisUntilEnd);
			Log.d(TAG, "  activeOrNextDoseTime: " + activeOrNextDoseTime);

			if(millisUntilBegin != -1)
			{
				assertEquals(millisUntilBegin, Settings.getMillisUntilDoseTimeBegin(time, activeOrNextDoseTime));
				Log.d(TAG, "  [OK] getMillisUntilDoseTimeBegin");
			}
			else
				Log.d(TAG, "  [N/A] getMillisUntilDoseTimeBegin");

			if(millisUntilEnd != -1)
			{
				assertEquals(millisUntilEnd, Settings.getMillisUntilDoseTimeEnd(time, activeOrNextDoseTime));
				Log.d(TAG, "  [OK] getMillisUntilDoseTimeEnd");
			}
			else
				Log.d(TAG, "  [N/A] getMillisUntilDoseTimeEnd");

			Log.d(TAG, "-----------------------------");
		}
	}

	public void testGetActiveDate()
	{
		// [0] ... hours
		// [1] ... minutes
		// [2] ... + days

		final int[][] testCases = {
			{      18, 30, 0 },
			{      21, 30, 0 },
			{ 24 + 00, 30, 0 },
			{ 24 + 02, 30, 1 },
			{ 24 + 05, 30, 1 },
			{ 24 + 06, 30, 1 },
			{ 24 + 24, 30, 1 },
			{ 48 + 06, 30, 2 }
		};

		final Calendar today = DateTime.todayCalendarMutable();

		for(int i = 0; i != testCases.length; ++i)
		{
			final Calendar time = (Calendar) today.clone();
			time.add(Calendar.HOUR_OF_DAY, testCases[i][0]);
			time.add(Calendar.MINUTE, testCases[i][1]);

			final Calendar expected = (Calendar) today.clone();
			expected.add(Calendar.DAY_OF_MONTH, testCases[i][2]);

			assertEquals(expected.getTime(), Settings.getActiveDate(time));
		}

	}

	@Override
	protected void setUp()
	{
		try
		{
			super.setUp();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		//GlobalContext.set(mContext);
		RxDroid.setContext(mContext);
		Settings.init();

		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mPrefBackup = new HashMap<String, String>();

		for (String key : mPrefs.getAll().keySet())
		{
			if (key.startsWith("time_"))
				mPrefBackup.put(key, mPrefs.getString(key, null));
		}

		Editor e = mPrefs.edit();

		e.putString("time_morning", "06:00-10:00");
		e.putString("time_evening", "18:00-21:00");
		e.putString("time_night", "23:00-01:30");

		e.commit();
	}

	@Override
	protected void tearDown()
	{
		try
		{
			super.tearDown();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		Editor e = mPrefs.edit();

		for (String key : mPrefBackup.keySet())
			e.putString(key, mPrefBackup.get(key));

		e.commit();
	}

	private static void assertEquals(Calendar expected, Calendar actual) {
		assertEquals(DateTime.toString(expected), DateTime.toString(actual));
	}
}
