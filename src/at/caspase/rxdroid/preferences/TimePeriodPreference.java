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
import at.caspase.rxdroid.util.Util;

public class TimePeriodPreference extends MyDialogPreference<TimePeriod>
{
	private static final String TAG = TimePeriodPreference.class.getName();

	private static final int BEFORE = 1;
	private static final int MAX = BEFORE;
	private static final int AFTER = 0;
	private static final int MIN = AFTER;

	public static class TimePeriod implements Serializable
	{
		private static final long serialVersionUID = -2432714902425872383L;

		public TimePeriod(DumbTime begin, DumbTime end)
		{
			this.begin = begin;
			this.end = end;
		}

		@Override
		public String toString() {
			return "" + begin + "-" + end;
		}

		public static TimePeriod fromString(String string)
		{
			final StringTokenizer st = new StringTokenizer(string, "|-");
			final DumbTime begin, end;

			begin = DumbTime.fromString(st.nextToken());
			end = DumbTime.fromString(st.nextToken());

			return new TimePeriod(begin, end);
		}

		public final DumbTime begin;
		public final DumbTime end;
	}

	private CharSequence mSummary;

	private TimePeriod mDialogValue;

	private String[] mConstraintKeys = new String[2];
	private DumbTime[] mConstraintTimes = new DumbTime[2];
	private boolean[] mAllowConstraintWrap = { false, false };

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

	public DumbTime getBegin() {
		return mDialogValue.begin;
	}

	public DumbTime getEnd() {
		return mDialogValue.end;
	}

	@Override
	public CharSequence getSummary()
	{
		final CharSequence summary = super.getSummary();
		if(summary == null)
			return getValue().toString();

		return summary;

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
	protected TimePeriod getDialogValue() {
		return mDialogValue;
	}

	@Override
	protected void onValueSet(TimePeriod value) {
		mDialogValue = value;
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
	protected void onShowDialog(Dialog dialog)
	{
		dialog.setOnShowListener(mOnShowListener);

		final ViewParent parent = mContainer.getParent();
		if(parent instanceof ViewGroup)
		{
			((ViewGroup) parent).removeView(mContainer);
			Log.d(TAG, "onShowDialog: removed mContainer from parent");
		}
	}

	@Override
	protected boolean callChangeListener(Object newValue)
	{
		/*if(!checkTime())
		{
			Log.w(TAG, "Attempting to persist a time period that is not within specified constraints.");
			Log.w(TAG "         value: " + getValue());
			Log.w(TAG, "  minTime/Key: " + mConstraintTimes[MIN] + "/" + mConstraintKeys[MIN]);
			Log.w(TAG, "  maxTime/Key: " + mConstraintTimes[MAX] + "/" + mConstraintKeys[MAX]);
		}*/

		return super.callChangeListener(newValue);
	}

	@Override
	protected void onAttachedToHierarchy(PreferenceManager preferenceManager)
	{
		super.onAttachedToHierarchy(preferenceManager);

		if(!hasValidConstraints(true))
			throw new IllegalStateException("Referenced constraints are invalid without wrapping enabled");
	}

	private void handleAttributes(Context context, AttributeSet attrs)
	{
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimePeriodPreference);

		final int[] timeIds = { R.styleable.TimePeriodPreference_minTime, R.styleable.TimePeriodPreference_maxTime };
		final int[] wrapIds = { R.styleable.TimePeriodPreference_allowMinWrap, R.styleable.TimePeriodPreference_allowMaxWrap };

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
					mConstraintKeys[i] = str;
				}
			}
		}

		for(int i = 0; i != wrapIds.length; ++i)
			mAllowConstraintWrap[i] = a.getBoolean(wrapIds[i], false);

		if(mAllowConstraintWrap[MIN] && mAllowConstraintWrap[MAX])
			throw new IllegalArgumentException("allowMinWrap and allowMaxWrap are mutually exclusive");

		if(!hasValidConstraints(false))
			throw new IllegalArgumentException("Specified range is invalid without wrapping enabled");

		mSummary = getSummary();

		//mSummary = context.obtainStyledAttributes(attrs, android.R.attr.pref)
	}

	private void onPageChanged(int page)
	{
		if(mCurrentPage != page)
			mCurrentPage = page;

		// Util.setTimePickerTime() would cause the listener to
		// be called, so we temporarily replace it with a dummy.
		mTimePicker.setOnTimeChangedListener(mTimeListenerDummy);

		if(page == 0)
		{
			mBackButton.setText(android.R.string.cancel);
			mNextButton.setText(R.string._btn_next);

			Util.setTimePickerTime(mTimePicker, mDialogValue.begin);
		}
		else if(page == 1)
		{
			mBackButton.setText(R.string._btn_back);
			mNextButton.setText(android.R.string.ok);

			Util.setTimePickerTime(mTimePicker, mDialogValue.end);
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

		return current.isWithinRange(min, max, mAllowConstraintWrap[mCurrentPage == 0 ? MIN : MAX]);
	}

	private DumbTime getConstraintTime(int which)
	{
		if(mConstraintKeys[which] != null)
		{
			final Preference p = findPreferenceInHierarchy(mConstraintKeys[which]);
			if(p == null || (!(p instanceof TimePeriodPreference) && !(p instanceof TimePreference)))
			{
				Log.w(TAG, "No TimePreference or TimePeriodPreference with key=" + mConstraintKeys[which] + " in hierarchy.");
				return null;
			}

			final DumbTime constraintTime;

			if(p instanceof TimePeriodPreference)
			{
				final TimePeriod period = ((TimePeriodPreference) p).getValue();
				constraintTime = (which == MIN) ? period.end : period.begin;
			}
			else
				constraintTime = ((TimePreference) p).getValue();

			return constraintTime;
		}

		return mConstraintTimes[which];
	}

	private DumbTime getConstraintTimeForCurrentlyVisibleTimePicker(int which)
	{
		if(mCurrentPage == 0)
			return which == MIN ? getConstraintTime(MIN) : mDialogValue.end;
		else
			return which == MIN ? mDialogValue.begin : getConstraintTime(MAX);
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

	private String toDebugString()
	{
		return
			"TimePeriodPreference(key=\"" + getKey() + "\", range=" + getValue() +
			", wrap=" + Arrays.toString(mAllowConstraintWrap) + ")";
	}

	private final OnTimeChangedListener mTimeListener = new OnTimeChangedListener() {

		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
		{
			final DumbTime time = mCurrentPage == 0 ? mDialogValue.begin : mDialogValue.end;

			time.setHours(hourOfDay);
			time.setMinutes(minute);

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
				Log.d(TAG, "onShow: positive button is on the left");
			}
			else
			{
				mBackButton = negativeButton;
				mNextButton = positiveButton;
				Log.d(TAG, "onShow: positive button is on the right");
			}

			mBackButton.setOnClickListener(mOnBtnClickListener);
			mNextButton.setOnClickListener(mOnBtnClickListener);

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

	private boolean hasValidConstraints(boolean checkRefs)
	{
		final DumbTime min = checkRefs ? getConstraintTime(MIN) : mConstraintTimes[MIN];
		final DumbTime max = checkRefs ? getConstraintTime(MAX) : mConstraintTimes[MAX];

		Log.d(TAG, "hasValidConstraints");
		Log.d(TAG, "  min=" + min);
		Log.d(TAG, "  max=" + max);
		Log.d(TAG, " this=" + toDebugString());


		if(min != null && max != null)
			return (min.after(max) && mAllowConstraintWrap[MIN]) || (max.before(min) && mAllowConstraintWrap[MAX]);

		return true;
	}
}
