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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

public class EditTextPreferenceHelper extends PreferenceHelper<EditTextPreference, String>
{
	@Override
	public void initPreference(EditTextPreference pref, String value)
	{
		pref.setText(value);
		EditText editText = pref.getEditText();
		editText.setHint(pref.getTitle());

		Dialog dialog = pref.getDialog();
		if(dialog != null)
		{
			Log.d("FOOOOOBAR", "Houston, we have a Dialog!");
			dialog.setOnShowListener(mShowListener);
		}
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




	private final OnShowListener mShowListener = new OnShowListener() {

		@Override
		public void onShow(DialogInterface dialog)
		{
			Window window = ((AlertDialog) dialog).getWindow();
			if(window != null)
			{
				window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED |
						WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

			}
		}
	};
}