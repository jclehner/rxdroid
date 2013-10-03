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

package at.jclehner.rxdroid;

import java.util.Calendar;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import at.jclehner.rxdroid.util.DateTime;

public class DatePickerFragment extends DialogFragment implements OnShowListener
{
	public static final String ARG_DATE = "date";

	private OnDateSetListener mListener;

	public DatePickerFragment() {}

	public static DatePickerFragment newInstance(Date date, OnDateSetListener listener)
	{
		final DatePickerFragment r = new DatePickerFragment(listener);
		final Bundle args = new Bundle();
		args.putSerializable(ARG_DATE, date);
		r.setArguments(args);
		return r;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final Calendar cal;

		final Bundle args = getArguments();
		if(args == null || !args.containsKey(ARG_DATE))
			cal = DateTime.nowCalendar();
		else
			cal = DateTime.calendarFromDate((Date) args.getSerializable(ARG_DATE));

		final int d = cal.get(Calendar.DAY_OF_MONTH);
		final int m  = cal.get(Calendar.MONTH);
		final int y = cal.get(Calendar.YEAR);

		if(Version.SDK_IS_JELLYBEAN_OR_NEWER)
		{
			// Workaround for Android issue #34833
			final Dialog dialog = new DatePickerDialog(getActivity(), null, y, m, d);
			dialog.setOnShowListener(this);
			return dialog;
		}
		else
			return new DatePickerDialog(getActivity(), mListener, y, m, d);
	}

	@Override
	public void onShow(DialogInterface dialogInterface)
	{
		if(Version.SDK_IS_JELLYBEAN_OR_NEWER)
		{
			final DatePickerDialog dialog = (DatePickerDialog) dialogInterface;
			dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {

				@TargetApi(11)
				@Override
				public void onClick(View v)
				{
					final DatePicker picker = dialog.getDatePicker();
					mListener.onDateSet(picker, picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
					dialog.dismiss();
				}
			});
		}
	}

	protected DatePickerFragment(OnDateSetListener listener) {
		mListener = listener;
	}
}
