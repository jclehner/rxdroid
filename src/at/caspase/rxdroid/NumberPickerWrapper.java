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

package at.caspase.rxdroid;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

/**
 * Simple wrapper for a NumberPicker.
 * <p>
 * If using this class on a pre-Honeycomb device, a custom implementation
 * by <a href="https://github.com/mrn/numberpicker">Mike Novak</a> is used. This
 * class implements only a very limited subset of the functions
 * provided by the <code>android.widget.NumberPicker</code> widget introduced in Honeycomb.
 * In theory, this class should be replaceable with said widget,
 * requiring no changes to the source (other than the type specifications of course).
 *
 * @see android.widget.NumberPicker
 * @author Joseph Lehner
 *
 */
@TargetApi(11)
public class NumberPickerWrapper extends LinearLayout
{
	private static final int MIN_DEFAULT = 0;
	private static final int MAX_DEFAULT = 99999;

	private final com.michaelnovakjr.numberpicker.NumberPicker mNumberPickerOld;
	private final android.widget.NumberPicker mNumberPickerNew;

	// As the old NumberPicker only provides a setRange method,
	// we have to store the value that we're currently NOT setting, so
	// that it can be passed to setRange when using setMinValue or
	// setMaxValue
	private int mMinValue;
	private int mMaxValue;

	private boolean mWrapSelectorWheel;

	private OnValueChangeListener mListener;

	public interface OnValueChangeListener
	{
		void onValueChange(NumberPickerWrapper picker, int oldVal, int newVal);
	}

	public NumberPickerWrapper(Context context) {
		this(context, null, 0);
	}

	public NumberPickerWrapper(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NumberPickerWrapper(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs);

		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.number_picker_wrapper, this, true);

		if(Version.SDK_IS_PRE_HONEYCOMB)
		{
			mNumberPickerNew = null;
			mNumberPickerOld =
					(com.michaelnovakjr.numberpicker.NumberPicker) findViewById(R.id.picker);
		}
		else
		{
			mNumberPickerOld = null;
			mNumberPickerNew =
					(android.widget.NumberPicker) findViewById(R.id.picker);

			mNumberPickerNew.setSelected(false);
		}

		if(!isInEditMode())
		{
			setMinValue(MIN_DEFAULT);
			setMaxValue(MAX_DEFAULT);
		}
	}

	public int getValue()
	{
		if(Version.SDK_IS_PRE_HONEYCOMB)
			return mNumberPickerOld.getCurrent();
		else
			return mNumberPickerNew.getValue();
	}

	public void setValue(int value)
	{
		if(Version.SDK_IS_PRE_HONEYCOMB)
			mNumberPickerOld.setCurrentAndNotify(value);
		else
			mNumberPickerNew.setValue(value);
	}

	public int getMinValue() {
		return mMinValue;
	}

	public void setMinValue(int minValue)
	{
		if(Version.SDK_IS_PRE_HONEYCOMB)
		{
			mNumberPickerOld.setRange(mMinValue = minValue, mMaxValue);
			maybeEnableWrap();
		}
		else
			mNumberPickerNew.setMinValue(mMinValue = minValue);
	}

	public int getMaxValue() {
		return mMaxValue;
	}

	public void setMaxValue(int maxValue)
	{
		if(Version.SDK_IS_PRE_HONEYCOMB)
		{
			mNumberPickerOld.setRange(mMinValue, mMaxValue = maxValue);
			maybeEnableWrap();
		}
		else
			mNumberPickerNew.setMaxValue(mMaxValue = maxValue);
	}

	public boolean getWrapSelectorWheel() {
		return mWrapSelectorWheel;
	}

	public void setWrapSelectorWheel(boolean wrapSelectorWheel)
	{
		if(Version.SDK_IS_PRE_HONEYCOMB)
			mNumberPickerOld.setWrap(mWrapSelectorWheel = wrapSelectorWheel);
		else
			mNumberPickerNew.setWrapSelectorWheel(mWrapSelectorWheel = wrapSelectorWheel);
	}

	public OnValueChangeListener getOnValueChangeListener() {
		return mListener;
	}

	public void setOnValueChangeListener(OnValueChangeListener l)
	{
		mListener = l;

		if(Version.SDK_IS_PRE_HONEYCOMB)
		{
			if(l == null)
			{
				mNumberPickerOld.setOnChangeListener(null);
				return;
			}

			mNumberPickerOld.setOnChangeListener(
					new com.michaelnovakjr.numberpicker.NumberPicker.OnChangedListener() {

				@Override
				public void onChanged(com.michaelnovakjr.numberpicker.NumberPicker picker,
						int oldVal, int newVal)
				{
					mListener.onValueChange(NumberPickerWrapper.this, oldVal, newVal);
				}
			});
		}
		else
		{
			if(l == null)
			{
				mNumberPickerNew.setOnValueChangedListener(null);
				return;
			}

			mNumberPickerNew.setOnValueChangedListener(
					new android.widget.NumberPicker.OnValueChangeListener() {

				@Override
				public void onValueChange(android.widget.NumberPicker picker,
						int oldVal, int newVal)
				{
					mListener.onValueChange(NumberPickerWrapper.this, oldVal, newVal);
				}
			});
		}
	}

	public void setOnLongPressUpdateInterval(long intervalMillis)
	{
		if(Version.SDK_IS_PRE_HONEYCOMB)
			mNumberPickerOld.setSpeed(intervalMillis);
		else
			mNumberPickerNew.setOnLongPressUpdateInterval(intervalMillis);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);

		if(Version.SDK_IS_PRE_HONEYCOMB)
			mNumberPickerOld.setEnabled(enabled);
		else
			mNumberPickerNew.setEnabled(enabled);
	}

	private void maybeEnableWrap()
	{
		if(Version.SDK_IS_PRE_HONEYCOMB)
		{
			if(Math.abs(mMaxValue - mMinValue) > 5)
				setWrapSelectorWheel(true);
		}
	}
}
