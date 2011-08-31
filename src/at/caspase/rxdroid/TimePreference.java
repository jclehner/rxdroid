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
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference implements OnTimeSetListener
{
	private static final String TAG = TimePreference.class.getName();
	private static final String DEFAULT_TIME = "00:00";

	private static final int IDX_AFTER = 0;
	private static final int IDX_BEFORE = 1;

	private MyTimePickerDialog mDialog;

	private DumbTime[] mConstraintTimes = new DumbTime[2];
	private String[] mConstraintTimePrefKeys = new String[2];

	private DumbTime mTime;
	private String mDefaultValue;

	private SharedPreferences mPrefs;;

	public TimePreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TimePreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimePreference, defStyle, 0);

		final int[] attrIds = { R.styleable.TimePreference_after, R.styleable.TimePreference_before };

		for(int i = 0; i != 2; ++i)
		{
			final String value = a.getString(attrIds[i]);

			if(value != null)
			{
				try
				{
					mConstraintTimes[i] = DumbTime.valueOf(value);
				}
				catch(IllegalArgumentException e)
				{
					mConstraintTimePrefKeys[i] = value;
				}
			}
		}

		mDefaultValue = a.getString(R.styleable.TimePreference_defaultValue);
		if(mDefaultValue == null)
			mDefaultValue = DEFAULT_TIME;

		mPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		a.recycle();
	}

	@Override
	public Dialog getDialog() {
		return mDialog;
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{
		// FIXME
		if(!mDialog.checkConstraints(hourOfDay, minute))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setTitle(R.string._title_error);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(R.string._msg_timepreference_constraint_failed);
			builder.setNeutralButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if(which == Dialog.BUTTON_NEUTRAL)
						showDialog(null);
				}
			});
			builder.show();
		}
		else
		{
			final DumbTime time = new DumbTime(hourOfDay, minute, 0);

			final String timeString = time.toString();
			mTime = time;
			//persistString(timeString);
			setSummary(timeString);

			Editor editor = mPrefs.edit();
			editor.putString(getKey(), timeString);
			editor.commit();
		}
	}

	@Override
	protected void onAttachedToActivity()
	{
		super.onAttachedToActivity();

		// getPersistedString returns null in the constructor, so we have to set the summary here
		final String persisted = getPersistedString(mDefaultValue);
		setSummary(persisted);
		mTime = DumbTime.valueOf(persisted);
	}

	@Override
	protected void showDialog(Bundle state)
	{
		mDialog = new MyTimePickerDialog(getContext(), this, mTime.getHours(), mTime.getMinutes(), DateFormat.is24HourFormat(getContext()));

		for(int i = 0; i != 2; ++i)
		{
			DumbTime time = mConstraintTimes[i];

			if(time == null)
				time = Settings.INSTANCE.getTimePreference(mConstraintTimePrefKeys[i]);

			if(i == IDX_AFTER)
				mDialog.setConstraintAfter(time);
			else if(i == IDX_BEFORE)
				mDialog.setConstraintBefore(time);
			else
				throw new RuntimeException();
		}
		mDialog.show();
	}

	@Override
	protected View onCreateDialogView() {
		return null;
	}

	private static class MyTimePickerDialog extends TimePickerDialog
	{
		private DumbTime mAfter = null;
		private DumbTime mBefore = null;

		public MyTimePickerDialog(Context context, int theme, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
			super(context, theme, callBack, hourOfDay, minute, is24HourView);
		}

		public MyTimePickerDialog(Context context, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
			super(context, callBack, hourOfDay, minute, is24HourView);
		}

		public void setConstraintAfter(DumbTime after)
		{
			mAfter = after;
			updateMessage();
		}

		public void setConstraintBefore(DumbTime before)
		{
			mBefore = before;
			updateMessage();
		}

		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
		{
			Log.d(TAG, "onTimeChanged");

			if(checkConstraints(hourOfDay, minute))
				super.onTimeChanged(view, hourOfDay, minute);
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			Log.d(TAG, "onClick");
			super.onClick(dialog, which);
		}

		public boolean checkConstraints(int hourOfDay, int minute)
		{
			final DumbTime time = new DumbTime(hourOfDay, minute);

			boolean isValid;

			if((mAfter != null && !time.after(mAfter)) || (mBefore != null && !time.before(mBefore)))
				isValid = false;
			else
				isValid = true;

			getButton(BUTTON_POSITIVE).setEnabled(isValid);

			return isValid;
		}

		private void updateMessage()
		{
			int msgId = -1;

			if(mAfter != null && mBefore != null)
				msgId = R.string._msg_constraints_ab;
			else if(mAfter != null)
				msgId = R.string._msg_constraints_a;
			else if(mBefore != null)
				msgId = R.string._msg_constraints_b;

			if(msgId != -1)
				setMessage(getContext().getString(msgId, mAfter, mBefore));
			else
				setMessage(null);
		}
	}
}
