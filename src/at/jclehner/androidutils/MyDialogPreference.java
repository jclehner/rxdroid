/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
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

import java.io.Serializable;

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
import android.view.Window;
import at.jclehner.androidutils.InstanceState.SaveState;
import at.jclehner.rxdroid.FractionInput.OnChangedListener;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.util.Util;


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
	private static final String TAG = MyDialogPreference.class.getSimpleName();
	private static final boolean LOGV = true;

	private static final String EMPTY = "<!!!!!!!!!!!!!!!!!!!!!!EMPTY";

	@SaveState
	private CharSequence mNeutralButtonText;

	@SaveState
	private T mValue;

	@SaveState
	private T mDefaultValue;

	@SaveState
	private boolean mAutoSummary = true;

	@SaveState
	private boolean mHasSummary = false;

	private Dialog mDialog;

	private static final String KEY_IS_DIALOG_SHOWING = TAG + ".is_showing";

	public MyDialogPreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		handleAttributes(attrs);
	}

	public MyDialogPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
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
	 * neccessary. When using the auto-summary functionality
	 * (enabled by default), the summary will be updated using
	 * {@link #toSummaryString(T)}. This function also calls
	 * the callback {@link #onValueSet(T)}.
	 */
	public final void setValue(T value)
	{
		if(LOGV) Log.v(TAG, getKey() + ": setValue: value=" + value);

		mValue = value;

		if(!mHasSummary && mAutoSummary)
			setSummaryInternal(value != null ? toSummaryString(value) : null);

		onValueSet(value);

		if(shouldPersist())
			persistString(toPersistedString(value));
	}

	/**
	 * Calls {@link #setValue(T)} iff the change listener returned <code>true</code>.
	 *
	 * @param value
	 * @see #setValue(Object)
	 */
	public final void changeValue(T value)
	{
		if(LOGV) Log.v(TAG, getKey() + ": changeValue: value=" + value);

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
			Log.e(TAG, "This shouldn't have happened...");

			String persisted = getPersistedString(EMPTY);
			if(persisted != null && persisted != EMPTY) // the != operator is intentional!
				mValue = fromPersistedString(persisted);
			else
			{
				//Log.w(TAG, "getValue: key=" + getKey() + " persisted string was null or not available");
				return mDefaultValue;
			}
		}

		if(LOGV) Log.v(TAG, getKey() + ": getValue: mValue=" + mValue);

		return mValue;
	}

	@Override
	public void setSummary(CharSequence summary)
	{
		super.setSummary(summary);
		mHasSummary = summary != null;
	}

	@Override
	public final Dialog getDialog() {
		return mDialog;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		onDialogClosed(which == Dialog.BUTTON_POSITIVE);
	}

	@Override
	public CharSequence getDialogTitle() {
		return getTitle();
	}

	protected abstract T fromPersistedString(String string);

	protected String toPersistedString(T value) {
		return value.toString();
	}

	protected String toSummaryString(T value) {
		return value.toString();
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index)
	{
		final String string = a.getString(index);
		final T newValue = fromPersistedString(string);

		if(LOGV) Log.v(TAG, getKey() + ": onGetDefaultValue: value=" + newValue);

		mDefaultValue = newValue;
		return newValue;
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
	{
		if(LOGV)
		{
			Log.v(TAG, getKey() + ": onSetInitialValue: restorePersistedValue=" + restorePersistedValue +
					", defaultValue=" + defaultValue);
		}

		if(restorePersistedValue)
		{
			final String persisted = getPersistedString(null);
			if(persisted != null)
			{
				setValue(fromPersistedString(persisted));
				return;
			}
		}

		if(defaultValue != null)
			setValue(fromPersistedString(defaultValue.toString()));
	}

	@Override
	protected void onBindDialogView(View view)
	{
		if(view != null)
			Util.detachFromParent(view);

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
	 * {@link #getDialogTitle()} will <em>not</em> be applied
	 * automatically when using a custom dialog.
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
	protected void onPrepareDialog(Dialog dialog) {
		// do nothing
	}

	/**
	 * Called when the dialog is dismissed.
	 * <p>
	 * Due to technical reasons, you cannot call {@link Dialog#setOnDismissListener(OnDismissListener)}
	 * in {@link #onPrepareDialog(Dialog)} or {@link #onGetCustomDialog()}. Use this function instead
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

		final int softInputMode = onGetSoftInputMode();
		if(softInputMode != 0)
		{
			final Window window = mDialog.getWindow();
			if(window != null)
				window.setSoftInputMode(softInputMode);
		}

		onPrepareDialog(mDialog);

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
		if(LOGV) Log.v(TAG, getKey() + ": onSaveInstanceState");

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
		if(LOGV) Log.v(TAG, getKey() + ": onRestoreInstanceState");

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
		if(LOGV) Log.v(TAG, getKey() + ": onDialogClosed: positiveResult=" + positiveResult);

		if(positiveResult)
			changeValue(getDialogValue());
	}

	protected void setSummaryInternal(CharSequence summary)
	{
		if(LOGV) Log.v(TAG, getKey() + ": setSummaryInternal: summary=" + summary);

		super.setSummary(summary);
		mAutoSummary = true;
	}

	private void handleAttributes(AttributeSet attrs)
	{
		if(attrs == null)
			return;

		if(false)
		{
			final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MyDialogPreference);
			mAutoSummary = a.getBoolean(R.styleable.MyDialogPreference_autoSummary, true);
			a.recycle();
		}
		else
		{
			mAutoSummary = attrs.getAttributeBooleanValue(R.styleable.MyDialogPreference_autoSummary, true);
		}

		if(LOGV) Log.v(TAG, getKey() + ": handleAttributes: mAutoSummary=" + mAutoSummary);
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
