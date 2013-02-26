/**
 * RxDroid - A Medication Reminder
 * Copyright 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
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

package at.jclehner.androidutils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;

public class AdvancedAlertDialog extends AlertDialog
{
	private static final String TAG = AdvancedAlertDialog.class.getSimpleName();

	private OnShowListener mOnShowListener;
	private Button mBtnPositive;
	private Button mBtnNeutral;
	private Button mBtnNegative;

	// Cannot use a plain array here if we want the indexes to be
	// BUTTON_<FOO> values.
	private SparseArray<Object> mBtnActions = new SparseArray<Object>(3);

	public AdvancedAlertDialog(Context context) {
		this(context, true, null);
	}

	public AdvancedAlertDialog(Context context, boolean cancelable, OnCancelListener cancelListener)
	{
		super(context, cancelable, cancelListener);

		setButton(BUTTON_POSITIVE, android.R.string.ok, (OnClickListener) null);
		setButton(BUTTON_NEGATIVE, android.R.string.cancel, (OnClickListener) null);

		super.setOnShowListener(mPrivateOnShowListener);
	}

	public void setButton(int which, int textResId, OnClickListener listener) {
		setButton(which, getContext().getString(textResId), listener);
	}

	@Override
	public void setButton(int whichButton, CharSequence text, 	OnClickListener listener)
	{
		super.setButton(whichButton, text, listener);
		mBtnActions.put(whichButton, listener);
	}

	public void setButton(int whichButton, int textResId, Message msg) {
		setButton(whichButton, getContext().getString(textResId), msg);
	}

	@Override
	public void setButton(int whichButton, CharSequence text, Message msg)
	{
		super.setButton(whichButton, text, msg);
		mBtnActions.put(whichButton, msg);
	}

	@Override
	public void setOnShowListener(OnShowListener listener) {
		mOnShowListener = listener;
	}

	protected String getString(int resId) {
		return getContext().getString(resId);
	}

	protected String getString(int resId, Object... formatArgs) {
		return getContext().getString(resId, formatArgs);
	}

	protected void onShow() {

	}

	private int getButtonWhich(Button b)
	{
		if(b == null)
			throw new NullPointerException();

		if(b == mBtnPositive)
			return BUTTON_POSITIVE;
		else if(b == mBtnNeutral)
			return BUTTON_NEUTRAL;
		else if(b == mBtnNegative)
			return BUTTON_NEGATIVE;

		throw new IllegalArgumentException();
	}

	private final OnShowListener mPrivateOnShowListener = new OnShowListener() {

		@Override
		public void onShow(DialogInterface dialog)
		{
			mBtnPositive = getButton(BUTTON_POSITIVE);
			mBtnNegative = getButton(BUTTON_NEGATIVE);
			mBtnNeutral = getButton(BUTTON_NEUTRAL);

			final Button[] buttons = { mBtnPositive, mBtnNegative, mBtnNeutral };
			for(Button b : buttons)
			{
				if(b != null)
					b.setOnClickListener(mOnButtonClickListener);
			}

			if(mOnShowListener != null)
				mOnShowListener.onShow(dialog);

			AdvancedAlertDialog.this.onShow();
		}
	};

	private final View.OnClickListener mOnButtonClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v)
		{
			final int which = getButtonWhich((Button) v);
			final Object action = mBtnActions.get(which);
			if(action == null)
				return;

			if(action instanceof OnClickListener)
				((OnClickListener) action).onClick(AdvancedAlertDialog.this, which);
			else if(action instanceof Message)
			{
				final Message msg = (Message) action;
				msg.sendToTarget();
			}

			// we do NOT call dismiss() by default
		}
	};

}
