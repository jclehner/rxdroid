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

package at.jclehner.androidutils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public abstract class AlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener
{
	private static final String TAG = AlertDialogFragment.class.getSimpleName();

	private Button mPositiveBtn;
	private Button mNeutralBtn;
	private Button mNegativeBtn;

	private CharSequence mTitle;
	private CharSequence mMessage;

	public void setTitle(CharSequence title)
	{
		mTitle = title;

		final AlertDialog dialog = getDialog();
		if(dialog == null)
			return;

		dialog.setTitle(title);
	}

	public CharSequence getTitle() {
		return mTitle;
	}

	public void setMessage(CharSequence message)
	{
		mMessage = message;

		final AlertDialog dialog = getDialog();
		if(dialog == null)
			return;

		dialog.setMessage(message);
	}

	public CharSequence getMessage() {
		return mMessage;
	}

	public CharSequence getPositiveButtonText() {
		return getString(android.R.string.ok);
	}

	public CharSequence getNeutralButtonText() {
		return null;
	}

	public CharSequence getNegativeButtonText() {
		return getString(android.R.string.cancel);
	}

	public int getIcon() {
		return 0;
	}

	@Override
	public final Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
		ab.setTitle(getTitle());
		ab.setMessage(getMessage());
		ab.setIcon(getIcon());

		// listeners are null because we use View.OnClickListener instead
		// of DialogInterface.OnClickListener
		ab.setPositiveButton(getPositiveButtonText(), null);
		ab.setNeutralButton(getNeutralButtonText(), null);
		ab.setNegativeButton(getNegativeButtonText(), null);

		onPrepareDialogBuilder(ab);

		final AlertDialog dialog = ab.create();
		dialog.setOnShowListener(mOnShowListener);

		return dialog;
	}

	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return null;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		dismiss();
	}

	public void onShow() {
		// stub
	}

	@Override
	public AlertDialog getDialog() {
		return (AlertDialog) super.getDialog();
	}

	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		// stub
	}

	protected Button getButton(int which)
	{
		switch(which)
		{
			case Dialog.BUTTON_POSITIVE:
				return mPositiveBtn;

			case Dialog.BUTTON_NEUTRAL:
				return mNeutralBtn;

			case Dialog.BUTTON_NEGATIVE:
				return mNegativeBtn;

			default:
				throw new IllegalArgumentException();
		}
	}

	private final OnShowListener mOnShowListener = new OnShowListener() {

		@Override
		public void onShow(DialogInterface dialogInterface)
		{
			final AlertDialog dialog = (AlertDialog) dialogInterface;

			mPositiveBtn = dialog.getButton(Dialog.BUTTON_POSITIVE);
			mNeutralBtn = dialog.getButton(Dialog.BUTTON_NEUTRAL);
			mNegativeBtn = dialog.getButton(Dialog.BUTTON_NEGATIVE);

			mPositiveBtn.setOnClickListener(mOnButtonClickListener);
			mNeutralBtn.setOnClickListener(mOnButtonClickListener);
			mNegativeBtn.setOnClickListener(mOnButtonClickListener);

			AlertDialogFragment.this.onShow();
		}
	};

	private final OnClickListener mOnButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			final int which;

			if(v == mPositiveBtn)
				which = Dialog.BUTTON_POSITIVE;
			else if(v == mNeutralBtn)
				which = Dialog.BUTTON_NEUTRAL;
			else if(v == mNegativeBtn)
				which = Dialog.BUTTON_NEGATIVE;
			else
			{
				Log.w(TAG, "Button's View.OnClickListener called with unknown view argument");
				return;
			}

			AlertDialogFragment.this.onClick(getDialog(), which);
		}
	};

}
