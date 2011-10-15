/**
 * Copyright (C) 2[011 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.caspase.rxdroid.debug;

import java.util.Calendar;

import at.caspase.rxdroid.Preferences;
import at.caspase.rxdroid.db.Drug;


/**
 * Fake Settings class for debugging.
 *
 * @see at.caspase.rxdroid.Preferences
 * @author Joseph Lehner
 *
 */
public class FakeSettings extends Preferences
{
	@SuppressWarnings("unused")
	private static final String TAG = FakeSettings.class.getName();
	private static int counter = 0;
	
	@Override
	public long getMillisUntilDoseTimeBegin(Calendar time, int doseTime) {
		return 10 * 1000;
	}

	@Override
	public long getMillisUntilDoseTimeEnd(Calendar time, int doseTime) {
		return 10 * 1000;
	}

	@Override
	public long getSnoozeTime() {
		return 5 * 1000;
	}

	@Override
	public int getActiveDoseTime(Calendar time)
	{
		final int doseTime = counter % (Drug.TIME_NIGHT + 1);
		++counter;
		
		return doseTime;
	}

	@Override
	public int getNextDoseTime(Calendar time) {
		return (counter % (Drug.TIME_NIGHT + 1));
	}
}
