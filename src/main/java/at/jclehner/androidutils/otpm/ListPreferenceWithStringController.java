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

import android.content.res.Resources;
import android.preference.ListPreference;

public abstract class ListPreferenceWithStringController extends DialogPreferenceController<ListPreference, String>
{
	private String[] mEntries;
	private String[] mValues;

	private int mEntriesResId;
	private int mValuesResId;

	public ListPreferenceWithStringController(int entriesResId, int valuesResId)
	{
		mEntriesResId = entriesResId;
		mValuesResId = valuesResId;
	}

	public ListPreferenceWithStringController(String[] entries, String[] values)
	{
		if(entries == null || values == null)
			throw new NullPointerException();

		mEntries = entries;
		mValues = values;
	}

	@Override
	public void initPreference(ListPreference preference, String fieldValue)
	{
		super.initPreference(preference, fieldValue);

		if(mEntries == null || mValues == null)
		{
			final Resources r = preference.getContext().getResources();
			mEntries = r.getStringArray(mEntriesResId);
			mValues = r.getStringArray(mValuesResId);
		}

		preference.setEntries(mEntries);
		preference.setEntryValues(mValues);
		preference.setValue(fieldValue);
		preference.setDialogTitle(preference.getTitle());
	}
}