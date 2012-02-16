package at.caspase.androidutils.otpm;

import android.preference.EditTextPreference;
import android.widget.EditText;

public class EditTextPreferenceHelper extends PreferenceHelper<EditTextPreference, String>
{
	@Override
	public void initPreference(EditTextPreference pref, String value)
	{
		pref.setText(value);
		EditText editText = pref.getEditText();
		editText.setHint(pref.getTitle());
	}

	@Override
	public boolean updatePreference(EditTextPreference preference, Object fieldValue)
	{
		String newText = (String) fieldValue;
		preference.setSummary(newText);
		setFieldValue(newText);
		return true;
	}

	@Override
	public boolean isPreferenceDisabled() {
		return false;
	}
}