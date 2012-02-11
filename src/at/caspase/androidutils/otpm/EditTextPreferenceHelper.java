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
	public boolean updatePreference(EditTextPreference preference, Object fieldValue) {
		setFieldValue((String) fieldValue);
		return true;
	}

	@Override
	public boolean isDisabled() {
		return false;
	}
}