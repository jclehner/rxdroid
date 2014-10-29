package at.jclehner.androidutils;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import at.jclehner.rxdroid.Version;

public class DatePickerDialog extends android.app.DatePickerDialog
{
	private final OnDateSetListener mListener;
	private OnShowListener mOnShowListener;

	public DatePickerDialog(Context context, OnDateSetListener listener, int year,
							int monthOfYear, int dayOfMonth) {
		this(context, 0, listener, year, monthOfYear, dayOfMonth);
	}

	public DatePickerDialog(Context context, int theme, OnDateSetListener listener, int year,
							int monthOfYear, int dayOfMonth) {
		super(context, theme, Version.SDK_IS_JELLYBEAN_OR_NEWER ? null : listener, year, monthOfYear, dayOfMonth);
		super.setOnShowListener(mLocalOnShowListener);
		mListener = listener;
	}

	public static DatePickerDialog withDate(Context context, int theme, OnDateSetListener listener, Date date)
	{
		final Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(date);

		final int d = cal.get(Calendar.DAY_OF_MONTH);
		final int m  = cal.get(Calendar.MONTH);
		final int y = cal.get(Calendar.YEAR);

		return new DatePickerDialog(context, theme, listener, y, m, d);
	}

	@Override
	public void setOnShowListener(OnShowListener listener) {
		mOnShowListener = listener;
	}

	private final OnShowListener mLocalOnShowListener = new OnShowListener()
	{
		@Override
		public void onShow(DialogInterface dialogInterface)
		{
			if(mOnShowListener != null)
				mOnShowListener.onShow(dialogInterface);

			if(Version.SDK_IS_JELLYBEAN_OR_NEWER)
			{
				final DatePickerDialog dialog = (DatePickerDialog) dialogInterface;
				dialog.getButton(BUTTON_POSITIVE).setOnClickListener(
						new View.OnClickListener()
						{
							@TargetApi(11)
							@Override
							public void onClick(View v)
							{
								final DatePicker picker = dialog.getDatePicker();
								mListener.onDateSet(picker, picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
								dialog.dismiss();
							}
						}
				);
			}
		}
	};
}
