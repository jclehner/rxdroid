
package at.caspase.rxdroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FractionPreference extends DialogPreference implements OnClickListener, OnLongClickListener, TextWatcher
{
	private static final String TAG = FractionPreference.class.getName();
	private static final String PREFKEY_MODE = "_fraction_preference_is_in_mixed_number_mode";
	
	private EditText mInputNumber;
	private EditText mInputNumerator;
	private EditText mInputDenominator;
	
	private Button mButtonPlus;
	private Button mButtonMinus;

	private Button mModeToggler;
	private boolean mIsInMixedNumberMode;

	// the value currently represented in our dialog
	private Fraction mDialogValue = Fraction.ZERO;
	// the actual value, copied from mDialogValue if the user presses
	// the "set" button in the dialog.
	private Fraction mValue = mDialogValue;
	
	private Fraction mLongClickSummand;
	private boolean mAllowNegativeValues;
	
	private String mLastInput;
	private boolean mIgnoreTextWatcherEvents = false;
	
	private SharedPreferences mSharedPrefs;

	public FractionPreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FractionPreference(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
		
		setDialogIcon(android.R.drawable.ic_dialog_dialer);
		
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mIsInMixedNumberMode = mSharedPrefs.getBoolean(PREFKEY_MODE, false);
	}
	
	public void setValue(Fraction value) {
		mDialogValue = mValue = value;
	}
	
	public Fraction getValue() {
		return new Fraction(mValue);
	}
	
	public void setAllowNegativeValues(boolean allowNegativeValues) {
		mAllowNegativeValues = allowNegativeValues;
	}
	
	public boolean getAllowNegativeValues() {
		return mAllowNegativeValues;
	}
	
	/**
	 * Sets a value that is added/subtracted from the current value when
	 * long-clicking the +/- buttons.
	 * 
	 * @param value a fraction. Use <code>null</code> to disable the long-click behaviour.
	 */	
	public void setOnLongClickSummand(Fraction summand) 
	{
		OnLongClickListener listener = null;
		
		if((mLongClickSummand = summand) != null)
			listener = this;
		
		mButtonPlus.setOnLongClickListener(listener);
		mButtonMinus.setOnLongClickListener(listener);		
	}
	
	@Override
	public CharSequence getSummary()
	{
		return mValue.toString();
	}

	@Override
	public void onClick(View view)
	{
		if(view.getId() == R.id.btn_mode_toggle)
		{
			mIsInMixedNumberMode = !mIsInMixedNumberMode;
			mInputNumber.setVisibility(mIsInMixedNumberMode ? View.VISIBLE : View.GONE);
			
			int inputType;

			if (mIsInMixedNumberMode)
				inputType = InputType.TYPE_CLASS_NUMBER;
			else
				inputType = InputType.TYPE_NUMBER_FLAG_SIGNED;

			mInputNumerator.setRawInputType(inputType);
			onDialogValueChanged();
			
		}
		else if(view.getId() == R.id.btn_plus)
		{
			mDialogValue = mDialogValue.plus(1);
			onDialogValueChanged();
		}
		else if(view.getId() == R.id.btn_minus)
		{
			setDialogValue(mDialogValue.minus(1));
			onDialogValueChanged();
		}
	}
	
	@Override
	public boolean onLongClick(View view)
	{
		if(mLongClickSummand != null)
		{
			if(view.getId() == R.id.btn_plus)
			{
				mDialogValue = mDialogValue.plus(mLongClickSummand);
				onDialogValueChanged();
				return true;
			}
			else if(view.getId() == R.id.btn_minus)
			{
				setDialogValue(mDialogValue.minus(mLongClickSummand));
				onDialogValueChanged();
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		if(mIgnoreTextWatcherEvents)
			return;
		
		int wholeNum, numerator, denominator;
		
		try
		{
			wholeNum = Integer.parseInt(mInputNumber.getText().toString(), 10);
			numerator = Integer.parseInt(mInputNumerator.getText().toString(), 10);
			denominator = Integer.parseInt(mInputDenominator.getText().toString(), 10);
		}
		catch(NumberFormatException e)
		{
			Log.d(TAG, "afterTextChanged: failed to parse input");
			return;
		}
				
		if(denominator == 0)
		{
			Toast.makeText(getContext(), "Denominator must not be zero!", Toast.LENGTH_SHORT).show();
			if(mLastInput == null || (denominator = Integer.parseInt(mLastInput, 10)) == 0)
			{
				Log.e(TAG, "afterTextChanged: last input was still zero");
				denominator = 1;
			}
			mInputDenominator.setText(Integer.toString(denominator));
		}
		
		mLastInput = null;
				
		try
		{
			mDialogValue = new Fraction(wholeNum, numerator, denominator);
		}
		catch(IllegalArgumentException e)
		{
			Log.d(TAG, "afterTextChanged: ignoring { " + wholeNum + ", " + numerator  + ", " + denominator + " }");
			
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) 
	{
		if(!mIgnoreTextWatcherEvents)
			mLastInput = s.toString();
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}
	
	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
	{
		super.onPrepareDialogBuilder(builder);
		
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				if(which == DialogInterface.BUTTON_POSITIVE)
				{
					mValue = mDialogValue;
					persistFractionInputMode();
					setSummary(mValue.toString());
					notifyChanged();
				}
			}
		};
		
		builder.setPositiveButton("Set", listener);
		builder.setNegativeButton("Cancel", null);
	}

	@Override
	protected View onCreateDialogView()
	{
		LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = inflater.inflate(R.layout.fraction_preference, null);

		mInputNumber = (EditText) view.findViewById(R.id.number);
		mInputNumber.addTextChangedListener(this);
		mInputNumber.setVisibility(mIsInMixedNumberMode ? View.VISIBLE : View.GONE);
		
		mInputNumerator = (EditText) view.findViewById(R.id.numerator);
		mInputNumerator.addTextChangedListener(this);
		
		mInputDenominator = (EditText) view.findViewById(R.id.denominator);
		mInputDenominator.addTextChangedListener(this);		
		
		mButtonPlus = (Button) view.findViewById(R.id.btn_plus);
		mButtonPlus.setOnClickListener(this);
		
		mButtonMinus = (Button) view.findViewById(R.id.btn_minus);
		mButtonMinus.setOnClickListener(this);

		mModeToggler = (Button) view.findViewById(R.id.btn_mode_toggle);
		mModeToggler.setOnClickListener(this);

		onDialogValueChanged();
		
		mLastInput = mInputDenominator.getText().toString();
		
		return view;
	}
	
	private void setDialogValue(Fraction value)
	{
		if(!mAllowNegativeValues && value.compareTo(0) == -1)
			Log.d(TAG, "setDialogValue: ignoring negative value");
		else
			mDialogValue = value;		
	}

	private void onDialogValueChanged()
	{
		// While updating the EditTexts, the TextWatcher events are handled
		// after every call to setText(), possibly resulting in a state where
		// combining the current EditText values obtained by getText() results
		// in an invalid fraction. To prevent this from happening, any TextWatcher
		// events in this class should be ignored while mIgnoreTextWatcherEvents is
		// set to true.
		mIgnoreTextWatcherEvents = true;
		
		int[] fractionData = mDialogValue.getFractionData(mIsInMixedNumberMode);

		if (mIsInMixedNumberMode)
			mInputNumber.setText(Integer.toString(fractionData[0]));
		else
			mInputNumber.setText("0");

		mInputNumerator.setText(Integer.toString(fractionData[1]));
		mInputDenominator.setText(Integer.toString(fractionData[2]));
		
		mIgnoreTextWatcherEvents = false;
		
		if(!mAllowNegativeValues && mLongClickSummand == null && mDialogValue.minus(1).compareTo(0) == -1)
			mButtonMinus.setEnabled(false);
		else
			mButtonMinus.setEnabled(true);
	}
	
	private void persistFractionInputMode()
	{
		Editor editor = mSharedPrefs.edit();
		editor.putBoolean(PREFKEY_MODE, mIsInMixedNumberMode);
		editor.commit();
	}
}
