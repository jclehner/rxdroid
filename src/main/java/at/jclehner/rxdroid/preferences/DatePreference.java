package at.jclehner.rxdroid.preferences;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.DatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import at.jclehner.androidutils.AdvancedDialogPreference;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.util.DateTime;

public class DatePreference extends AdvancedDialogPreference<Date>
{
	private static final String TAG = DatePreference.class.getSimpleName();

	private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private Date mDialogValue = null;

	public DatePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setAutoSummaryEnabled(true);
	}

	@Override
	protected Dialog onGetCustomDialog()
	{
		Date date = getValue();
		if(date == null)
			date = DateTime.today();

		final Calendar cal = DateTime.calendarFromDate(date);

		final int year = cal.get(Calendar.YEAR);
		final int month = cal.get(Calendar.MONTH);
		final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

		Log.d(TAG, "onGetCustomDialog: y-m-d=" + year + "-" + month + "-" + dayOfMonth);

		final DatePickerDialog dialog = new DatePickerDialog(getContext(), mListener,
				year, month, dayOfMonth);

		dialog.setTitle(getDialogTitle());
		dialog.setIcon(getDialogIcon());

		return dialog;
	}

	@Override
	protected Date fromPersistedString(String string)
	{
		if(string.length() != 0)
		{
			try
			{
				return mDateFormat.parse(string);
			}
			catch(ParseException e)
			{
				// return null below
			}
		}

		return null;
	}

	@Override
	protected String toPersistedString(Date value)
	{
		if(value == null)
			return "";

		return mDateFormat.format(value);
	}

	@Override
	protected String toSummaryString(Date value)
	{
		if(value == null)
			return getContext().getString(R.string._summary_not_available);

		return mDateFormat.format(value);
	}

	@Override
	protected Date getDialogValue() {
		return mDialogValue;
	}

	private final DatePickerDialog.OnDateSetListener mListener = new DatePickerDialog.OnDateSetListener()
	{
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
		{
			mDialogValue = new Date(year - 1900, monthOfYear, dayOfMonth);
			changeValue(mDialogValue);
		}
	};
}
