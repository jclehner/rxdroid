package at.caspase.androidutils.otpm;

import android.preference.Preference;

public class ReadonlyPreferenceHelper extends PreferenceHelper<Preference, Object>
{
	@Override
	public void initPreference(Preference preference, Object fieldValue) {
		preference.setEnabled(false);
	}

	@Override
	public void updateSummary(Preference preference, Object newValue) {
		preference.setSummary(newValue == null ? "(null)" : newValue.toString());
	}

	@Override
	public void onDependencyChange(Preference preference, String depKey) {
		updateSummary(preference, getFieldValue());
	}
}
