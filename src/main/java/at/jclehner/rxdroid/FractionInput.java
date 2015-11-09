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

package at.jclehner.rxdroid;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import at.jclehner.androidutils.InstanceState;
import at.jclehner.androidutils.InstanceState.SaveState;
import android.widget.NumberPicker.OnValueChangeListener;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.ShowcaseViews;
import at.jclehner.rxdroid.util.Util;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.github.espiandev.showcaseview.ShowcaseViewBuilder2;

/**
 * A widget for fraction input.
 *
 * @author Joseph Lehner
 */
public class FractionInput extends LinearLayout
{
	public interface OnChangedListener
	{
		public void onFractionChanged(FractionInput widget, Fraction oldValue);
	}

	private static final String TAG = FractionInput.class.getSimpleName();
	private static final boolean LOGV = false;

	private static final String[] MODE_SWITCHER_LABELS = { "1", "1\u00BD", "\u00BD" };

	public static final int MODE_INTEGER = 0;
	public static final int MODE_MIXED = 1;
	public static final int MODE_FRACTION = 2;
	public static final int MODE_INVALID = 3;

	private NumberPicker mIntegerPicker;
	private NumberPicker mNumeratorPicker;
	private NumberPicker mDenominatorPicker;
	private TextView mFractionBar;
	private Button mModeSwitcher;

	private final ShowcaseViews mShowcaseQueue = new ShowcaseViews();

	@SaveState
	private int mInteger = 0;
	@SaveState
	private int mNumerator = 0;
	@SaveState
	private int mDenominator = 1;

	@SaveState
	private int mFractionInputMode = MODE_INVALID;
	@SaveState
	private boolean mIsAutoInputModeEnabled = false;

	private boolean mAttached = false;

	private OnChangedListener mListener;

	public FractionInput(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		LayoutInflater lf = LayoutInflater.from(context);
		lf.inflate(R.layout.fraction_input2, this, true);

		mIntegerPicker = (NumberPicker) findViewById(R.id.integer);
		mNumeratorPicker = (NumberPicker) findViewById(R.id.numerator);
		mDenominatorPicker = (NumberPicker) findViewById(R.id.denominator);
		mFractionBar = (TextView) findViewById(R.id.fraction_bar);
		mModeSwitcher = (Button) findViewById(R.id.mode_switcher);

		setPickerLimits(0, 9999);

		mIntegerPicker.setOnValueChangedListener(mPickerListener);
		mIntegerPicker.setWrapSelectorWheel(false);

		mNumeratorPicker.setOnValueChangedListener(mPickerListener);
		mNumeratorPicker.setWrapSelectorWheel(false);

		mDenominatorPicker.setOnValueChangedListener(mPickerListener);
		mDenominatorPicker.setMinValue(1); // must be called before setWrapSelectorWheel!
		mDenominatorPicker.setWrapSelectorWheel(false);

		mModeSwitcher.setOnClickListener(mModeSwitcherListener);

		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_HORIZONTAL);
		setFractionInputMode(MODE_FRACTION);

		updateView();
	}

	public void setValue(Fraction value)
	{
		if(mIsAutoInputModeEnabled)
		{
			final int mode;

			if(value.isInteger())
				mode = MODE_INTEGER;
			else if(Double.compare(value.doubleValue(), 1.0) == 1)
				mode = MODE_MIXED;
			else
				mode = MODE_FRACTION;

			if(LOGV) Log.v(TAG, "setValue: mode " + mFractionInputMode + " -> " + mode + " (auto)");

			if(mode != mFractionInputMode)
			{
				if(LOGV) Log.v(TAG, "setValue: mode " + mFractionInputMode + " -> " + mode + " (auto)");
				mFractionInputMode = mode;
			}
		}
		else
			if(LOGV) Log.v(TAG, "setValue: mode=" + mFractionInputMode);

		// for MODE_INTEGER and MODE_MIXED get the value as a mixed number
		int data[] = value.getFractionData(mFractionInputMode != MODE_FRACTION);

		mInteger = data[0];
		mNumerator = data[1];
		mDenominator = data[2];

		updateView();
	}

	public Fraction getValue() {
		return new Fraction(mInteger, mNumerator, mDenominator);
	}

	/**
	 * Sets the widget's input mode.
	 * <p>
	 * Valid input modes are MODE_INTEGER, MODE_FRACTION and MODE_MIXED. The mode set
	 * determines which number picker widgets are visible. Note that a call to
	 * <code>setMode(MODE_INTEGER)</code> is ignored if the widget's underlying value
	 * cannot be converted to an integer.
	 * <p>
	 * Note that automatic fraction intput mode guessing (see {@link #setAutoInputModeEnabled(boolean)})
	 * will be disabled, if a call to this function succeeds (i.e. returns <code>true</code>).
	 *
	 * @param mode either MODE_INTEGER, MODE_FRACTION or MODE_MIXED
	 * @return <code>false</code> if mode is MODE_INTEGER but the underlying value is
	 *         not an integer. For other arguments, this function always returns <code>true</code>.
	 */
	public boolean setFractionInputMode(int mode) {
		return setFractionInputMode(mode, false);
	}

	public void disableFractionInputMode(boolean disable)
	{
		final int visibility;

		if(disable)
		{
			setFractionInputMode(MODE_INTEGER);
			visibility = View.GONE;
		}
		else
			visibility = View.VISIBLE;

		mModeSwitcher.setVisibility(visibility);
	}

	public boolean isFractionInputModeDisabled() {
		return mModeSwitcher.getVisibility() == View.GONE && mFractionInputMode == MODE_INTEGER;
	}

	public int getFractionInputMode() {
		return mFractionInputMode;
	}

	/**
	 * Enables automatic input mode setting.
	 * <p>
	 * If enabled, {@link #setValue(Fraction)} will try to guess the most
	 * appropriate input mode for that value.
	 * <p>
	 * Note that later calls to {@link #setFractionInputMode(int)} will override
	 * this setting.
	 *
	 * @param enabled
	 */
	public void setAutoInputModeEnabled(boolean enabled)
	{
		mIsAutoInputModeEnabled = enabled;
		if(enabled)
			setValue(getValue());
	}

	public boolean isAutoInputModeEnabled(boolean enabled) {
		return mIsAutoInputModeEnabled;
	}

	public OnChangedListener getOnChangeListener() {
		return mListener;
	}

	public void setOnChangeListener(OnChangedListener listener) {
		mListener = listener;
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);

		mIntegerPicker.setEnabled(enabled);
		mNumeratorPicker.setEnabled(enabled);
		mDenominatorPicker.setEnabled(enabled);
		mModeSwitcher.setEnabled(enabled);
	}

	@Override
	public void clearFocus()
	{
		mIntegerPicker.clearFocus();
		mNumeratorPicker.clearFocus();
		mDenominatorPicker.clearFocus();
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		Parcelable superState = super.onSaveInstanceState();
		return InstanceState.createFrom(this, superState, null);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		Parcelable superState = InstanceState.getSuperState(state);
		super.onRestoreInstanceState(superState);
		InstanceState.restoreTo(this, state);

		updateView();
	}

	@Override
	public void setVisibility(int visibility)
	{
		super.setVisibility(visibility);
		maybeExplainCurrentState();
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		mAttached = true;
		maybeExplainCurrentState();
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		mShowcaseQueue.hide();
		mAttached = false;
	}

	private void setPickerLimits(int min, int max)
	{
		final NumberPicker[] pickers = {
				mIntegerPicker, mNumeratorPicker, mDenominatorPicker
		};

		for(NumberPicker picker : pickers)
		{
			picker.setMinValue(min);
			picker.setMaxValue(max);
		}
	}

	private void updateView()
	{
		// hide in fraction mode
		mIntegerPicker.setVisibility(mFractionInputMode == MODE_FRACTION ? GONE : VISIBLE);
		// hide in integer mode
		mNumeratorPicker.setVisibility(mFractionInputMode == MODE_INTEGER ? GONE: VISIBLE);
		mDenominatorPicker.setVisibility(mFractionInputMode == MODE_INTEGER ? GONE: VISIBLE);
		mFractionBar.setVisibility(mFractionInputMode == MODE_INTEGER ? GONE: VISIBLE);

		mIntegerPicker.setValue(mInteger);
		mNumeratorPicker.setValue(mNumerator);
		mDenominatorPicker.setValue(mDenominator);

		updateModeSwitcher();
	}

	private boolean setFractionInputMode(int mode, boolean maybeExplainState)
	{
		if(mode == MODE_INTEGER)
		{
			if(!getValue().isInteger())
				return false;
		}
		else if(mode >= MODE_INVALID)
			throw new IllegalArgumentException();

		// an explicit request for a specific mode overrides the automatic setting
		setAutoInputModeEnabled(false);

		if(mode != mFractionInputMode)
		{
			mFractionInputMode = mode;
			setValue(getValue());
			if(maybeExplainState)
				maybeExplainCurrentState();
		}

		return true;
	}

	private void maybeExplainCurrentState()
	{
		if(!mAttached || getVisibility() != View.VISIBLE)
			return;

		if(!BuildConfig.DEBUG)
		{
			if(Settings.getBoolean(Settings.Keys.HAS_FRACTIONS_IN_ANY_SCHEDULE, false))
				return;
		}

		post(new Runnable() {

			@Override
			public void run()
			{
				if(!isShown())
					return;

				if(mFractionInputMode == MODE_INTEGER && !isFractionInputModeDisabled())
				{
					mShowcaseQueue.add(makeShowcaseView(mModeSwitcher, 0xdeadc0de,
							R.string._help_title_to_fraction_mode, R.string._help_msg_to_fraction_mode));
				}
				else if(mFractionInputMode == MODE_FRACTION)
				{
					mShowcaseQueue.add(makeShowcaseView(null, 0xdeadc0de+1,
							R.string._help_title_input_fraction, R.string._help_msg_input_fraction));

					mShowcaseQueue.add(makeShowcaseView(mModeSwitcher, 0xdeadc0de+2,
							R.string._help_title_to_mixed_mode, R.string._help_msg_to_mixed_mode));
				}
				else if(mFractionInputMode == MODE_MIXED)
				{
					mShowcaseQueue.add(makeShowcaseView(null, 0xdeadc0de+3,
							R.string._help_title_input_mixed, R.string._help_msg_input_mixed));
				}

				mShowcaseQueue.show();
			}
		});
	}

	private ShowcaseView makeShowcaseView(View view, int showcaseId, int titleResId, int msgResId)
	{
		ShowcaseViewBuilder2 svb = new ShowcaseViewBuilder2(getContext());
		svb.setHideOnClickOutside(false);
		svb.setBlock(false);
		svb.setShowcaseId(showcaseId);
		svb.setText(titleResId, msgResId);
		svb.setShowcaseView(view);
		svb.setShotType(ShowcaseView.TYPE_ONE_SHOT);

		return svb.build();
	}

	private void updateModeSwitcher()
	{
		final int nextMode = getNextInputMode();
		mModeSwitcher.setText(MODE_SWITCHER_LABELS[nextMode]);
	}

	private int getNextInputMode()
	{
		final int[] modes = getValue().isInteger() ? MODES_IF_INTEGER_VALUE : MODES_IF_FRACTION_VALUE;
		final int modeIndex = CollectionUtils.indexOf(mFractionInputMode, modes);

		if(modeIndex == -1)
		{
			Log.e(TAG, "Unexpected input mode, defaulting to MODE_MIXED");
			return MODE_MIXED;
		}

		return modes[(modeIndex + 1) % modes.length];
	}

	private OnValueChangeListener mPickerListener = new OnValueChangeListener() {

		@Override
		public void onValueChange(NumberPicker picker, int oldVal, int newVal)
		{
			Fraction oldValue = getValue();

			if(picker.getId() == R.id.integer)
				mInteger = newVal;
			else if(picker.getId() == R.id.numerator)
				mNumerator = newVal;
			else if(picker.getId() == R.id.denominator)
			{
				if(newVal > 0)
					mDenominator = newVal;
				else // this shouldn't happen
					mDenominator = 1;
			}
			else
				return;

			updateModeSwitcher();

			if(mListener != null)
				mListener.onFractionChanged(FractionInput.this, oldValue);
		}
	};

	private static final int[] MODES_IF_INTEGER_VALUE = { MODE_FRACTION, MODE_MIXED, MODE_INTEGER };
	private static final int[] MODES_IF_FRACTION_VALUE = { MODE_MIXED, MODE_FRACTION };

	private OnClickListener mModeSwitcherListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			setFractionInputMode(getNextInputMode(), true);
		}
	};
}
