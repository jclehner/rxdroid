/**
 * Copyright (C) 2011, 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid;

import android.content.Context;

public class IntegerInputDialog extends FractionInputDialog
{
	public interface OnIntegerSetListener
	{
		void onIntegerSet(IntegerInputDialog dialog, int value);
	}

	private int mValue;
	private OnIntegerSetListener mListener;

	public IntegerInputDialog(Context context, int value, OnIntegerSetListener listener)
	{
		super(context, new Fraction(value), null);

		mValue = value;
		mListener = listener;

		setOnFractionSetListener(mFractionSetListener);
		mInput.disableFractionInputMode(true);
	}

	public void setOnIntegerSetListener(OnIntegerSetListener listener) {
		mListener = listener;
	}

	public OnIntegerSetListener getOnIntegerSetListener() {
		return mListener;
	}

	public void setIntValue(int value)
	{
		mValue = value;
		super.setValue(new Fraction(mValue));
	}

	public int getIntValue() {
		return mValue;
	}

	@Override
	public Fraction getValue() {
		return new Fraction(mValue);
	}

	@Override
	public final void setOnFractionSetListener(OnFractionSetListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final OnFractionSetListener getOnFractionSetListener() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setValue(Fraction value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setFractionInputMode(int mode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setAutoInputModeEnabled(boolean enabled) {
		throw new UnsupportedOperationException();
	}

	private final OnFractionSetListener mFractionSetListener = new OnFractionSetListener() {

		@Override
		public void onFractionSet(FractionInputDialog dialog, Fraction value)
		{
			if(!value.isInteger())
				throw new RuntimeException("Expected integer, got " + value);

			mValue = value.intValue();
		}
	};
}
