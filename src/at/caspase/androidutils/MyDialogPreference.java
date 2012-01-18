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

package at.caspase.androidutils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.Window;
import at.caspase.androidutils.StateSaver.SaveState;


/**
 * Improved DialogPreference class.
 * <p>
 * This class, while retaining all functionality of the original DialogPreference,
 * allows for more customized dialogs.
 *
 *
 * @author Joseph Lehner
 *
 */
public class MyDialogPreference extends DialogPreference implements OnDismissListener
{
	private static final String TAG = MyDialogPreference.class.getName();

	private static final boolean LOGV = true;

	@SaveState
	private CharSequence mNeutralButtonText;
	@SaveState
	private int mSoftInputMode = 0;

	private Dialog mDialog;

	private static final String KEY_IS_DIALOG_SHOWING = "is_showing";

	public MyDialogPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MyDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.dialogPreferenceStyle);
	}

	public void setNeutralButtonText(CharSequence text) {
		mNeutralButtonText = text;
	}

	public void setNeutralButtonText(int resId) {
		setNeutralButtonText(getContext().getString(resId));
	}

	public CharSequence getNeutralButtonText() {
		return mNeutralButtonText;
	}

	@Override
	public Dialog getDialog() {
		return mDialog;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		mDialog = null;
	}

	public boolean isDialogShowing() {
		return mDialog != null;
	}

	/**
	 * Returns a custom dialog, if present.
	 * <p>
	 * Note that if overriding this function, you must do all
	 * initialization before returning. If this function returns
	 * something other than <code>null</code>,
	 * {@link #onPrepareDialogBuilder(android.app.AlertDialog.Builder)}
	 * will never be called. Also note that an OnDismissListener
	 * set on the returned dialog will be overridden in {@link #showDialog(Bundle)}!
	 * <p>
	 * Also note that dialog properties returned by functions such as
	 * {@link #getDialogTitle()} will <em>not</em> be automatically
	 * applied if using a custom dialog.
	 *
	 * @return the custom dialog, or <code>null</code> by default.
	 */
	protected Dialog onGetCustomDialog() {
		return null;
	}

	/**
	 * Called before the dialog is actually shown.
	 *
	 * @param dialog
	 */
	protected void onShowDialog(Dialog dialog) {
		// stub
	}

	@Override
	protected void showDialog(Bundle state)
	{
		if(state != null)
			Log.w(TAG, "showDialog: ignoring non-null state");

		mDialog = onGetCustomDialog();
		if(mDialog == null)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
					.setTitle(getTitle())
					.setIcon(getDialogIcon())
					.setPositiveButton(getPositiveButtonText(), this)
					.setNeutralButton(getNeutralButtonText(), this)
					.setNegativeButton(getNegativeButtonText(), this)
			;

			View contentView = onCreateDialogView();
			if(contentView != null)
			{
				onBindDialogView(contentView);
				builder.setView(contentView);
			}
			else
				builder.setMessage(getDialogMessage());

			onPrepareDialogBuilder(builder);
			mDialog = builder.create();
		}

		if(mSoftInputMode != 0)
		{
			Window window = mDialog.getWindow();
			window.setSoftInputMode(mSoftInputMode);
		}

		onShowDialog(mDialog);

		mDialog.setOnDismissListener(this);
		mDialog.show();
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		// do nothing
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		// This might be a hack, but it's the only way I've found to work
		// around those pesky 'Window leaked by Activity' errors.
		boolean isShowing = isDialogShowing();
		if(isShowing)
			getDialog().dismiss();

		Parcelable superState = super.onSaveInstanceState();
		Bundle extras = new Bundle();
		extras.putBoolean(KEY_IS_DIALOG_SHOWING, isShowing);

		return StateSaver.createInstanceState(this, superState, extras);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		super.onRestoreInstanceState(StateSaver.getSuperState(state));
		StateSaver.restoreInstanceState(this, state);

		Bundle extras = StateSaver.getExtras(state);
		if(extras != null)
		{
			if(extras.getBoolean(KEY_IS_DIALOG_SHOWING, false))
				showDialog(null);
		}
	}
}
