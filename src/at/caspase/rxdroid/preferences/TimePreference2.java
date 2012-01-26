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

package at.caspase.rxdroid.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.Toast;
import at.caspase.androidutils.MyDialogPreference;
import at.caspase.androidutils.StateSaver.SaveState;
import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.util.Util;

public class TimePreference2 extends MyDialogPreference
{
	private static final String TAG = TimePreference2.class.getName();
	private static final boolean LOGV = false;

	private static final int WRAP_AFTER = 1;
	private static final int WRAP_BEFORE = 1 << 1;

	private static final String NS_BASE = "http://schemas.android.com/apk/res/";
	private static final String NS_ANDROID = NS_BASE + "android";
	private static final String NS_PREF = NS_BASE + "at.caspase.rxdroid";

	private static final int IDX_AFTER = 0;
	private static final int IDX_BEFORE = 1;

	private DumbTime[] mConstraintTimes = new DumbTime[2];
	private String[] mConstraintKeys = new String[2];

	private int mWrapFlags;

	private String mDefaultValue;
	private DumbTime mTime;

	@SaveState
	private DumbTime mDialogTime;

	private Button mSetButton;

	public TimePreference2(Context context, AttributeSet attrs)
	{
		// we don't want the DialgPreference layout in pre honeycomb here
		super(context, attrs, android.R.attr.preferenceStyle);

		mDefaultValue = Util.getStringAttribute(context, attrs, NS_ANDROID, "defaultValue", "00:00");

		final String[] attributeNames = { "after", "before" };

		for(int i = 0; i != attributeNames.length; ++i)
		{
			String value = attrs.getAttributeValue(NS_PREF, attributeNames[i]);
			try
			{
				mConstraintTimes[i] = DumbTime.valueOf(value);
			}
			catch(IllegalArgumentException e)
			{
				mConstraintKeys[i] = value;
			}
		}

		mWrapFlags = 0;

		if(attrs.getAttributeBooleanValue(NS_PREF, "allowAfterWrap", false))
			mWrapFlags |= WRAP_AFTER;

		if(attrs.getAttributeBooleanValue(NS_PREF, "allowBeforeWrap", false))
			mWrapFlags |= WRAP_BEFORE;

		setPositiveButtonText(R.string._btn_set);
		setNegativeButtonText(android.R.string.cancel);

		if(getDialogTitle() == null)
			setDialogTitle(getTitle());
		if(getDialogIcon() == null)
			setDialogIcon(android.R.drawable.ic_menu_recent_history);
	}

	@Override
	public CharSequence getSummary() {
		return mTime != null ? mTime.toString() : mDefaultValue;
	}

	@Override
	public void setDialogMessage(CharSequence message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CharSequence getDialogMessage()
	{
		final DumbTime after = getConstraint(IDX_AFTER);
		final DumbTime before = getConstraint(IDX_BEFORE);

		final int msgId;

		if(after != null && before != null)
			msgId = R.string._msg_constraints_ab;
		else if(after != null)
			msgId = R.string._msg_constraints_a;
		else if(before != null)
			msgId = R.string._msg_constraints_b;
		else
			return null;

		return getContext().getString(msgId, after, before);
	}

	@Override
	protected void onAttachedToActivity()
	{
		String persisted = getPersistedString(null);
		if(persisted == null)
		{
			if(shouldPersist())
				persistString(mDefaultValue);
			mTime = DumbTime.valueOf(mDefaultValue);
			Log.d(TAG, "onAttachedToActivity: setting defaults for key=" + getKey());
		}
		else
			mTime = DumbTime.valueOf(persisted);

		if(mDialogTime == null)
			mDialogTime = mTime;
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
	{
		final Context context = getContext();
		final TimePicker timePicker = new TimePicker(context);
		timePicker.setIs24HourView(DateFormat.is24HourFormat(context));
		timePicker.setCurrentHour(mDialogTime.getHours());
		timePicker.setCurrentMinute(mDialogTime.getMinutes());
		timePicker.setOnTimeChangedListener(mListener);

		builder.setView(timePicker);
		builder.setMessage(getDialogMessage());
	}

	@Override
	protected void onShowDialog(Dialog dialog)
	{
		dialog.setOnShowListener(new OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog)
			{
				mSetButton = ((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
				if(!isTimeWithinConstraints(mDialogTime))
					mSetButton.setEnabled(false);
			}
		});
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if(positiveResult)
		{
			if(isTimeWithinConstraints(mDialogTime))
			{
				mTime = mDialogTime;

				if(callChangeListener(mTime))
					persistString(mTime.toString());

				notifyChanged();
			}
			else
			{
				// This should never happen, but we're playing it safe here...
				Toast.makeText(getContext(), R.string._msg_timepreference_constraint_failed,
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/*@Override
	protected Parcelable onSaveInstanceState()
	{
		Parcelable superState = super.onSaveInstanceState();
		return StateSaver.createInstanceState(this, superState, null);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		super.onRestoreInstanceState(StateSaver.getSuperState(state));
		StateSaver.restoreInstanceState(this, state);


	}*/

	private boolean isTimeWithinConstraints(DumbTime time)
	{
		final DumbTime after = getConstraint(IDX_AFTER);
		final DumbTime before = getConstraint(IDX_BEFORE);

		Log.d(TAG, "isTimeWithinConstraints: key=" + getKey());
		Log.d(TAG, "  after=" + after);
		Log.d(TAG, "  before=" + before);

		if(after != null && before != null)
		{
			if(mWrapFlags != 0 && before.before(after))
				return time.after(after) || time.before(before);

			return time.after(after) && time.before(before);
		}
		else if(after != null)
			return time.after(after);
		else if(before != null)
			return time.before(before);

		return true;
	}

	private DumbTime getConstraint(int index)
	{
		if(mConstraintTimes[index] != null)
			return mConstraintTimes[index];

		final String key = mConstraintKeys[index];
		if(key != null)
		{
			final TimePreference2 constraintPref = (TimePreference2) findPreferenceInHierarchy(key);
			if(constraintPref == null)
				throw new IllegalArgumentException("No such TimePreference: " + key);

			return constraintPref.mTime;
		}

		return null;
	}

	private final OnTimeChangedListener mListener = new OnTimeChangedListener() {

		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
		{
			mDialogTime = new DumbTime(hourOfDay, minute);
			Log.d(TAG, "onTimeChanged: " + mDialogTime);
			if(mSetButton != null)
				mSetButton.setEnabled(isTimeWithinConstraints(mDialogTime));
		}
	};
}
