package at.caspase.rxdroid;

import java.util.Arrays;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import at.caspase.rxdroid.Database.Drug;

public class DosePreference extends Preference
{
	private Drug mDrug;
	
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
	
	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		
		if(view != null)
		{
			final String[] keys = { "morning", "noon", "evening", "night" };
			final int doseTime = Arrays.binarySearch(keys, 0, keys.length, getKey());
		
			if(doseTime < 0)
				throw new IllegalStateException("Illegal key for DosePreference. Valid keys: morning, noon, evening, night");
			
			DoseView doseView = (DoseView) view.findViewById(R.id.morning);
			if(doseView == null)
				return;
			
			doseView.setDoseTime(doseTime);
			doseView.setDrug(mDrug);
		}
	}
}
