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

package at.caspase.rxdroid.preferences;

import java.util.Date;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TimePicker;
import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Util;

public class TimePreference extends Preference implements OnTimeSetListener, OnPreferenceClickListener
{
	@SuppressWarnings("unused")
	private static final String TAG = TimePreference.class.getName();
	
	private String mDefaultValue;
	private DumbTime mTime;
	
	private DumbTime[] mConstraintTimes = new DumbTime[2];
	private String[] mConstraintKeys = new String[2];
		
	private boolean mWrapsAroundMidnight;
	
	public TimePreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TimePreference(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
				
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
		
		super.setOnPreferenceClickListener(this);		
		updateTime();
	}
	
	@Override
	public CharSequence getSummary() {
		return mTime == null ? null : mTime.toString();
	}
	
	@Override
	public void setOnPreferenceClickListener(OnPreferenceClickListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		showTimePicker();
		return true;
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{
		mTime = new DumbTime(hourOfDay, minute);
		
		if(!isTimeWithinConstraints())
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(R.string._title_error);
			builder.setMessage(R.string._msg_timepreference_constraint_failed);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					showTimePicker();					
				}
			});
			builder.show();
		}
		else
		{
			if(shouldPersist())
				persistString(mTime.toString());
			
			notifyChanged();
		}
	}
	
	private void showTimePicker()
	{
		updateTime();
		
		final boolean is24HourView = DateFormat.is24HourFormat(getContext());
		TimePickerDialog dialog = new TimePickerDialog(getContext(), this, mTime.getHours(), mTime.getMinutes(), is24HourView);
		
		dialog.setMessage(generateDialogMessage());
		dialog.show();
	}
	
	private String generateDialogMessage()
	{
		int msgId = -1;

		DumbTime after = getConstraint(IDX_AFTER);
		DumbTime before = getConstraint(IDX_BEFORE);
		
		if(after != null && before != null)
		{
			if(after.after(before)) // see comment in isTimeWithinConstraints for an explanation
				msgId = R.string._msg_constraints_b;
			else			
				msgId = R.string._msg_constraints_ab;
		}			
		else if(after != null)
			msgId = R.string._msg_constraints_a;
		else if(before != null)
			msgId = R.string._msg_constraints_b;

		if(msgId == -1)
			return null;
		
		return getContext().getString(msgId, after, before);		
	}
	
	private DumbTime getConstraint(int index)
	{
		if(mConstraintTimes[index] != null)
			return mConstraintTimes[index];
		
		final String key = mConstraintKeys[index];
		if(key != null)
		{
			final TimePreference constraintPref = (TimePreference) findPreferenceInHierarchy(key);
			if(constraintPref == null)
				throw new IllegalStateException("No such TimePreference: " + key);
			
			return constraintPref.mTime;
		}
		
		return null;
	}
	
	private void updateTime() {
		mTime = DumbTime.valueOf(getPersistedString(mDefaultValue));
	}
	
	private boolean isTimeWithinConstraints()
	{
		DumbTime after = getConstraint(IDX_AFTER);
		DumbTime before = getConstraint(IDX_BEFORE);
		
		if(after != null && before != null)
		{
			if(before.before(after))
			{
				// if the time constraint specified by 'before' is also before the 
				// time specified by the 'after' constraint, the dates wrap around
				// midnight!
				Log.d(TAG, getKey() + ": constraint 'before' wraps around midnight");				
				return mTime.after(after) || mTime.before(before);				
			}
			else if(after.after(before))
			{
				// the time constraint specified by 'after' is before our 'before'
				// constraint. we thus ignore that value and only check against the
				// 'before' constraint
				Log.d(TAG, getKey() + ": constraint 'after' wraps around midnight");				
				return mTime.before(before);
			}			
						
			return mTime.after(after) && mTime.before(before);
		}
		else if(after != null)
			return mTime.after(after);
		else if(before != null)
			return mTime.before(before);
		
		return true;
	}
	
	private static final String NS_BASE = "http://schemas.android.com/apk/res/";
	private static final String NS_ANDROID = NS_BASE + "android";
	private static final String NS_PREF = NS_BASE + "at.caspase.rxdroid";
		
	private static final int IDX_AFTER = 0;
	private static final int IDX_BEFORE = 1;
}
