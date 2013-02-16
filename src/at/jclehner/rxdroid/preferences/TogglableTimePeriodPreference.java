package at.jclehner.rxdroid.preferences;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Settings;

public class TogglableTimePeriodPreference extends TimePeriodPreference
{
	private static final String TAG = TimePeriodPreference.class.getName();

	private CompoundButton mToggler;
	private boolean mChecked = true;

	public TogglableTimePeriodPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setWidgetLayoutResource(R.layout.toggler);
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		mToggler = (CompoundButton) view.findViewById(android.R.id.checkbox);
		mToggler.setChecked(mChecked);
		mToggler.setOnCheckedChangeListener(mToggleListener);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
	{
		super.onSetInitialValue(restorePersistedValue, defaultValue);
		mChecked = Settings.isChecked(getKey(), false);
	}

	private final OnCheckedChangeListener mToggleListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			mChecked = isChecked;
			Settings.setChecked(getKey(), mChecked);

			if(mChecked)
				showDialog(null);
		}
	};
}
