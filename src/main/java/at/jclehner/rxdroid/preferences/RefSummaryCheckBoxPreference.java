package at.jclehner.rxdroid.preferences;


import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import at.jclehner.androidutils.RefString;

public class RefSummaryCheckBoxPreference extends CheckBoxPreference
{
	public RefSummaryCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public RefSummaryCheckBoxPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RefSummaryCheckBoxPreference(Context context) {
		super(context);
	}

	@Override
	public void setSummary(CharSequence summary) {
		super.setSummary(RefString.resolve(getContext(), summary));
	}

	@Override
	protected void onAttachedToActivity()
	{
		super.onAttachedToActivity();
		setSummary(getSummary());
	}
}
