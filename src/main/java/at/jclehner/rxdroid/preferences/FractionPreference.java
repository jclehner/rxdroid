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

import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import at.jclehner.androidutils.InstanceState;
import at.jclehner.androidutils.InstanceState.SaveState;
import at.jclehner.androidutils.AdvancedDialogPreference;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.FractionInputDialog;
import at.jclehner.rxdroid.FractionInputDialog.OnFractionSetListener;

/**
 * A preference for storing fractions.
 *
 * @author Joseph Lehner
 *
 */
public class FractionPreference extends BaseAdvancedDialogPreference<Fraction> implements OnFractionSetListener
{
	private static final String TAG = FractionPreference.class.getSimpleName();

	private static final String KEY_VALUE = "value";

	@SaveState
	private Fraction mDialogValue;
	@SaveState
	private boolean mDisableFractionInputMode = false;

	public FractionPreference(Context context) {
		this(context, null);
	}

	public FractionPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}

	public FractionPreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs);
		mDialogValue = getValue();
	}

	public void disableFractionInputMode(boolean disable) {
		mDisableFractionInputMode = disable;
	}

	@Override
	public CharSequence getSummary()
	{
		CharSequence summary = super.getSummary();
		if(summary == null)
		{
			Object value = getValue();
			if(value != null)
				return value.toString();

			return null;
		}

		return summary;
	}

	@Override
	public void onFractionSet(FractionInputDialog dialog, Fraction value) {
		changeValue(value);
	}

	@Override
	protected Fraction fromPersistedString(String string) {
		return Fraction.valueOf(string);
	}

	@Override
	protected String toPersistedString(Fraction value) {
		return value.toString();
	}

	@Override
	protected void onValueSet(Fraction value) {
		mDialogValue = value;
	}

	@Override
	protected Fraction getDialogValue() {
		return mDialogValue;
	}

	@Override
	protected Dialog onGetCustomDialog()
	{
		FractionInputDialog dialog = new FractionInputDialog(getContext(), mDialogValue, this);
		dialog.setTitle(getDialogTitle());
		dialog.setIcon(getDialogIcon());
		if(!mDisableFractionInputMode)
			dialog.setAutoInputModeEnabled(true);
		else
			dialog.disableFractionInputMode(true);

		return dialog;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index)
	{
		String value = a.getString(index);
		Log.d(TAG, "onGetDefaultValue: value=" + value);
		return value != null ? value : "0";
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		Bundle extras = new Bundle();
		FractionInputDialog dialog = (FractionInputDialog) getDialog();
		final Fraction value;

		if(dialog != null)
			value = dialog.getValue();
		else
			value = mDialogValue;

		extras.putSerializable(KEY_VALUE, value);
		Parcelable superState = super.onSaveInstanceState();
		return InstanceState.createFrom(this, superState, extras);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		InstanceState.restoreTo(this, state);

		Bundle extras = InstanceState.getExtras(state);
		if(extras != null)
		{
			mDialogValue = (Fraction) extras.getSerializable(KEY_VALUE);
			if(mDialogValue == null)
				mDialogValue = new Fraction();
		}

		super.onRestoreInstanceState(InstanceState.getSuperState(state));
	}
}
