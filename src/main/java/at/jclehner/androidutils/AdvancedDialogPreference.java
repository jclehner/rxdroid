/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

import android.support.v7.app.AlertDialog;
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
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;

import at.jclehner.androidutils.InstanceState.SaveState;
import at.jclehner.rxdroid.BuildConfig;
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
public abstract class AdvancedDialogPreference<T extends Serializable> extends DialogPreference
		implements CompoundButton.OnCheckedChangeListener
{
	private static final String TAG = AdvancedDialogPreference.class.getSimpleName();
	private static final boolean LOGV = BuildConfig.DEBUG;

	private static final boolean USE_NEW_DISMISS_LOGIC = false;

	@SuppressWarnings("unused")
	private static final String EXTRA_DIALOG_VALUE = "at.jclehner.androidutils.DIALOG_VALUE";
	private static final String EXTRA_DIALOG_STATE = "at.jclehner.androidutils.DIALOG_STATE";

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

	@SaveState
	private int mLastButtonId = -1;

	private Dialog mDialog;

	private LayoutInflater mThemedInflater;
	private Context mThemedContext;

	private CompoundButton mToggler;
	private boolean mCheckable = false;

	//private static final String KEY_IS_DIALOG_SHOWING = TAG + ".is_showing";
	//private static final String KEY_DIALOG_VALUE = TAG + ".dialog_value";

	public AdvancedDialogPreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		handleAttributes(attrs);
		if(LOGV) Log.d(TAG, "ctor: key=" + getKey());
	}

	public AdvancedDialogPreference(Context context, AttributeSet attrs) {
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

	public void setAutoSummaryEnabled(boolean enabled) {
		mAutoSummary = enabled;
	}

	public boolean isAutoSummaryEnabled() {
		return mAutoSummary;
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

		if(mToggler != null)
			mToggler.setChecked(value != null);

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
			if(shouldPersist())
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

	public void setCheckable(boolean checkable)
	{
		mCheckable = checkable;
		setWidgetLayoutResource(checkable ? R.layout.toggler : 0);
		if(LOGV) Log.v(TAG, getKey() + ": setCheckable: checkable=" + checkable);
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
	public void onClick(DialogInterface dialog, int which)
	{
		if(LOGV) Log.v(TAG, getKey() + ": onClick: which=" + which);

		if(USE_NEW_DISMISS_LOGIC)
			mLastButtonId = which;
		else
		{
			super.onClick(dialog, which);
			//onDialogClosed(which == Dialog.BUTTON_POSITIVE);
		}
	}

	@Override
	public CharSequence getDialogTitle()
	{
		final CharSequence title = super.getDialogTitle();
		return title != null ? title : getTitle();
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
	 * {@link #onPrepareDialogBuilder(android.support.v7.app.AlertDialog.Builder)}
	 * will never be called. Also note that an OnDismissListener
	 * set on the returned dialog will be overridden in {@link #showDialog(Bundle)}!
	 * <p>
	 * Using this function requires care, as the following functions will <em>never</em>
	 * be called:
	 * <ul>
	 * <li>{@link #onPrepareDialogBuilder(android.support.v7.app.AlertDialog.Builder)}</li>
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

	protected int getDialogThemeResId() {
		return 0;
	}

	/**
	 * Returns a <code>Context</code> with the theme set by {@link #setGlobalDialogTheme(int)}.
	 * <p>
	 *
	 * @return a <code>ContextThemeWrapper</code> if the theme set by {@link #setGlobalDialogTheme(int)} is
	 * not <code>0</code>. Otherwise, it returns {@link #getContext()}.
	 */
	protected final Context getThemedContext()
	{
		requireThemedContextAndInflater();
		return mThemedContext;
	}

	/**
	 * Called when the dialog is dismissed.
	 * <p>
	 * Due to technical reasons, you cannot call {@link Dialog#setOnDismissListener(OnDismissListener)}
	 * in {@link #onPrepareDialog(Dialog)} or {@link #onGetCustomDialog()}. Use this function instead
	 * do perform any additional cleanup.
	 */
	@Override
	public void onDismiss(DialogInterface dialog)
	{
		if(USE_NEW_DISMISS_LOGIC)
			onDialogClosed(mLastButtonId == Dialog.BUTTON_POSITIVE);
		else
			super.onDismiss(dialog);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		if(isChecked)
		{
			// Reset the checked status as this will be set by changeValue (if not null)
			mToggler.setChecked(false);
			showDialog(null);
		}
		else
			changeValue(null);
	}

	@Override
	protected final void showDialog(Bundle state)
	{
		mDialog = onGetCustomDialog();
		if(mDialog == null)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getThemedContext())
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

			mDialog = builder.create();
		}

		final int softInputMode = onGetSoftInputMode();
		if(softInputMode != 0)
		{
			final Window window = mDialog.getWindow();
			if(window != null)
				window.setSoftInputMode(softInputMode);
			else
				Log.d(TAG, "showDialog: window was null");
		}

		onPrepareDialog(mDialog);

		if(state != null)
			mDialog.onRestoreInstanceState(state);

//		final T dialogValue;
//		if(state != null)
//			dialogValue = fromPersistedString(state.getString(EXTRA_DIALOG_VALUE));
//		else
//			dialogValue = mValue;
//
//		onSetInitialDialogValue(mDialog, dialogValue);

		mDialog.setOnDismissListener(mDismissListener);
		mDialog.show();

		onShow(mDialog);
	}

	protected void onShow(Dialog dialog) {
		// do nothing
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		if(LOGV) Log.v(TAG, getKey() + ": onSaveInstanceState");

		final Bundle extras = new Bundle();

		if(mDialog != null)
		{
			extras.putBundle(EXTRA_DIALOG_STATE, mDialog.onSaveInstanceState());
			mDialog.dismiss();
			//extras.putString(EXTRA_DIALOG_VALUE, toPersistedString(getDialogValue()));
		}

		final Parcelable superState = super.onSaveInstanceState();
		return InstanceState.createFrom(this, superState, extras);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		if(LOGV) Log.v(TAG, getKey() + ": onRestoreInstanceState: value=" + mValue);

		super.onRestoreInstanceState(InstanceState.getSuperState(state));
		InstanceState.restoreTo(this, state);

		if(LOGV) Log.v(TAG, "  value=" + mValue);

		final Bundle extras = InstanceState.getExtras(state);
		if(extras != null)
		{
			final Bundle dialogState = extras.getBundle(EXTRA_DIALOG_STATE);
			if(dialogState != null)
				showDialog(dialogState);
		}

		//mDialog.onS
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if(LOGV) Log.v(TAG, getKey() + ": onDialogClosed: positiveResult=" + positiveResult);

		if(positiveResult)
			changeValue(getDialogValue());
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);

		if(mCheckable)
		{
			mToggler = (CompoundButton) view.findViewById(android.R.id.checkbox);

			if(mToggler != null)
			{
				mToggler.setChecked(mValue != null);
				mToggler.setOnCheckedChangeListener(this);
			}
		}
	}

	protected void setSummaryInternal(CharSequence summary)
	{
		if(LOGV) Log.v(TAG, getKey() + ": setSummaryInternal: summary=" + summary);

		super.setSummary(summary);
		mAutoSummary = true;
	}

	/**
	 * Returns a LayoutInflater using the theme set by {@link #setGlobalDialogTheme(int)}.
	 */
	protected final LayoutInflater getLayoutInflater()
	{
		requireThemedContextAndInflater();
		return mThemedInflater;
	}

	private void handleAttributes(AttributeSet attrs)
	{
		if(attrs == null)
			return;

		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AdvancedDialogPreference);

		setAutoSummaryEnabled(a.getBoolean(R.styleable.AdvancedDialogPreference_autoSummary, true));
		setNeutralButtonText(a.getString(R.styleable.AdvancedDialogPreference_neutralButtonText));

		setCheckable(a.getBoolean(R.styleable.AdvancedDialogPreference_checkable, false));

		a.recycle();

		if(LOGV) Log.v(TAG, getKey() + ": handleAttributes: mAutoSummary=" + mAutoSummary);
	}

	private void requireThemedContextAndInflater()
	{
		if(mThemedInflater == null || mThemedContext == null)
		{
			synchronized(this)
			{
				if(mThemedContext == null)
				{
					final int themeResId = getDialogThemeResId();
					if(themeResId != 0)
						mThemedContext = new ContextThemeWrapper(getContext(), themeResId);
					else
						mThemedContext = getContext();
				}

				if(mThemedInflater == null)
					mThemedInflater = LayoutInflater.from(mThemedContext);
			}
		}
	}

	private final OnDismissListener mDismissListener = new OnDismissListener() {

		@Override
		public void onDismiss(DialogInterface dialog)
		{
			AdvancedDialogPreference.this.onDismiss(mDialog);
			mDialog = null;
		}
	};
}
