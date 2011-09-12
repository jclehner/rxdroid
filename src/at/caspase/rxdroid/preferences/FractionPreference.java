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

package at.caspase.rxdroid.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.util.Log;
import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.FractionInputDialog;
import at.caspase.rxdroid.FractionInputDialog.OnFractionSetListener;

public class FractionPreference extends Preference implements OnPreferenceClickListener, OnFractionSetListener
{
	private static final String TAG = FractionPreference.class.getName();
	
	private Fraction mValue;
	
	public FractionPreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public FractionPreference(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
		
		mValue = Fraction.decode(getPersistedString("0"));
		setOnPreferenceClickListener(this);
	}
	
	public void setValue(Fraction value) {
		mValue = value;
	}
	
	public Fraction getValue() {
		return mValue;
	}
	
	@Override
	public CharSequence getSummary() {
		return mValue.toString();
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		FractionInputDialog dialog = new FractionInputDialog(getContext(), mValue, this);
		dialog.show();
		return true;
	}

	@Override
	public void onFractionSet(FractionInputDialog dialog, Fraction value)
	{
		boolean canPersist = callChangeListener(value);
		
		if(canPersist && shouldPersist())
			persistString(value.toString());
		
		mValue = dialog.getValue();		
		setSummary(mValue.toString());
		notifyChanged();
		
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) 
	{
		String value = a.getString(index);
		Log.d(TAG, "onGetDefaultValue: value=" + value);
		return value != null ? value : "0"; 
	}
}
