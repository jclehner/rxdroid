package at.caspase.androidutils.otpm;

import at.caspase.androidutils.MyDialogPreference;

public class MyDialogPreferenceHelper extends PreferenceHelper<MyDialogPreference, Object>
{
	@Override
	public void initPreference(MyDialogPreference preference, Object fieldValue)
	{
		preference.setValue(fieldValue);
		//preference.setSummary(fieldValue.toString());
	}

	@Override
	public boolean updatePreference(MyDialogPreference preference, Object newPrefValue)
	{
		setFieldValue(newPrefValue);
		return true;
	}

}
