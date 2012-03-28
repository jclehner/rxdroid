package at.caspase.androidutils.otpm;

import android.preference.EditTextPreference;
import android.text.InputType;
import android.widget.EditText;

public class NumericEditTextPreferenceHelper extends PreferenceHelper<EditTextPreference, Integer>
{
	@Override
	public void initPreference(EditTextPreference preference, Integer fieldValue)
	{
		preference.setText(fieldValue.toString());
		preference.setSummary(fieldValue.toString());

		final EditText editText = preference.getEditText();
		editText.setMaxLines(1);
		editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		editText.setSelectAllOnFocus(true);
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
