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
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Settings;

public class TogglableTimePeriodPreference extends TimePeriodPreference
{
	@SuppressWarnings("unused")
	private static final String TAG = TimePeriodPreference.class.getSimpleName();

	private CompoundButton mToggler;
	private boolean mChecked = true;

	public TogglableTimePeriodPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs, android.R.attr.dialogPreferenceStyle);
		setWidgetLayoutResource(R.layout.toggler);
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		mToggler = (CompoundButton) view.findViewById(android.R.id.checkbox);
		mToggler.setClickable(true);
		mToggler.setFocusable(true);
		mToggler.setChecked(mChecked);
		mToggler.setOnCheckedChangeListener(mToggleListener);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
	{
		super.onSetInitialValue(restorePersistedValue, defaultValue);
		setChecked(Settings.isChecked(getKey(), false));
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		super.onDialogClosed(positiveResult);

		if(!positiveResult && mToggler != null)
			mToggler.setChecked(false);
	}

	private void setChecked(boolean checked)
	{
		if(!checked)
			setSummaryInternal(getContext().getString(R.string._title_disabled));

		Settings.setChecked(getKey(), checked);
		mChecked = checked;
	}

	private final OnCheckedChangeListener mToggleListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			setChecked(isChecked);

			if(mChecked)
				showDialog(null);
		}
	};
}
