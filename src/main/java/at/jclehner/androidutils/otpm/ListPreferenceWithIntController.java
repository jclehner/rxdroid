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

import android.preference.ListPreference;
import at.jclehner.rxdroid.util.Util;

public abstract class ListPreferenceWithIntController extends DialogPreferenceController<ListPreference, Integer>
{
	private String[] mEntries;
	private int mEntriesResId;

	public ListPreferenceWithIntController(int entriesResId) {
		mEntriesResId = entriesResId;
	}

	public ListPreferenceWithIntController(String[] entries)
	{
		if((mEntries = entries) == null)
			throw new NullPointerException();
	}

	@Override
	public void initPreference(ListPreference preference, Integer fieldValue)
	{
		super.initPreference(preference, fieldValue);

		if(mEntries == null)
			mEntries = preference.getContext().getResources().getStringArray(mEntriesResId);

		preference.setEntries(mEntries);
		Util.populateListPreferenceEntryValues(preference);
		preference.setValueIndex(fieldValue);
		preference.setDialogTitle(preference.getTitle());
	}

	/*@Override
	public boolean updatePreference(ListPreference preference, Integer newValue)
	{
		preference.setValueIndex(newValue);
		return super.updatePreference(preference, newValue);
	}*/

	@Override
	public Integer toFieldType(Object prefValue) {
		return Integer.valueOf((String) prefValue, 10);
	}

	@Override
	public void updateSummary(ListPreference preference, Integer newValue)
	{
		preference.setSummary(mEntries[newValue]);
	}

	@Override
	public void onFieldValueSet(Integer value) {
		getPreference().setValueIndex(value);
	}
}