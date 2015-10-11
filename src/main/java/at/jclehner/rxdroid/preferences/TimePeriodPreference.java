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

import java.io.Serializable;
import java.util.HashMap;

import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.res.TypedArray;
import android.os.Message;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import at.jclehner.androidutils.AdvancedDialogPreference;
import at.jclehner.rxdroid.DumbTime;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.preferences.TimePeriodPreference.TimePeriod;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

public class TimePeriodPreference extends BaseAdvancedDialogPreference<TimePeriod>
{
	private static final String TAG = TimePeriodPreference.class.getSimpleName();

	private static final boolean USE_CACHE = true;

	private static final int END = 1;
	private static final int MAX = END;
	private static final int BEGIN = 0;
	private static final int MIN = BEGIN;

	public static class TimePeriod implements Serializable
	{
		private static final long serialVersionUID = -2432714902425872383L;
		private static final HashMap<String, TimePeriod> CACHE = new HashMap<String, TimePeriod>();

		private final DumbTime mBegin;
		private final DumbTime mEnd;

		public TimePeriod(DumbTime begin, DumbTime end)
		{
			this.mBegin = begin;
			this.mEnd = end;
		}

		@Override
		public String toString() {
			return mBegin.toString(true, false) + "-" + mEnd.toString(true, false);
		}

		public static TimePeriod fromString(String string)
		{
			synchronized(CACHE)
			{
				TimePeriod value = USE_CACHE ? CACHE.get(string) : null;
				if(value == null)
				{
					final String[] tokens = string.split("-");
					if(tokens.length != 2)
						throw new IllegalArgumentException();

					final DumbTime begin, end;

					begin = DumbTime.fromString(tokens[0]);
					end = DumbTime.fromString(tokens[1]);

					CACHE.put(string, value = new TimePeriod(begin, end));
				}

				return value;
			}
		}

		public DumbTime begin() {
			return mBegin;
		}

		public DumbTime end() {
			return mEnd;
		}

		public boolean contains(DumbTime time) {
			return time.isWithinRange(mBegin, mEnd, true);
		}
	}

	private DumbTime mBegin;
	private DumbTime mEnd;

	private String[] mConstraintKeys = new String[2];
	private DumbTime[] mConstraintTimes = new DumbTime[2];
	private boolean mAllowEndWrap = false;

	private View mContainer;
	private TimePicker mTimePicker;
	private TextView mMessageView;

	private Button mBackButton;
	private Button mNextButton;

	private int mCurrentPage = 0;
	private final int mPageCount = 2;

	public TimePeriodPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public TimePeriodPreference(Context context, AttributeSet attrs)
	{
		this(context, attrs, android.R.attr.preferenceStyle);

		handleAttributes(context, attrs);

		// This ensures that the created dialog actually has buttons
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	@Override
	public CharSequence getSummary()
	{
//		if(!mHasSummary)
//			updateSummary(mBegin, mEnd);

		return super.getSummary();

		/*if(mHasSummary)
			return super.getSummary();

		final CharSequence summary = super.getSummary();
		if(summary != null)
			return summary;

		final TimePeriod value = getValue();
		if(value != null)
			return DateTime.toNativeTime(value.begin()) + "-" + DateTime.toNativeTime(value.end());

		return null;*/
	}

	@Override
	protected String toPersistedString(TimePeriod value)
	{
		String str = value.toString();
		return str;
	}

	@Override
	protected TimePeriod fromPersistedString(String string) {
		return TimePeriod.fromString(string);
	}

	@Override
	protected String toSummaryString(TimePeriod value)
	{
		final String beginStr = DateTime.toNativeTime(value.begin());
		final String endStr = DateTime.toNativeTime(value.end());

		return getContext().getString(R.string._title_x_to_y, beginStr, endStr);
	}

	@Override
	protected TimePeriod getDialogValue() {
		return new TimePeriod(mBegin, mEnd);
	}

	@Override
	protected void onValueSet(TimePeriod value)
	{
		mBegin = value.begin();
		mEnd = value.end();
	}

	@Override
	protected View onCreateDialogView()
	{
		if(mContainer == null)
		{
			mContainer = getLayoutInflater().inflate(R.layout.time_period_preference, null);

			mMessageView = (TextView) mContainer.findViewById(R.id.message);
			mTimePicker = (TimePicker) mContainer.findViewById(R.id.picker);

			mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
			mTimePicker.setOnTimeChangedListener(mTimeListener);
		}

		return mContainer;
	}

	@Override
	protected void onPrepareDialog(Dialog dialog) {
		dialog.setOnShowListener(mOnShowListener);
	}

	private void handleAttributes(Context context, AttributeSet attrs)
	{
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimePeriodPreference);
		final int[] timeIds = { R.styleable.TimePeriodPreference_after, R.styleable.TimePeriodPreference_before };
		for(int i = 0; i != timeIds.length; ++i)
		{
			final String str = a.getString(timeIds[i]);
			if(str != null)
			{
				try
				{
					mConstraintTimes[i] = DumbTime.fromString(str);
				}
				catch(IllegalArgumentException e)
				{
					if(str.equals(getKey()))
						throw new IllegalArgumentException("Preference references itself in min/maxTime");

					mConstraintKeys[i] = str;
				}
			}
		}

		mAllowEndWrap = a.getBoolean(R.styleable.TimePeriodPreference_allowEndWrap, false);
		a.recycle();
	}

	private void onPageChanged(int page)
	{
		if(mCurrentPage != page)
			mCurrentPage = page;

		// Util.setTimePickerTime() would cause the listener to
		// be called, so we temporarily replace it with a dummy.
		mTimePicker.setOnTimeChangedListener(mTimeListenerDummy);

		if(page == BEGIN)
		{
			mBackButton.setText(android.R.string.cancel);
			mNextButton.setText(R.string._btn_next);

			Util.setTimePickerTime(mTimePicker, mBegin);
		}
		else if(page == END)
		{
			mBackButton.setText(R.string._btn_back);
			mNextButton.setText(android.R.string.ok);

			Util.setTimePickerTime(mTimePicker, mEnd);
		}

		final Dialog dialog = getDialog();
		if(dialog != null)
		{
			final int[] titleIds = { R.string._title_begin, R.string._title_end };
			dialog.setTitle(getTitle() + " - " + getContext().getString(titleIds[page]));
		}

		mTimePicker.setOnTimeChangedListener(mTimeListener);
		updateMessageAndButtons();
	}

	private boolean isCurrentlyVisibleTimePickerValueValid()
	{
		final DumbTime current = DumbTime.fromTimePicker(mTimePicker);
		final DumbTime min, max;

		min = getConstraintTimeForCurrentlyVisibleTimePicker(MIN);
		max = getConstraintTimeForCurrentlyVisibleTimePicker(MAX);

		if(min == null && max == null)
			return true;

		return current.isWithinRange(min, max, mCurrentPage == END ? mAllowEndWrap : false);
	}

	private DumbTime getConstraintTime(int which)
	{
		if(mConstraintKeys[which] != null)
		{
			final Preference p = findPreferenceInHierarchy(mConstraintKeys[which]);
			if(p instanceof TimePeriodPreference)
			{
				final TimePeriod period = ((TimePeriodPreference) p).getValue();
				return (which == MIN) ? period.mEnd : period.mBegin;
			}
			else
			{
				Log.w(TAG, "No TimePeriodPreference with key=" + mConstraintKeys[which] + " in hierarchy (yet).");
				return null;
			}


		}

		return mConstraintTimes[which];
	}

	private DumbTime getConstraintTimeForCurrentlyVisibleTimePicker(int which)
	{
		if(mCurrentPage == BEGIN)
		{
			final DumbTime min = getConstraintTime(MIN);
			if(min == null)
				return null;

			if(which == MIN)
			{
				if(min.isGreaterThan(mEnd) && !mAllowEndWrap)
					return null;

				return min;
			}
			else
			{
				if(mEnd.isLessThan(min) && mAllowEndWrap)
					return null;

				return get1MinuteBefore(mEnd);
			}
		}
		else
		{
			final DumbTime max = getConstraintTime(MAX);
			if(max == null)
				return null;

			if(which == MIN)
			{
				if(mBegin.isGreaterThan(max) && !mAllowEndWrap)
					return null;

				return get1MinuteAfter(mBegin);
			}
			else
				return max;
		}
	}

	private void updateMessageAndButtons()
	{
		if(mNextButton == null)
			return;

		final String min = getConstraintTimeForCurrentlyVisibleTimePickerAsString(MIN);
		final String max = getConstraintTimeForCurrentlyVisibleTimePickerAsString(MAX);

		final int resId;

		if(min != null && max != null)
			resId = R.string._msg_constraints_ab;
		else if(max != null)
			resId = R.string._msg_constraints_b;
		else if(min != null)
			resId = R.string._msg_constraints_a;
		else
			resId = 0;

		if(resId != 0)
			mMessageView.setText(getContext().getString(resId, min, max));
		else
			mMessageView.setVisibility(View.GONE);

		final boolean isValidTime = isCurrentlyVisibleTimePickerValueValid();
		mNextButton.setEnabled(isValidTime);
		if(mCurrentPage == 1)
			mBackButton.setEnabled(isValidTime);
	}

	private String getConstraintTimeForCurrentlyVisibleTimePickerAsString(int which)
	{
		final DumbTime time = getConstraintTimeForCurrentlyVisibleTimePicker(which);
		if(time == null || time.equals(Constants.MIDNIGHT))
			return null;

		return DateTime.toNativeTime(time);
	}

	private static DumbTime get1MinuteAfter(DumbTime time)
	{
		if(time == null)
			return null;

		long millis = time.getMillisFromMidnight() + 60000;
		if(millis >= Constants.MILLIS_PER_DAY)
			millis -= Constants.MILLIS_PER_DAY;

		return new DumbTime(millis);
	}

	private static DumbTime get1MinuteBefore(DumbTime time)
	{
		if(time == null)
			return null;

		long millis = time.getMillisFromMidnight() - 60000;
		if(millis < 0)
			millis += Constants.MILLIS_PER_DAY;

		return new DumbTime(millis);
	}

	private final OnTimeChangedListener mTimeListener = new OnTimeChangedListener() {

		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
		{
			final DumbTime newTime = new DumbTime(hourOfDay, minute);
			if(mCurrentPage == 0)
				mBegin = newTime;
			else
				mEnd = newTime;

			//mNextButton.setEnabled(isCurrentlyVisibleTimePickerValueValid());
			updateMessageAndButtons();
		}
	};

	private final OnTimeChangedListener mTimeListenerDummy = new OnTimeChangedListener() {

		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			// dummy
		}
	};

	private final OnShowListener mOnShowListener = new OnShowListener() {

		@Override
		public void onShow(DialogInterface dialogInterface)
		{
			final AlertDialog dialog = (AlertDialog) dialogInterface;
			final Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
			final Button negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE);

			// Before Honeycomb, the positive button was on the left. For our purposes,
			// we want the 'next' button to be on the right, and the 'back' button on
			// the left.
			// In case some locale uses the exact opposite, we dynamically check the
			// buttons' locations each time.

			if(positiveButton.getLeft() < negativeButton.getLeft())
			{
				mBackButton = positiveButton;
				mNextButton = negativeButton;
			}
			else
			{
				mBackButton = negativeButton;
				mNextButton = positiveButton;
			}

			mBackButton.setOnClickListener(mOnBtnClickListener);
			mNextButton.setOnClickListener(mOnBtnClickListener);

			final TimePeriod value = getValue();
			mBegin = value.begin();
			mEnd = value.end();

			onPageChanged(0);
		}
	};

	private final OnClickListener mOnBtnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			// FIXME call onClick(DialogInterface, int) here!!!

			mTimePicker.clearFocus();

			if(v == mBackButton)
			{
				if(mCurrentPage == 0)
				{
					TimePeriodPreference.this.onClick(getDialog(), Dialog.BUTTON_NEGATIVE);

					//onDialogClosed(false);
					getDialog().dismiss();
					return;
				}

				onPageChanged(--mCurrentPage);
			}
			else if(v == mNextButton)
			{
				// must be called before incrementing mCurrentPage
				final boolean positiveResult = isCurrentlyVisibleTimePickerValueValid();

				if(++mCurrentPage == mPageCount)
				{
					final int which = positiveResult ? Dialog.BUTTON_POSITIVE :
							Dialog.BUTTON_NEGATIVE;

					TimePeriodPreference.this.onClick(getDialog(), which);

					// We check the value again because it might have
					// changed by the call to clearFocus()
					//onDialogClosed(positiveResult);
					getDialog().dismiss();
					return;
				}

				onPageChanged(mCurrentPage);
			}
		}
	};

	/*private boolean hasValidConstraints(boolean checkRefs)
	{
		final DumbTime min = checkRefs ? getConstraintTime(MIN) : mConstraintTimes[MIN];
		final DumbTime max = checkRefs ? getConstraintTime(MAX) : mConstraintTimes[MAX];

		//if(min != null && max != null)
		//	return (min.after(max) && mAllowConstraintWrap[MIN]) || (max.before(min) && mAllowConstraintWrap[MAX]);

		return true;
	}*/
}
