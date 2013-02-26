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
	private static final String TAG = TimePeriodPreference.class.getSimpleName();
	private static final TimePeriod EMPTY = TimePeriod.fromString("00:00-00:00");

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
		setChecked(Settings.isChecked(getKey(), false));
		//onValueSet(getValue());
	}

	@Override
	protected String toSummaryString(TimePeriod value)
	{
		if(value == EMPTY)
			return null;

		return super.toSummaryString(value);
	}

	private void setChecked(boolean checked)
	{
		mChecked = checked;
		if(!mChecked)
			setSummaryInternal(getContext().getString(R.string._title_disabled));

		Settings.setChecked(getKey(), mChecked);
	}

	private final OnCheckedChangeListener mToggleListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			setChecked(isChecked);

			if(mChecked)
				showDialog(null);
		}
	};
}
