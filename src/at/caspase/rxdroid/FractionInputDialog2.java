/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.caspase.rxdroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;

public class FractionInputDialog2 extends AlertDialog implements OnClickListener, FractionInput.OnChangedListener
{
	private static final String TAG = FractionInputDialog2.class.getName();

	public interface OnFractionSetListener
	{
		void onFractionSet(FractionInputDialog2 dialog, Fraction value);
	}

	private FractionInput mInput;
	private Fraction mValue;
	private OnFractionSetListener mListener;

	public FractionInputDialog2(Context context, Fraction value, OnFractionSetListener listener)
	{
		super(context);

		mInput = new FractionInput(context, null);
		mInput.setOnChangeListener(this);
		mInput.setValue(value);

		mValue = value;

		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel),
				(OnClickListener) null);

		setView(mInput);
	}

	public void setOnFractionSetListener(OnFractionSetListener listener) {
		mListener = listener;
	}

	public OnFractionSetListener getOnFractionSetListener() {
		return mListener;
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == BUTTON_POSITIVE)
		{
			Log.d(TAG, "onClick: mListener=" + mListener);
			if(mListener != null)
				mListener.onFractionSet(this, mValue);
		}
	}

	@Override
	public void onChanged(FractionInput widget, Fraction oldValue)
	{
		mValue = widget.getValue();
		Log.d(TAG, "onChanged: value=" + mValue);
	}
}
