package at.caspase.androidutils.otpm;

import android.content.Context;
import android.preference.ListPreference;
import android.util.Log;
import at.caspase.rxdroid.util.Util;
/**
 * Copyright (C) 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

public class ListPreferenceWithIntHelper extends PreferenceHelper<ListPreference, Integer>
{
	private final String[] mEntries;

	public ListPreferenceWithIntHelper() {
		throw new UnsupportedOperationException("You must extend this class to use it");
	}

	public ListPreferenceWithIntHelper(Context context, int entriesResId) {
		this(context.getResources().getStringArray(entriesResId));
	}

	public ListPreferenceWithIntHelper(String[] entries) {
		mEntries = entries;
	}

	@Override
	public void initPreference(ListPreference preference, Integer fieldValue)
	{
		preference.setEntries(mEntries);
		Util.populateListPreferenceEntryValues(preference);
		preference.setValueIndex(fieldValue);
		preference.setSummary(mEntries[fieldValue]);
	}

	@Override
	public boolean updatePreference(ListPreference preference, Object newPrefValue)
	{
		Log.d("XXXXXXX", "(" + newPrefValue.getClass().getSimpleName() + ") = " + newPrefValue);

		Integer index = Integer.valueOf((String) newPrefValue, 10);
		setFieldValue(index);
		preference.setSummary(mEntries[index]);
		return true;
	}
}