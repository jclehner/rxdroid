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

package at.jclehner.rxdroid.util;

import java.util.Calendar;

import android.text.format.DateUtils;
import at.jclehner.rxdroid.DumbTime;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.db.Drug;

@SuppressWarnings("deprecation")
public final class Constants
{
	public static final long MILLIS_PER_DAY = 24L * 3600 * 1000;
	public static final String NOTIFICATION_BULLET = "\u2022 ";
	public static final long NOTIFICATION_INITIAL_DELAY = 5000;

	public static final String EMPTY_STRING = "";

	public static final DumbTime MIDNIGHT = new DumbTime(0, 0);

	public static int getDoseViewId(int doseTime) {
		return DOSE_VIEW_IDS[doseTime];
	}

	public static final int[] DOSE_VIEW_IDS = {
			R.id.morning,
			R.id.noon,
			R.id.evening,
			R.id.night
	};

	public static final int[] WEEK_DAYS = {
			Calendar.MONDAY,
			Calendar.TUESDAY,
			Calendar.WEDNESDAY,
			Calendar.THURSDAY,
			Calendar.FRIDAY,
			Calendar.SATURDAY,
			Calendar.SUNDAY
	};

	public static final Integer[] DOSE_TIMES = { Drug.TIME_MORNING, Drug.TIME_NOON, Drug.TIME_EVENING, Drug.TIME_NIGHT };

	public static final String[] LONG_WEEK_DAY_NAMES;
	public static final String[] SHORT_WEEK_DAY_NAMES;

	static
	{
		LONG_WEEK_DAY_NAMES = new String[WEEK_DAYS.length];
		SHORT_WEEK_DAY_NAMES = new String[WEEK_DAYS.length];

		for(int i = 0; i != WEEK_DAYS.length; ++i)
		{
			LONG_WEEK_DAY_NAMES[i] = DateUtils.getDayOfWeekString(WEEK_DAYS[i], DateUtils.LENGTH_LONG);
			SHORT_WEEK_DAY_NAMES[i] = DateUtils.getDayOfWeekString(WEEK_DAYS[i], DateUtils.LENGTH_MEDIUM);
		}
	}
}
