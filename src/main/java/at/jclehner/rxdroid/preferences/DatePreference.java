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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

import org.joda.time.LocalDate;

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
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
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
	public CharSequence getDialogTitle() {
		return Version.SDK_IS_LOLLIPOP_OR_NEWER ? null : super.getDialogTitle();
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

	@TargetApi(11)
	@Override
	protected View onCreateDialogView()
	{
		final LocalDate value = getValue();
		final DatePicker picker = new DatePicker(getThemedContext());

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
			picker.setMinDate(mToday.toDate().getTime());

		picker.init(value.getYear(), value.getMonthOfYear() - 1, value.getDayOfMonth(), this);

		return picker;
	}
}
