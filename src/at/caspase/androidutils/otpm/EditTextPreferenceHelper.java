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

package at.caspase.androidutils.otpm;

import android.preference.EditTextPreference;
import android.widget.EditText;

public class EditTextPreferenceHelper extends PreferenceHelper<EditTextPreference, String>
{
	@Override
	public void initPreference(EditTextPreference pref, String value)
	{
		pref.setText(value);
		EditText editText = pref.getEditText();
		editText.setHint(pref.getTitle());
	}

	@Override
	public boolean updatePreference(EditTextPreference preference, Object fieldValue)
	{
		String newText = (String) fieldValue;
		preference.setSummary(newText);
		setFieldValue(newText);
		return true;
	}

	@Override
	public boolean isPreferenceDisabled() {
		return false;
	}
}