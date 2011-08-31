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
import android.graphics.Color;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * An EditText field to input fractions.
 *
 * This class enables the user to input fractions. Once the EditText is focused/touched,
 * a Dialog will open, presenting the user with a method of typing fractions.
 *
 * @author Joseph Lehner
 *
 */

public class EditFraction extends EditText implements OnTouchListener
{
	private static final String TAG = EditFraction.class.getName();

	private FractionPickerDialog mDialog;

	public EditFraction(Context context)
	{
		super(context);
		setup(context, null);
	}

	public EditFraction(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setup(context, attrs);
	}

	public EditFraction(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setup(context, attrs);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		boolean ret = super.onKeyDown(keyCode, event);
		Log.d(TAG, "onKeyDown: keyCode=" + keyCode + ", event=" + event);
		return ret;
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event)
	{
		boolean ret = super.onKeyLongPress(keyCode, event);
		Log.d(TAG, "onKeyLongPress: keyCode=" + keyCode + ", event=" + event);
		return ret;
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event)
	{
		boolean ret = super.onKeyMultiple(keyCode, count, event);
		Log.d(TAG, "onKeyMultiple: keyCode=" + keyCode + ", count=" + count + ", event=" + event);
		return ret;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		boolean ret = super.onKeyUp(keyCode, event);
		Log.d(TAG, "onKeyDown: keyCode=" + keyCode + ", event=" + event);
		return ret;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		requestFocus();
		mDialog.show();
		return true;
	}

	private void setup(Context context, AttributeSet attrs)
	{
		if(isClickable())
		{
			mDialog = new FractionPickerDialog(context, this);
			setHint(null);

			InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getWindowToken(), 0);
			setInputType(InputType.TYPE_NULL);
			setOnTouchListener(this);
		}
		else
			Log.d(TAG, "Not clickable. Ignoring");
	}

	public static class FractionPickerDialog extends AlertDialog implements OnKeyboardActionListener
	{
		private TextView mParent;

		private View mButtonPlus;
		private View mButtonMinus;
		private EditText mFractionInput;
		private StringBuffer mText;
		private KeyboardView mKeypad;

		private Fraction mValue = new Fraction(0);
		private boolean mAllowNegative = false;

		public FractionPickerDialog(Context context, TextView parent)
		{
			super(context, true, null);

			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.fraction_picker_dialog, null);
			setView(view);

			mParent = parent;

			CharSequence hint = mParent.getHint();
			if(hint != null && hint.length() != 0)
				setTitle(hint);

			mButtonPlus = view.findViewById(R.id.fraction_input_btn_plus);
			mButtonMinus = view.findViewById(R.id.fraction_input_btn_minus);
			mFractionInput = (EditText) view.findViewById(R.id.fraction_input);
			mKeypad = (KeyboardView) view.findViewById(R.id.fraction_picker_keypad);

			mText = new StringBuffer(mParent.getText());

			android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
					if(!hasValidFraction())
					{
						if(mText.length() != 0)
							return;
						mText.append("0");
						updateFractionInput();
					}

					switch(v.getId())
					{
						case R.id.fraction_input_btn_plus:
							mValue = mValue.plus(1);
							break;
						case R.id.fraction_input_btn_minus:
						{
							if(mAllowNegative || mValue.doubleValue() >= 1.0)
								mValue = mValue.minus(1);
							else
								mValue = new Fraction(0);

							break;
						}
						default:
							return;
					}
					mFractionInput.setText(mValue.toString());
				}
			};

			mButtonPlus.setOnClickListener(onClickListener);
			mButtonMinus.setOnClickListener(onClickListener);

			InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mFractionInput.getWindowToken(), 0);

			mKeypad.setKeyboard(new Keyboard(context, R.xml.fraction));
			mKeypad.setOnKeyboardActionListener(this);

			setIcon(android.R.drawable.ic_dialog_dialer);

			updateFractionInput();
		}

		@Override
		public void onKey(int primaryCode, int[] keyCodes)
		{
			mText = new StringBuffer(mFractionInput.getText());

			switch(primaryCode)
			{
				// TODO export this to R
				case -5:
				{
					if(mText.length() == 0)
						return;
					mText.delete(mText.length() - 1, mText.length());
					break;
				}
				case 10:
				{
					if(hasValidFraction())
						mParent.setText(mValue.toString());

					dismiss();
					return;
				}
				case -1:
				{
					/*
					AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

					builder.setIcon(android.R.drawable.ic_dialog_info);
					builder.setTitle("Help");
					builder.setMessage("This input field accepts fraction values. Examples for valid input include '1 1/4', '5/4' or '5' " +
							"(not including the single quotes).");

					builder.show();
					*/
					break;
				}
				case '-':
				{
					if(!mAllowNegative)
						break;
					// fall through otherwise
				}

				default:
					mText.insert(mText.length(), Character.toString((char) primaryCode));
			}

			updateFractionInput();
		}

		@Override
		public void onPress(int primaryCode) {}

		@Override
		public void onRelease(int primaryCode) {}

		@Override
		public void onText(CharSequence text) {}

		@Override
		public void swipeDown() {}

		@Override
		public void swipeLeft() {}

		@Override
		public void swipeRight() {}

		@Override
		public void swipeUp() {}

		@Override
		protected void onStart()
		{
			mText = new StringBuffer(mParent.getText());
			updateFractionInput();
		}

		private boolean hasValidFraction()
		{
			try
			{
				Fraction.decode(mText.toString());

				return true;
			}
			catch(NumberFormatException e)
			{
				return false;
			}

		}

		private void updateFractionInput()
		{
			try
			{
				mValue = Fraction.decode(mText.toString());
				//mFractionInput.setText(mValue.toString());
				//mFractionInput.setTextAppearance(context, resid)
				//mFractionInput.setBackgroundDrawable(mFractionInputBackground);
				mFractionInput.setTextColor(Color.BLACK);
				//mFractionInput.set
			}
			catch(NumberFormatException e)
			{
				//mFractionInput.setBackgroundColor(Color.WHITE);
				mFractionInput.setTextColor(Color.RED);
			}

			mFractionInput.setText(mText);
			//mFractionInput.setT
		}
	}
}


