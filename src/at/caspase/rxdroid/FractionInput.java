package at.caspase.rxdroid;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quietlycoding.android.picker.NumberPicker;

/**
 * A widget for fraction input.
 *
 * This implementation basically uses the private NumberPicker implementation
 * available in the pre-Honeycomb sources.
 *
 * @author Joseph Lehner
 */
public class FractionInput extends LinearLayout implements NumberPicker.OnChangedListener, OnClickListener
{
	public interface OnChangedListener
	{
		public void onChanged(FractionInput widget, Fraction oldValue);
	}

	private static final String TAG = FractionInput.class.getName();
	private static final int MAX = 99999;

	public static final int MODE_INTEGER = 1;
	public static final int MODE_MIXED = 2;
	public static final int MODE_FRACTION = 3;
	public static final int MODE_INVALID = 4;

	private NumberPicker mIntegerPicker;
	private NumberPicker mNumeratorPicker;
	private NumberPicker mDenominatorPicker;
	private TextView mFractionBar;
	private Button mModeSwitcher;

	private int mInteger = 0;
	private int mNumerator = 0;
	private int mDenominator = 1;

	private int mFractionInputMode = MODE_INVALID;

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

		mIntegerPicker.setOnChangeListener(this);
		mIntegerPicker.setRange(0, MAX);
		mIntegerPicker.setWrap(false);

		mNumeratorPicker.setOnChangeListener(this);
		mNumeratorPicker.setRange(0, MAX);
		mNumeratorPicker.setWrap(false);

		mDenominatorPicker.setOnChangeListener(this);
		mDenominatorPicker.setRange(1, MAX);
		mDenominatorPicker.setWrap(false);

		mModeSwitcher.setOnClickListener(this);

		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_HORIZONTAL);
		setFractionInputMode(MODE_FRACTION);

		updateView();
	}

	public void setValue(Fraction value)
	{
		// for MODE_INTEGER and MODE_MIXED get the value as a mixed number
		int data[] = value.getFractionData(mFractionInputMode != MODE_FRACTION);

		mInteger = (mFractionInputMode == MODE_FRACTION) ? 0 : data[0];
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
	 *
	 * @param mode either MODE_INTEGER, MODE_FRACTION or MODE_MIXED
	 * @return <code>false</code> if mode is MODE_INTEGER but the underlying value is
	 *         not an integer. For other arguments, this function always returns <code>true</code>.
	 */
	public boolean setFractionInputMode(int mode)
	{
		if(mode == MODE_INTEGER)
		{
			if(!getValue().isInteger())
				return false;
		}
		else if(mode == MODE_INVALID)
			throw new IllegalArgumentException();

		if(mode != mFractionInputMode)
		{
			mFractionInputMode = mode;

			String modeSelectorText = (mode == MODE_INTEGER) ? "¾" : "1¾";
			int modeSelectorVisibility = (mode == MODE_INTEGER) ? VISIBLE : GONE;

			mModeSwitcher.setText(modeSelectorText);
			mModeSwitcher.setVisibility(modeSelectorVisibility);

			setValue(getValue());
		}

		return true;
	}

	public int getFractionInputMode() {
		return mFractionInputMode;
	}

	public OnChangedListener getOnChangeListener() {
		return mListener;
	}

	public void setOnChangeListener(OnChangedListener listener) {
		mListener = listener;
	}

	@Override
	public void onChanged(NumberPicker picker, int oldVal, int newVal)
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

		if(mListener != null)
			mListener.onChanged(this, oldValue);

		/*if(mFractionInputMode != MODE_INTEGER)
		{
			if(Math.abs(getValue().doubleValue()) < 1)
				setFractionInputMode(MODE_FRACTION);
			else
				setFractionInputMode(MODE_MIXED);
		}*/
	}

	@Override
	public void onClick(View v)
	{
		if(v.getId() == R.id.mode_switcher)
		{
			if(mFractionInputMode == MODE_INTEGER)
				setFractionInputMode(MODE_FRACTION);
		}
	}

	private void updateView()
	{
		Log.d(TAG, "updateView: mFractionInputMode=" + mFractionInputMode);

		// hide in fraction mode
		mIntegerPicker.setVisibility(mFractionInputMode == MODE_FRACTION ? GONE : VISIBLE);
		// hide in integer mode
		mNumeratorPicker.setVisibility(mFractionInputMode == MODE_INTEGER ? GONE: VISIBLE);
		mDenominatorPicker.setVisibility(mFractionInputMode == MODE_INTEGER ? GONE: VISIBLE);
		mFractionBar.setVisibility(mFractionInputMode == MODE_INTEGER ? GONE: VISIBLE);

		mIntegerPicker.setCurrent(mInteger);
		mNumeratorPicker.setCurrent(mNumerator);
		mDenominatorPicker.setCurrent(mDenominator);
	}
}
