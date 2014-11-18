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

package at.jclehner.androidutils.otpm;

import android.preference.Preference;

public class ReadonlyPreferenceController extends PreferenceController<Preference, Object>
{
	@Override
	public void initPreference(Preference preference, Object fieldValue) {
		preference.setEnabled(false);
	}

	@Override
	public void updateSummary(Preference preference, Object newValue) {
		preference.setSummary(newValue == null ? "(null)" : newValue.toString());
	}

	@Override
	public void onDependencyChange(Preference preference, String depKey, Object newPrefValue) {
		updateSummary(preference, newPrefValue);
	}
}
