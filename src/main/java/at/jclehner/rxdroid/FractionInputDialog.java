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

package at.jclehner.rxdroid;

import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.view.ViewStub;

public class FractionInputDialog extends AlertDialog implements OnClickListener, FractionInput.OnChangedListener
{
	@SuppressWarnings("unused")
	private static final String TAG = FractionInputDialog.class.getSimpleName();

	public interface OnFractionSetListener
	{
		void onFractionSet(FractionInputDialog dialog, Fraction value);
	}

	protected FractionInput mInput;
	protected ViewStub mFooterStub;

	private Fraction mValue;
	private OnFractionSetListener mListener;

	public FractionInputDialog(Context context, Fraction value, OnFractionSetListener listener)
	{
		super(context);

		final View view = getLayoutInflater().inflate(R.layout.fraction_dialog, null, false);
		mInput = (FractionInput) view.findViewById(R.id.input);
		mFooterStub = (ViewStub) view.findViewById(R.id.footer_stub);
		mInput.setOnChangeListener(this);
		mListener = listener;

		if(value != null)
			setValue(value);

		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), (OnClickListener) null);


		//set

		setView(view);
	}

	public void setValue(Fraction value)
	{
		mInput.setValue(value);
		mValue = value;
	}

	public Fraction getValue() {
		return new Fraction(mValue);
	}

	public void setFractionInputMode(int mode) {
		mInput.setFractionInputMode(mode);
	}

	public void setAutoInputModeEnabled(boolean enabled) {
		mInput.setAutoInputModeEnabled(enabled);
	}

	public void disableFractionInputMode(boolean disabled) {
		mInput.disableFractionInputMode(disabled);
	}

	public void setOnFractionSetListener(OnFractionSetListener listener) {
		mListener = listener;
	}

	public OnFractionSetListener getOnFractionSetListener() {
		return mListener;
	}

	public ViewStub getFooterStub() {
		return mFooterStub;
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		mInput.clearFocus();

		if(which == BUTTON_POSITIVE)
		{
			if(mListener != null)
				mListener.onFractionSet(this, mValue);
		}
	}

	@Override
	public void onFractionChanged(FractionInput widget, Fraction oldValue)
	{
		mValue = widget.getValue();
		//Log.d(TAG, "onChanged: value=" + mValue);
	}
}
