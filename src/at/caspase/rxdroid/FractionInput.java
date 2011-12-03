package at.caspase.rxdroid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.quietlycoding.android.picker.NumberPicker;

/**
 * A widget for fraction input.
 * 
 * This implementation basically uses the private NumberPicker implementation 
 * available in the pre-Honeycomb sources.
 * 
 * @author Joseph Lehner
 */
public class FractionInput extends LinearLayout implements NumberPicker.OnChangedListener
{
	public interface OnChangedListener
	{
		public void onChanged(FractionInput widget, Fraction oldValue);
	}	
	
	private static final int MAX = 99999;
	
	private NumberPicker mIntegerPicker;
	private NumberPicker mNumeratorPicker;
	private NumberPicker mDenominatorPicker;
	
	private int mInteger = 0;
	private int mNumerator = 0;
	private int mDenominator = 1;
	
	private boolean mUseMixedNumberMode = false;
	
	private OnChangedListener mListener;
	
	public FractionInput(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		LayoutInflater lf = LayoutInflater.from(context);
		lf.inflate(R.layout.fraction_input2, this, true);
		
		mIntegerPicker = (NumberPicker) findViewById(R.id.integer);
		mNumeratorPicker = (NumberPicker) findViewById(R.id.numerator);
		mDenominatorPicker = (NumberPicker) findViewById(R.id.denominator);
		
		mIntegerPicker.setOnChangeListener(this);
		mNumeratorPicker.setOnChangeListener(this);
		mDenominatorPicker.setOnChangeListener(this);
		
		mIntegerPicker.setRange(0, MAX);
		mIntegerPicker.setWrap(false);
		
		mNumeratorPicker.setRange(0, MAX);
		mNumeratorPicker.setWrap(false);
		
		mDenominatorPicker.setRange(1, MAX);
		mDenominatorPicker.setWrap(false);
						
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_HORIZONTAL);
		
		updateView();
	}
	
	public void setValue(Fraction value)
	{
		int data[] = value.getFractionData(mUseMixedNumberMode);
		
		mInteger = mUseMixedNumberMode ? data[0] : 0;
		mNumerator = data[1];
		mDenominator = data[2];
		
		updateView();		
	}
	
	public Fraction getValue() {
		return new Fraction(mInteger, mNumerator, mDenominator);
	}
	
	public void setMixedNumberMode(boolean useMixedNumberMode)
	{
		mUseMixedNumberMode = useMixedNumberMode;
		// ugly, but this updates the fraction's members
		setValue(getValue());
		updateView();		
	}
	
	public boolean isInMixedNumberMode() {
		return mUseMixedNumberMode;
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
		if(picker.getId() == R.id.numerator)
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
	}
	
	private void updateView()
	{
		mIntegerPicker.setVisibility(mUseMixedNumberMode ? VISIBLE : GONE);
		mIntegerPicker.setCurrent(mInteger);
		
		mNumeratorPicker.setCurrent(mNumerator);
		mDenominatorPicker.setCurrent(mDenominator);
	}
}
