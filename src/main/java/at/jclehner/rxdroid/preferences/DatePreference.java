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

package at.jclehner.rxdroid.preferences;

import android.annotation.TargetApi;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import org.joda.time.LocalDate;

import java.util.IllegalFormatException;

import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Version;
import at.jclehner.rxdroid.ui.DatePickerDialog;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

public class DatePreference extends BaseAdvancedDialogPreference<LocalDate>
		implements DatePickerDialog.OnDateSetListener
{
	private LocalDate mMinDate;
	private LocalDate mMaxDate;

	private DatePickerDialog mDialog;

	public DatePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		handleAttributes(context, attrs);
	}

	@Override
	public CharSequence getDialogTitle() {
		return Version.SDK_IS_LOLLIPOP_OR_NEWER ? null : super.getDialogTitle();
	}

	public void setMinDate(LocalDate minDate) {
		mMinDate = minDate;
	}

	public void setMaxDate(LocalDate maxDate) {
		mMaxDate = maxDate;
	}

	@Override
	public void onDateSet(DatePickerDialog dialog, LocalDate date) {
		changeValue(date);
	}

	@Override
	protected LocalDate getDialogValue() {
		return mDialog != null ? mDialog.getDate() : null;
	}

	@Override
	protected LocalDate fromPersistedString(String string) {
		return xmlValueToDate(string);
	}

	@Override
	protected String toPersistedString(LocalDate value) {
		return value != null ? value.toString() : null;
	}

	@Override
	protected String toSummaryString(LocalDate value) {
		return DateTime.toNativeDate(value.toDate());
	}

	@Override
	protected Dialog onGetCustomDialog()
	{
		if(mDialog == null)
			mDialog = new DatePickerDialog(getThemedContext(), getValue(), this);

		mDialog.setMinDate(mMinDate);
		mDialog.setMaxDate(mMaxDate);

		return mDialog;
	}

	private void handleAttributes(Context context, AttributeSet attrs)
	{
		if(attrs == null)
			return;

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DatePreference);
		mMinDate = xmlValueToDate(a.getString(R.styleable.DatePreference_minDate));
		mMaxDate = xmlValueToDate(a.getString(R.styleable.DatePreference_maxDate));
	}

	private LocalDate xmlValueToDate(String value)
	{
		if(value == null)
			return null;

		try
		{
			return LocalDate.parse(value);
		}
		catch(RuntimeException e)
		{
			// ignore
		}

		final LocalDate today = LocalDate.now();

		if("today".equals(value))
			return today;
		else if("tomorrow".equals(value))
			return today.plusDays(1);
		else if("yesterday".equals(value))
			return today.minusDays(1);

		throw new IllegalArgumentException("Failed to interpret value as date: " + value);
	}
}
