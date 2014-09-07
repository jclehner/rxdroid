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

package at.jclehner.rxdroid.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import at.jclehner.rxdroid.util.CollectionUtils;

public class AutoSummaryListPreference extends ListPreference
{
	private static final String TAG = AutoSummaryListPreference.class.getSimpleName();

	public AutoSummaryListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected boolean callChangeListener(Object newValue)
	{
		if(super.callChangeListener(newValue))
		{
			setSummaryFromValue((String) newValue);
			return true;
		}

		return false;
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
	{
		Log.d(TAG, getKey() + " onSetIntialValue(" + restoreValue + ", " + defaultValue);
		super.onSetInitialValue(restoreValue, defaultValue);
		setSummaryFromValue(getValue());
	}

	private void setSummaryFromValue(String value)
	{
		final CharSequence[] entries = getEntries();
		final CharSequence[] values = getEntryValues();

		int index = CollectionUtils.indexOf(value, values);
		if(index != -1 && index < entries.length)
			setSummary(entries[index]);
		else
			Log.d(TAG, "setSummaryFromValue: no entry for value " + value);
	}
}
