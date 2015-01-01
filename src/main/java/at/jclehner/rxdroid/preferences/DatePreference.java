package at.jclehner.rxdroid.preferences;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

import org.joda.time.LocalDate;

import java.util.Date;

import at.jclehner.androidutils.AdvancedDialogPreference;
import at.jclehner.androidutils.DatePickerDialog;
import at.jclehner.rxdroid.Version;

public class DatePreference extends BaseAdvancedDialogPreference<LocalDate>
		implements DatePicker.OnDateChangedListener
{
	private final LocalDate mToday = new LocalDate();
	private boolean mIsValidDate = true;
	private LocalDate mDate;

	public DatePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setDefaultValue(new LocalDate());
	}

	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{
		final LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
		mIsValidDate = !date.isBefore(mToday);

		if(mIsValidDate)
			mDate = date;

		((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(mIsValidDate);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(mIsValidDate ? positiveResult : false);
	}

	@Override
	protected LocalDate getDialogValue() {
		return mDate;
	}

	@Override
	protected LocalDate fromPersistedString(String string) {
		return LocalDate.parse(string);
	}

	@Override
	protected String toPersistedString(LocalDate value) {
		return value.toString();
	}

	on

	@TargetApi(11)
	@Override
	protected View onCreateDialogView()
	{
		final LocalDate value = getValue();
		final DatePicker picker = new DatePicker(getThemedContext());

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
		{
			picker.setMinDate(mToday.toDate().getTime());
			picker.setSpinnersShown(true);
			picker.setCalendarViewShown(false);
		}

		picker.init(value.getYear(), value.getMonthOfYear() - 1, value.getDayOfMonth(), this);

		return picker;
	}
}
