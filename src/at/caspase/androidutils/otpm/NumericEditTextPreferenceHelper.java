package at.caspase.androidutils.otpm;

import android.preference.EditTextPreference;

public class NumericEditTextPreferenceHelper extends PreferenceHelper<EditTextPreference, Integer>
{
	@Override
	public void initPreference(EditTextPreference preference, Integer fieldValue)
	{
		preference.setText(fieldValue.toString());
		preference.setSummary(fieldValue.toString());
	}

	@Override
	public boolean updatePreference(EditTextPreference preference, Object newValue)
	{
		final String valueStr = (String) newValue;
		preference.setText(valueStr);
		preference.setSummary(valueStr);
		setFieldValue(Integer.parseInt(valueStr, 10));
		return true;
	}
}
