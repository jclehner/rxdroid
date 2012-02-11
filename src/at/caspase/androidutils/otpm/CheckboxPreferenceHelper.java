package at.caspase.androidutils.otpm;

import android.preference.CheckBoxPreference;

public class CheckboxPreferenceHelper extends
		PreferenceHelper<android.preference.CheckBoxPreference, Boolean>
{
	@Override
	public void initPreference(CheckBoxPreference preference, Boolean fieldValue)
	{
		preference.setChecked(fieldValue);
		preference.setSummary(getSummary());
	}
}
