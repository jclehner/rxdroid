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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FractionInputDialog extends AlertDialog implements 
		DialogInterface.OnClickListener, View.OnClickListener, OnLongClickListener, TextWatcher
{
	public interface OnFractionSetListener
	{
		void onFractionSet(FractionInputDialog dialog, Fraction value);		
	}
		
	private static final String TAG = FractionInputDialog.class.getName();
	private static final String PREFKEY_MODE = "_fraction_preference_is_in_mixed_number_mode";
	
	private EditText mInputNumber;
	private EditText mInputNumerator;
	private EditText mInputDenominator;
	
	private Button mButtonPlus;
	private Button mButtonMinus;

	private Button mModeToggler;
	private boolean mIsInMixedNumberMode;	
	
	// the value currently represented in our dialog
	private Fraction mDialogValue = Fraction.ZERO;
	// the actual value, copied from mDialogValue if the user presses
	// the "set" button in the dialog.
	private Fraction mValue = mDialogValue;
	
	private Fraction mLongClickSummand;
	private OnFractionSetListener mOnFractionSetListener;
	private boolean mAllowNegativeValues;
	
	private boolean mIgnoreTextWatcherEvents = false;
	
	private SharedPreferences mSharedPrefs;
	
	public FractionInputDialog(Context context, Fraction value, OnFractionSetListener listener) 
	{
		super(context);
		
		setValue(value);
		setOnFractionSetListener(listener);
		setIcon(android.R.drawable.ic_dialog_dialer);
		
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(R.layout.fraction_input_dialog, null);
				
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		mIsInMixedNumberMode = mSharedPrefs.getBoolean(PREFKEY_MODE, false);
		
		mInputNumber = (EditText) view.findViewById(R.id.number);
		mInputNumber.addTextChangedListener(this);
		mInputNumber.setVisibility(mIsInMixedNumberMode ? View.VISIBLE : View.GONE);
		
		mInputNumerator = (EditText) view.findViewById(R.id.numerator);
		mInputNumerator.addTextChangedListener(this);
		
		mInputDenominator = (EditText) view.findViewById(R.id.denominator);
		mInputDenominator.addTextChangedListener(this);		
		
		mButtonPlus = (Button) view.findViewById(R.id.btn_plus);
		mButtonPlus.setOnClickListener(this);
		
		mButtonMinus = (Button) view.findViewById(R.id.btn_minus);
		mButtonMinus.setOnClickListener(this);

		mModeToggler = (Button) view.findViewById(R.id.btn_mode_toggle);
		mModeToggler.setOnClickListener(this);
		
		setView(view);
		
		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), (OnClickListener) null);
		
		updateInputFields();
	}
	
	public void setValue(Fraction value) 
	{
		mDialogValue = value;
		mValue = value;
	}
	
	public Fraction getValue() {
		return new Fraction(mValue);
	}
	
	public void setAllowNegativeValues(boolean allowNegativeValues) 
	{
		mAllowNegativeValues = allowNegativeValues;
		
		if(!allowNegativeValues)
		{
			mInputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
			mInputNumerator.setInputType(InputType.TYPE_CLASS_NUMBER);			
		}
		else
		{
			if(mIsInMixedNumberMode)
			{
				mInputNumber.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
				mInputNumerator.setInputType(InputType.TYPE_CLASS_NUMBER);				
			}
			else
				mInputNumerator.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);			
		}
	}
	
	public boolean allowsNegativeValues() {
		return mAllowNegativeValues;
	}
	
	public void setOnFractionSetListener(OnFractionSetListener listener) {
		mOnFractionSetListener = listener;
	}
	
	public OnFractionSetListener getOnFractionSetListener() {
		return mOnFractionSetListener;
	}
	
	/**
	 * Sets a value that is added/subtracted from the current value when
	 * long-clicking the +/- buttons.
	 * 
	 * @param value a fraction. Use <code>null</code> to disable the long-click behaviour.
	 */	
	public void setOnLongClickSummand(Fraction summand) 
	{
		OnLongClickListener listener = null;
		
		if((mLongClickSummand = summand) != null)
			listener = this;
		
		mButtonPlus.setOnLongClickListener(listener);
		mButtonMinus.setOnLongClickListener(listener);		
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == BUTTON_POSITIVE)
		{
			mValue = mDialogValue;
			if(mOnFractionSetListener != null)
				mOnFractionSetListener.onFractionSet(this, mValue);
			persistFractionInputMode();
		}		
	}

	@Override
	public void onClick(View view)
	{
		if(view.getId() == R.id.btn_mode_toggle)
		{
			mIsInMixedNumberMode = !mIsInMixedNumberMode;
			mInputNumber.setVisibility(mIsInMixedNumberMode ? View.VISIBLE : View.GONE);
						
			int inputType = InputType.TYPE_CLASS_NUMBER;

			if(!mIsInMixedNumberMode && mAllowNegativeValues)
				inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED;

			mInputNumerator.setInputType(inputType);
			updateInputFields();
			
		}
		else if(view.getId() == R.id.btn_plus)
		{
			setDialogValue(mDialogValue.plus(1));
			updateInputFields();
		}
		else if(view.getId() == R.id.btn_minus)
		{
			setDialogValue(mDialogValue.minus(1));
			updateInputFields();
		}
	}
	
	@Override
	public boolean onLongClick(View view)
	{
		if(mLongClickSummand != null)
		{
			if(view.getId() == R.id.btn_plus)
			{
				setDialogValue(mDialogValue.plus(mLongClickSummand));
				updateInputFields();
				return true;
			}
			else if(view.getId() == R.id.btn_minus)
			{
				setDialogValue(mDialogValue.minus(mLongClickSummand));
				updateInputFields();
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void afterTextChanged(Editable s)
	{
		if(mIgnoreTextWatcherEvents)
			return;
		
		int wholeNum, numerator, denominator;
		
		try
		{
			wholeNum = Integer.parseInt(mInputNumber.getText().toString(), 10);
			numerator = Integer.parseInt(mInputNumerator.getText().toString(), 10);
			denominator = Integer.parseInt(mInputDenominator.getText().toString(), 10);
		}
		catch(NumberFormatException e)
		{
			Log.d(TAG, "afterTextChanged: failed to parse input");
			return;
		}
				
		if(denominator == 0)
		{
			Log.d(TAG, "afterTextChanged: mDialogValue=" + mDialogValue);
			
			Toast toast = Toast.makeText(getContext(), "Denominator must not be zero!", Toast.LENGTH_SHORT);
			// display the Toast on top, as it might get lost when being displayed on the keypad (the default Toast
			// style and the keypad have very similar colors)
			toast.setGravity(Gravity.TOP, toast.getXOffset(), toast.getYOffset());
			toast.show();
			
			mInputDenominator.setText(Integer.toString(mDialogValue.getFractionData(false)[2]));
			mInputDenominator.selectAll();
			return;
		}
				
		try
		{
			setDialogValue(new Fraction(wholeNum, numerator, denominator));
		}
		catch(IllegalArgumentException e)
		{
			Log.d(TAG, "afterTextChanged: ignoring { " + wholeNum + ", " + numerator  + ", " + denominator + " }");			
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}
		
	@Override
	protected void onStart()
	{
		super.onStart();
		
		// taken from Android's DialogPreference.java
		Window window = getWindow();
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	}
	
	private void setDialogValue(Fraction value)
	{
		if(!mAllowNegativeValues && value.compareTo(0) == -1)
			Log.d(TAG, "setDialogValue: ignoring negative value");
		else
		{
			mDialogValue = value;
			updateTitle();
		}
	}

	private void updateInputFields()
	{
		// While updating the EditTexts, the TextWatcher events are handled
		// after every call to setText(), possibly resulting in a state where
		// combining the current EditText values obtained by getText() results
		// in an invalid fraction. To prevent this from happening, any TextWatcher
		// events in this class should be ignored while mIgnoreTextWatcherEvents is
		// set to true.
		mIgnoreTextWatcherEvents = true;
		
		int[] fractionData = mDialogValue.getFractionData(mIsInMixedNumberMode);

		if (mIsInMixedNumberMode)
			mInputNumber.setText(Integer.toString(fractionData[0]));
		else
			mInputNumber.setText("0");

		mInputNumerator.setText(Integer.toString(fractionData[1]));
		mInputDenominator.setText(Integer.toString(fractionData[2]));
		
		mIgnoreTextWatcherEvents = false;
		
		if(!mAllowNegativeValues && mLongClickSummand == null && mDialogValue.minus(1).compareTo(0) == -1)
			mButtonMinus.setEnabled(false);
		else
			mButtonMinus.setEnabled(true);
	}
	
	private void updateTitle() 
	{
		Log.d(TAG, "updateTitle: mDialogValue=" + mDialogValue);
		setTitle(mDialogValue.toString());
	}
	
	private void persistFractionInputMode()
	{
		Editor editor = mSharedPrefs.edit();
		editor.putBoolean(PREFKEY_MODE, mIsInMixedNumberMode);
		editor.commit();
	}
}
