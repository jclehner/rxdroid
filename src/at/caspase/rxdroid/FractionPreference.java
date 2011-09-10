
package at.caspase.rxdroid;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.util.Log;
import at.caspase.rxdroid.FractionInputDialog.OnFractionSetListener;

public class FractionPreference extends Preference implements OnPreferenceClickListener, OnFractionSetListener
{
	private static final String TAG = FractionPreference.class.getName();
	
	private Fraction mValue;
	
	public FractionPreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public FractionPreference(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
		
		mValue = Fraction.decode(getPersistedString("0"));
		setOnPreferenceClickListener(this);
	}	
	
	public Fraction getValue() {
		return mValue;
	}
	
	@Override
	public CharSequence getSummary() {
		return mValue.toString();
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		FractionInputDialog dialog = new FractionInputDialog(getContext(), mValue, this);
		dialog.show();
		return true;
	}

	@Override
	public void onFractionSet(FractionInputDialog dialog, Fraction value)
	{
		if(shouldPersist())
			persistString(value.toString());
		
		mValue = dialog.getValue();		
		setSummary(mValue.toString());
		notifyChanged();
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) 
	{
		String value = a.getString(index);
		Log.d(TAG, "onGetDefaultValue: value=" + value);
		return value != null ? value : "0"; 
	}
}
