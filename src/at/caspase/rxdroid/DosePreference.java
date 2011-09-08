package at.caspase.rxdroid;

import java.util.Arrays;

import android.content.Context;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import at.caspase.rxdroid.Database.Drug;

public class DosePreference extends Preference implements OnClickListener
{
	private static final String TAG = DosePreference.class.getName();
	
	private int mDoseTime = -1;
	private Drug mDrug;
	private DoseView mDoseView;
	
	public DosePreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DosePreference(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
		setWidgetLayoutResource(R.layout.dose_preference);
	}
	
	public void setDrug(Drug drug) {
		mDrug = drug;
	}
	
	public Fraction getDose()
	{
		if(mDoseView == null)
			throw new IllegalStateException("mDoseView == null");
		
		return mDoseView.getDose();
	}

	@Override
	public void onClick(View view)
	{
		Log.d(TAG, "onPreferenceClick");
		
		TextView tv = new TextView(getContext());
		EditFraction.FractionPickerDialog dialog = new EditFraction.FractionPickerDialog(getContext(), tv);
		dialog.show();
	}
	
	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		
		if(view != null)
		{
			final String[] keys = { "morning", "noon", "evening", "night" };
			mDoseTime = Arrays.binarySearch(keys, 0, keys.length, getKey());
		
			if(mDoseTime < 0)
				throw new IllegalStateException("Illegal key for DosePreference. Valid keys: morning, noon, evening, night");
			
			mDoseView = (DoseView) view.findViewById(R.id.morning);
			if(mDoseView == null)
				return;
			
			mDoseView.setDoseTime(mDoseTime);
			mDoseView.setDrug(mDrug);
			mDoseView.setOnClickListener(this);
		}
	}
}
