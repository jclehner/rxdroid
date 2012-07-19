package at.caspase.rxdroid.preferences;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Formatter;
import java.util.StringTokenizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import at.caspase.androidutils.MyDialogPreference;
import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.preferences.TimePeriodPreference.TimePeriod;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.Util;

public class TimePeriodPreference extends MyDialogPreference<TimePeriod>
{
	private static final String TAG = TimePeriodPreference.class.getName();

	private static final int END = 1;
	private static final int MAX = END;
	private static final int BEGIN = 0;
	private static final int MIN = BEGIN;

	public static class TimePeriod implements Serializable
	{
		private static final long serialVersionUID = -2432714902425872383L;

		private final DumbTime mBegin;
		private final DumbTime mEnd;

		public TimePeriod(DumbTime begin, DumbTime end)
		{
			this.mBegin = begin;
			this.mEnd = end;
		}

		@Override
		public String toString() {
			return "" + mBegin + "-" + mEnd;
		}

		public static TimePeriod fromString(String string)
		{
			final StringTokenizer st = new StringTokenizer(string, "|-");
			final DumbTime begin, end;

			begin = DumbTime.fromString(st.nextToken());
			end = DumbTime.fromString(st.nextToken());

			return new TimePeriod(begin, end);
		}

		public DumbTime getBegin() {
			return mBegin;
		}

		public DumbTime getEnd() {
			return mEnd;
		}
	}

	//private CharSequence mSummary;
	//private TimePeriod mDialogValue;

	private DumbTime mBegin;
	private DumbTime mEnd;

	private String[] mConstraintKeys = new String[2];
	private DumbTime[] mConstraintTimes = new DumbTime[2];
	//private boolean[] mAllowConstraintWrap = { false, false };
	private boolean[] mAllowTimeWrap = { false, false };

	private View mContainer;
	private TimePicker mTimePicker;
	private TextView mMessageView;

	private Button mBackButton;
	private Button mNextButton;

	private int mCurrentPage = 0;
	private final int mPageCount = 2;

	public TimePeriodPreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, android.R.attr.preferenceStyle);

		handleAttributes(context, attrs);

		// This ensures that the created dialog actually has buttons
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	public TimePeriodPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}

	@Override
	public CharSequence getSummary()
	{
		/*final CharSequence summary = super.getSummary();
		if(summary == null)
			return getValue().toString();

		return summary;*/

		return getValue().toString();

		/*if(mSummary == null)
			return super.getSummary();

		Formatter f = new Formatter();
		return f.format(mSummary.toString(), getValue()).toString();*/

		//return getContext().getStr
	}

	//public set

	@Override
	protected String toPersistedString(TimePeriod value) {
		return value.toString();
	}

	@Override
	protected TimePeriod fromPersistedString(String string) {
		return TimePeriod.fromString(string);
	}

	@Override
	protected TimePeriod getDialogValue()
	{
		Log.d(TAG, "getDialogValue: value=" + mBegin + "-" + mEnd);
		return new TimePeriod(mBegin, mEnd);
	}

	@Override
	protected void onValueSet(TimePeriod value)
	{
		mBegin = value.getBegin();
		mEnd = value.getEnd();
	}

	@Override
	protected View onCreateDialogView()
	{
		if(mContainer == null)
		{
			mContainer = LayoutInflater.from(getContext()).inflate(R.layout.time_period_preference, null);

			mMessageView = (TextView) mContainer.findViewById(R.id.message);
			mTimePicker = (TimePicker) mContainer.findViewById(R.id.picker);

			mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
			//mTimePicker.setCurrentHour(mDialogValue.begin.getHours());
			//mTimePicker.setCurrentMinute(mDialogValue.begin.getMinutes());
			mTimePicker.setOnTimeChangedListener(mTimeListener);

			//mMessageView.
		}

		return mContainer;
	}

	@Override
	protected void onCustomizeDialog(Dialog dialog) {
		dialog.setOnShowListener(mOnShowListener);
	}

	private void handleAttributes(Context context, AttributeSet attrs)
	{
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimePeriodPreference);

		final int[] timeIds = { R.styleable.TimePeriodPreference_minTime, R.styleable.TimePeriodPreference_maxTime };
		final int[] timeWrapIds = { R.styleable.TimePeriodPreference_allowBeginWrap, R.styleable.TimePeriodPreference_allowEndWrap };

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

		for(int i = 0; i != timeWrapIds.length; ++i)
		{
			//mAllowConstraintWrap[i] = a.getBoolean(constraintWrapIds[i], false);
			mAllowTimeWrap[i] = a.getBoolean(timeWrapIds[i], false);
		}

		if(mAllowTimeWrap[BEGIN] && mAllowTimeWrap[END])
			throw new IllegalArgumentException("Cannot set both allowBeginWrap and allowEndWrap");

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

		return current.isWithinRange(min, max, mAllowTimeWrap[mCurrentPage]);
	}

	private DumbTime getConstraintTime(int which)
	{
		if(mConstraintKeys[which] != null)
		{
			final Preference p = findPreferenceInHierarchy(mConstraintKeys[which]);
			if(p == null || (!(p instanceof TimePeriodPreference) && !(p instanceof TimePreference)))
			{
				Log.w(TAG, "No TimePreference or TimePeriodPreference with key=" + mConstraintKeys[which] + " in hierarchy (yet).");
				return null;
			}

			if(p instanceof TimePeriodPreference)
			{
				final TimePeriod period = ((TimePeriodPreference) p).getValue();
				return (which == MIN) ? period.mEnd : period.mBegin;
			}
			else
				return ((TimePreference) p).getValue();
		}

		return mConstraintTimes[which];
	}

	private DumbTime getConstraintTimeForCurrentlyVisibleTimePicker(int which)
	{
		if(mCurrentPage == BEGIN)
		{
			final DumbTime min = getConstraintTime(MIN);

			if(which == MAX)
			{
				if(mEnd.before(min))
					return null;

				return mEnd;
			}
			else
				return min;
		}
		else
		{
			final DumbTime max = getConstraintTime(MAX);

			if(which == MIN)
			{
				if(mBegin.after(max))
					return null;

				return mBegin;
			}
			else
				return max;
		}
	}

	private void updateMessageAndButtons()
	{
		final DumbTime min = getConstraintTimeForCurrentlyVisibleTimePicker(MIN);
		final DumbTime max = getConstraintTimeForCurrentlyVisibleTimePicker(MAX);

		final int resId;

		if(min != null && max != null)
			resId = R.string._msg_constraints_ab;
		else if(min == null)
			resId = R.string._msg_constraints_b;
		else
			resId = R.string._msg_constraints_a;

		mMessageView.setText(getContext().getString(resId, min, max));

		final boolean isValidTime = isCurrentlyVisibleTimePickerValueValid();
		mNextButton.setEnabled(isCurrentlyVisibleTimePickerValueValid());
		if(mCurrentPage == 1)
			mBackButton.setEnabled(isValidTime);
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
			mBegin = value.getBegin();
			mEnd = value.getEnd();

			onPageChanged(0);
		}
	};

	private final OnClickListener mOnBtnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			if(v == mBackButton)
			{
				if(mCurrentPage == 0)
				{
					onDialogClosed(false);
					getDialog().dismiss();
					return;
				}

				onPageChanged(--mCurrentPage);
			}
			else if(v == mNextButton)
			{
				if(++mCurrentPage == mPageCount)
				{
					onDialogClosed(true);
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
