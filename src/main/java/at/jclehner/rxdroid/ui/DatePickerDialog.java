package at.jclehner.rxdroid.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;

import org.joda.time.LocalDate;

import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Version;

public class DatePickerDialog extends AlertDialog implements
		DatePicker.OnDateChangedListener, DialogInterface.OnClickListener
{
	// In some locales, Samsung Lollipop ROMs crash with an obscure
	// java.util.IllegalFormatConversionException: %d can't format java.lang.String arguments
	// related to DatePickers on these ROMs[1,2]. Attempting to reproduce the crash in
	// Samsung's "Remote Testing Lab" failed. In the spirit of "better safe than sorry",
	// we fall back to the pre-Lollipop DatePicker without the Calendar.
	//
	// [1] https://stackoverflow.com/questions/28345413/datepicker-crash-in-samsung-with-android-5-0
	// [2] https://stackoverflow.com/questions/28618405/datepicker-crashes-on-my-device-when-clicked-with-personal-app
	private static final boolean NEED_SAMSUNG_DATE_PICKER_HACK = BuildConfig.DEBUG ||
			Version.SDK_IS_LOLLIPOP_OR_NEWER
			&& Build.MANUFACTURER.equalsIgnoreCase("Samsung")
			&& Build.FINGERPRINT.contains("5.0/");

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
			if(Version.SDK_IS_HONEYCOMB_OR_NEWER && !context.getResources().getBoolean(R.bool.is_tablet))
				mPicker.setCalendarViewShown(false);
		}
		else
		{
			mPicker = (DatePicker) LayoutInflater.from(context)
					.inflate(R.layout.date_picker_spinner_mode, null);
		}

		setPickerDate(date);

		setView(mPicker);
		setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);
		setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok),
				NEED_5_0_DATE_PICKER_HACK ? null : this);
	}

	public LocalDate getDate() {
		return mDate;
	}

	@TargetApi(11)
	public void setMinDate(LocalDate date)
	{
		mMinDate = date;

		if(mMaxDate != null && mMaxDate.isBefore(date))
			throw new IllegalArgumentException("Requested min date " + date + " is before max date " + mMaxDate);

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
			mPicker.setMinDate(mMinDate.toDate().getTime());
	}

	@TargetApi(11)
	public void setMaxDate(LocalDate date)
	{
		mMaxDate = date;

		if(mMinDate != null && mMinDate.isAfter(date))
			throw new IllegalArgumentException("Requested max date " + date + " is after min date " + mMinDate);

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
			mPicker.setMaxDate(mMaxDate.toDate().getTime());
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
		if(mDate == null)
			mDate = LocalDate.now();

		super.show();

		if(NEED_5_0_DATE_PICKER_HACK)
			getButton(BUTTON_POSITIVE).setOnClickListener(mBtnListener);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null)
		{
			final String date = savedInstanceState.getString("date");
			if(date != null)
				setPickerDate(LocalDate.parse(date));
		}
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

	private final View.OnClickListener mBtnListener = new View.OnClickListener() {
		@Override
		public void onClick(View v)
		{
			mDate = getPickerDate();

			// If the date is outside the valid range, set it to the closest
			// valid date and DON't close the dialog.

			if(mMinDate != null && mDate.isBefore(mMinDate))
				setPickerDate(mMinDate);
			else if(mMaxDate != null && mDate.isBefore(mMaxDate))
				setPickerDate(mMaxDate);
			else
			{
				DatePickerDialog.this.onClick(DatePickerDialog.this, BUTTON_POSITIVE);
				dismiss();
			}
		}
	};





	public boolean isDateWithinValidRange()
	{
		if(mMinDate != null && mDate.isBefore(mMinDate))
			return false;

		if(mMaxDate != null && mDate.isAfter(mMaxDate))
			return false;

		return true;
	}




}
