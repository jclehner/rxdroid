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

import java.io.Serializable;
import java.util.NoSuchElementException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import at.caspase.androidutils.InstanceState.SaveState;


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
public abstract class MyDialogPreference<T extends Serializable> extends DialogPreference
{
	private static final String TAG = MyDialogPreference.class.getName();
	private static final boolean LOGV = false;

	private static final String EMPTY = "<!!!!!!!!!!!!!!!!!!!!!!EMPTY";

	@SaveState
	private CharSequence mNeutralButtonText;

	@SaveState
	private T mValue;

	@SaveState
	private T mDefaultValue;

	//@SaveState
	private Dialog mDialog;

	private static final String KEY_IS_DIALOG_SHOWING = "is_showing";

	public MyDialogPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MyDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.preferenceStyle);
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

	/**
	 * Sets the internal value represented by this Preference.
	 * <p>
	 * Calling this function will also persist the value, if
	 * enabled. Note that the summary will be automatically
	 * updated with the result of <code>value.toString()</code>.
	 * The summary may be changed again by overriding {@link #onValueSet(Object)}.
	 */
	public final void setValue(T value)
	{
		mValue = value;
		setSummary(getSummary());

		onValueSet(value);

		if(shouldPersist())
			persistString(toPersistedString(value));
	}

	/**
	 * Similar to {@link #setValue(Object)}, but also calls the <code>OnChangeListener</code>
	 * associated with this Preference.
	 *
	 * @param value
	 * @see #setValue(Object)
	 */
	public final void changeValue(T value)
	{
		if(callChangeListener(value))
		{
			setValue(value);
			notifyChanged();
		}
	}

	public final T getValue()
	{
		if(mValue == null)
		{
			String persisted = getPersistedString(EMPTY);
			if(persisted != null && persisted != EMPTY) // the != operator is intentional!
				mValue = fromPersistedString(persisted);
			else
			{
				Log.w(TAG, "getValue: key=" + getKey() + " persisted string was null or not available");
				return mDefaultValue;
			}
		}

		return mValue;
	}

	@Override
	public final Dialog getDialog() {
		return mDialog;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		onDialogClosed(which == Dialog.BUTTON_POSITIVE);
	}

	protected abstract String toPersistedString(T value);
	protected abstract T fromPersistedString(String string);

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index)
	{
		final String string = a.getString(index);
		final T newValue = fromPersistedString(string);

		mDefaultValue = newValue;
		return newValue;
	}

	@Override
	protected void onBindDialogView(View view)
	{
		if(view != null)
		{
			final ViewParent parent = view.getParent();
			if(parent != null && parent instanceof ViewGroup)
				((ViewGroup) parent).removeView(view);
		}

		super.onBindDialogView(view);
	}

	protected abstract T getDialogValue();

	protected void onValueSet(T value) {
		// empty
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
	 * Using this function requires care, as the following functions will <em>never</em>
	 * be called:
	 * <ul>
	 * <li>{@link #onPrepareDialogBuilder(android.app.AlertDialog.Builder)}</li>
	 * <li>{@link #onCreateDialogView()}</li>
	 * <li>{@link #onDialogClosed(boolean)}<sup>*</sup></li>
	 * </ul>
	 *
	 * Also note that dialog properties returned by functions such as
	 * {@link #getDialogTitle()} will <em>not</em> be automatically
	 * applied if using a custom dialog.
	 * <p>
	 * <sup>*)</sup> This function <em>might</em> be called, if you've used the
	 * {@link OnClickListener} returned by {@link #getDialogOnClickListener()}
	 * for your buttons.
	 *
	 * @return the custom dialog, or <code>null</code> by default.
	 */
	protected Dialog onGetCustomDialog() {
		return null;
	}

	protected int onGetSoftInputMode() {
		return 0;
	}

	/**
	 * Called before the dialog is actually shown.
	 * <p>
	 * This function may be used to set extra options on dialogs
	 * not created via {@link #onGetCustomDialog()}.
	 * <p>
	 * Note that you cannot use {@link Dialog#setOnDismissListener(OnDismissListener)}, as
	 * this listener is used internally by this class and will be overridden.
	 *
	 * @param dialog
	 */
	protected void onCustomizeDialog(Dialog dialog) {
		// do nothing
	}

	/**
	 * Called when the dialog is dismissed.
	 * <p>
	 * Due to technical reasons, you cannot call {@link Dialog#setOnDismissListener(OnDismissListener)}
	 * in {@link #onCustomizeDialog(Dialog)} or {@link #onGetCustomDialog()}. Use this function instead
	 * do perform any additional cleanup.
	 */
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
	}

	@Override
	protected final void showDialog(Bundle state)
	{
		if(state != null)
			Log.w(TAG, "showDialog: ignoring non-null state");

		mDialog = onGetCustomDialog();
		if(mDialog == null)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
					.setTitle(getDialogTitle())
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


		int softInputMode = onGetSoftInputMode();
		if(softInputMode != 0)
		{
			Window window = mDialog.getWindow();
			window.setSoftInputMode(softInputMode);
		}

		onCustomizeDialog(mDialog);

		mDialog.setOnDismissListener(mDismissListener);
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
		final boolean isShowing = mDialog != null;
		if(isShowing)
			mDialog.dismiss();

		Parcelable superState = super.onSaveInstanceState();
		Bundle extras = new Bundle();
		extras.putBoolean(KEY_IS_DIALOG_SHOWING, isShowing);

		return InstanceState.createFrom(this, superState, extras);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		super.onRestoreInstanceState(InstanceState.getSuperState(state));
		InstanceState.restoreTo(this, state);

		Bundle extras = InstanceState.getExtras(state);
		if(extras != null)
		{
			if(extras.getBoolean(KEY_IS_DIALOG_SHOWING, false))
				showDialog(null);
		}
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if(positiveResult)
			changeValue(getDialogValue());
	}

	private final OnDismissListener mDismissListener = new OnDismissListener() {

		@Override
		public void onDismiss(DialogInterface dialog)
		{
			MyDialogPreference.this.onDismiss(mDialog);
			mDialog = null;
		}
	};
}
