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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference implements OnTimeSetListener, OnClickListener
{
	private static final String TAG = TimePreference.class.getName();
		
	private TimePickerDialog mDialog;
			
	private String mAfterTimeKey;
	private String mBeforeTimeKey;
	
	private DumbTime mTime;
	private DumbTime mAfter;
	private DumbTime mBefore;
			
	private String mDefaultValue = "00:00";
		
	public TimePreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public TimePreference(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);		
				
		// FIXME
		for(int i = 0; i != attrs.getAttributeCount(); ++i)
		{
			final String name = attrs.getAttributeName(i);
			String value = attrs.getAttributeValue(i);
				
			if(name.equals("defaultValue"))
			{
				if(value.charAt(0) == '@')
				{
					final Resources res = context.getResources();										
					value = res.getString(Integer.parseInt(value.substring(1), 10));
				}			
				mDefaultValue = value;
			}
			else if(name.equals("isAfter"))
				mAfterTimeKey = value;
			else if(name.equals("isBefore"))
				mBeforeTimeKey = value;			
		}
		
		Log.d(TAG, "init: after=" + mAfterTimeKey + ", before=" + mBeforeTimeKey);
	}
	
	@Override
	public Dialog getDialog() 
	{
		return mDialog;
	}
	
	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{		
		final DumbTime time = new DumbTime(hourOfDay, minute, 0);
		
		Log.d(TAG, "onTimeSet: hourOfDay=" + hourOfDay + ", minute=" + minute);
		
		Log.d(TAG, "onTimeSet: mTime=" + mTime + "(" + mTime.getTime() + ")");
		Log.d(TAG, "onTimeSet: time=" + time + "(" + time.getTime() + ")");
		//Log.d(TAG, "UTC: " + Time.UTC(year, month, day, hour, minute, second))
		
		if((mAfter != null && time.compareTo(mAfter) == -1) || (mBefore != null && !time.before(mBefore)))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(R.string._Error);		
			builder.setMessage("Time is not within the required constraints.");
			
			builder.setNeutralButton(android.R.string.ok, this);
			builder.show();
		}
		else
		{
			final String timeString = time.toString(false);
			mTime = time;
			persistString(timeString);
			setSummary(timeString);
			Log.d(TAG, "onTimeSet: persisting");
			setupTimePicker();
		}
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == Dialog.BUTTON_NEUTRAL)
		{
			// dialog is the AlertDialog created above. clicking OK should bring back
			// the TimePickerDialog
			mDialog.show();
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
		
		setupTimePicker();
	}
	
	@Override
	protected void showDialog(Bundle state)
	{
		getDialog().show();
	}
		
	private void setupTimePicker()
	{
		final boolean is24HourFormat = DateFormat.is24HourFormat(getContext());
		mDialog = new TimePickerDialog(getContext(), this, mTime.getHours(), mTime.getMinutes(), is24HourFormat);
		
		String message = null;
		
		mAfter = Settings.INSTANCE.getTime(mAfterTimeKey);
		mBefore = Settings.INSTANCE.getTime(mBeforeTimeKey);		
				
		if(mAfter != null && mBefore != null)
		{
			message = "Choose a time after %1 and before %2.";
			message = message.replace("%1", mAfter.toString());
			message = message.replace("%2", mBefore.toString());
		}
		else if(mAfter != null)
		{
			message = "Choose a time after %1.";
			message = message.replace("%1", mAfter.toString());			
		}
		else if(mBefore != null)
		{
			message = "Choose a time before %1.";
			message = message.replace("%1", mBefore.toString());		
		}
				
		mDialog.setMessage(message);		
	}		
}
