/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2019 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid.ui;

import android.annotation.TargetApi;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.LocalDate;

import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Version;
import at.jclehner.rxdroid.util.DateTime;

public class DatePickerDialog extends AlertDialog implements
		DatePicker.OnDateChangedListener, DialogInterface.OnClickListener
{
	private static final String TAG = DatePickerDialog.class.getSimpleName();
	private static final boolean LOGV = BuildConfig.DEBUG;

	// In some locales, Samsung Lollipop ROMs crash with an obscure
	// java.util.IllegalFormatConversionException: %d can't format java.lang.String arguments
	// related to DatePickers on these ROMs[1,2]. Attempting to reproduce the crash in
	// Samsung's "Remote Testing Lab" failed. In the spirit of "better safe than sorry",
	// we fall back to the pre-Lollipop DatePicker without the Calendar.
	//
	// [1] https://stackoverflow.com/questions/28345413/datepicker-crash-in-samsung-with-android-5-0
	// [2] https://stackoverflow.com/questions/28618405/datepicker-crashes-on-my-device-when-clicked-with-personal-app
	private static final boolean NEED_SAMSUNG_DATE_PICKER_HACK =
			Version.SDK_IS_LOLLIPOP_OR_NEWER
			&& Build.MANUFACTURER.equalsIgnoreCase("Samsung")
			&& Build.FINGERPRINT.contains("5.0");

	// DatePicker is broken on Lollipop 5.0, as the OnDateChangeListener is never
	// called. The second problem is that while you can set min/max dates, this
	// only visually 'disables' the dates not within this range, but you can still
	// select them.
	// This means that we have to check the DatePicker's date when clicking the
	// 'OK' button, and not close the dialog if it's not within range
	// (see mBtnListener).
	private static final boolean NEED_5_0_DATE_PICKER_HACK =
			!NEED_SAMSUNG_DATE_PICKER_HACK &&
			Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP;

	private final DatePicker mPicker;
	private LocalDate mDate;
	private OnDateSetListener mListener;

	private LocalDate mMinDate;
	private LocalDate mMaxDate;

	private boolean mShowConstraintMessage = false;

	public interface OnDateSetListener
	{
		void onDateSet(DatePickerDialog dialog, LocalDate date);
	}

	@TargetApi(11)
	public DatePickerDialog(Context context, LocalDate date, OnDateSetListener listener)
	{
		super(context);

		mDate = date;
		mListener = listener;

		if(!NEED_SAMSUNG_DATE_PICKER_HACK)
		{
			mPicker = new DatePicker(context);
			if(!context.getResources().getBoolean(R.bool.is_tablet))
				mPicker.setCalendarViewShown(false);
		}
		else
		{
			mPicker = (DatePicker) LayoutInflater.from(context)
					.inflate(R.layout.date_picker_spinner_mode, null);
		}

		if(date != null)
			setPickerDate(date);

		setView(mPicker);
		setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);
		setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok),
				NEED_5_0_DATE_PICKER_HACK ? null : this);
	}

	public LocalDate getDate() {
		return mDate;
	}

	public void setShowConstraintMessage(boolean showMessage)
	{
		mShowConstraintMessage = showMessage;
		updateMessage();
	}

	@TargetApi(11)
	public void setMinDate(LocalDate date)
	{
		mMinDate = date;

		if(date != null && mMaxDate != null && mMaxDate.isBefore(date))
			throw new IllegalArgumentException("Requested min date " + date + " is before max date " + mMaxDate);

		if(mMinDate != null)
			mPicker.setMinDate(mMinDate.toDate().getTime());

		updateMessage();
	}

	@TargetApi(11)
	public void setMaxDate(LocalDate date)
	{
		mMaxDate = date;

		if(date != null && mMinDate != null && mMinDate.isAfter(date))
			throw new IllegalArgumentException("Requested max date " + date + " is after min date " + mMinDate);

		if(mMaxDate != null)
			mPicker.setMaxDate(mMaxDate.toDate().getTime());

		updateMessage();
	}

	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{
		mDate = getPickerDate();

		final Button b = getButton(BUTTON_POSITIVE);
		if(b != null)
			b.setEnabled(isDateWithinValidRange());
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == BUTTON_POSITIVE)
		{
			mDate = getPickerDate();
			if(mListener != null)
				mListener.onDateSet(this, mDate);
		}
	}

	@Override
	public void show()
	{
		super.show();

		final Button btn = getButton(BUTTON_POSITIVE);
		if(btn != null)
		{
			btn.setEnabled(isDateWithinValidRange());
			if(NEED_5_0_DATE_PICKER_HACK)
				btn.setOnClickListener(mBtnListener);
		}

		updateMessage();

		final View v = findViewById(android.R.id.message);
		if(v != null && v instanceof TextView)
			((TextView) v).setGravity(Gravity.CENTER);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null)
		{
			final String dateStr = savedInstanceState.getString("date");
			if(dateStr != null)
				mDate = LocalDate.parse(dateStr);
		}
		else if(mDate == null)
			mDate = mMinDate == null ? LocalDate.now() : mMinDate;

		setPickerDate(mDate);
	}

	@Override
	public Bundle onSaveInstanceState()
	{
		final Bundle state = super.onSaveInstanceState();
		state.putString("date", mDate != null ? mDate.toString() : null);
		return state;
	}

	private LocalDate getPickerDate() {
		return new LocalDate(mPicker.getYear(), mPicker.getMonth() + 1, mPicker.getDayOfMonth());
	}

	private void setPickerDate(LocalDate date) {
		mPicker.init(date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth(), this);
	}

	private void updateMessage()
	{
		if(mShowConstraintMessage)
		{
			final String msg = getConstraintMessage();
			setMessage(msg != null ? Html.fromHtml("<small>" + msg + "</small>") : null);
		}
	}

	private String getConstraintMessage()
	{
		final String begin = mMinDate != null ? DateTime.toNativeDate(mMinDate.toDate()) : null;
		String end = mMaxDate != null ? DateTime.toNativeDate(mMaxDate.toDate()) : null;

		final int resId;

		if(mMinDate != null && mMaxDate != null)
			resId = R.string._msg_constraints_date_between;
		else if(mMinDate != null)
			resId = R.string._msg_constraints_date_from;
		else if(mMaxDate != null)
		{
			// To avoid the use of the ambiguous 'until', we
			// use 'before' instead. Yes, I've been to
			// english.stackexchange.com to research this.
			resId = R.string._msg_constraints_date_before;
			end = DateTime.toNativeDate(mMaxDate.plusDays(1).toDate());
		}
		else
			resId = 0;

		return resId != 0 ? getContext().getString(resId, begin, end) : null;
	}

	private final View.OnClickListener mBtnListener = new View.OnClickListener() {
		@Override
		public void onClick(View v)
		{
			mDate = getPickerDate();

			// If the date is outside the valid range, set it to the closest
			// valid date and DON't close the dialog. Also, display a toast.

			if(mMinDate != null && mDate.isBefore(mMinDate))
				setPickerDate(mMinDate);
			else if(mMaxDate != null && mDate.isAfter(mMaxDate))
				setPickerDate(mMaxDate);
			else
			{
				DatePickerDialog.this.onClick(DatePickerDialog.this, BUTTON_POSITIVE);
				dismiss();
				return;
			}

			Toast.makeText(getContext(), getConstraintMessage(), Toast.LENGTH_SHORT).show();
		}
	};

	private boolean isDateWithinValidRange()
	{
		if(mMinDate != null && mDate.isBefore(mMinDate))
			return false;
		if(mMaxDate != null && mDate.isAfter(mMaxDate))
			return false;

		return true;
	}
}
