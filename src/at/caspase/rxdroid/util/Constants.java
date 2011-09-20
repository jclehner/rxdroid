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

package at.caspase.rxdroid.util;

import java.util.Calendar;

import android.text.format.DateUtils;
import at.caspase.rxdroid.R;

public final class Constants
{
	public static final long MILLIS_PER_DAY = 24L * 3600 * 1000;
	public static final String NOTIFICATION_BULLET = "\u2022 ";
	public static final long NOTIFICATION_INITIAL_DELAY = 10000;

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

	public static final String[] WEEK_DAY_NAMES;

	static
	{
		WEEK_DAY_NAMES = new String[WEEK_DAYS.length];

		for(int i = 0; i != WEEK_DAY_NAMES.length; ++i)
			WEEK_DAY_NAMES[i] = DateUtils.getDayOfWeekString(WEEK_DAYS[i], DateUtils.LENGTH_LONG);
	}
}
