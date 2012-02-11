package at.caspase.androidutils.otpm;

import android.content.Context;
import android.preference.ListPreference;
import at.caspase.rxdroid.util.Util;

public class ListPreferenceWithIntHelper extends PreferenceHelper<ListPreference, Integer>
{
	private final String[] mEntries;

	public ListPreferenceWithIntHelper() {
		throw new UnsupportedOperationException("You must extend this class to use it");
	}

	public ListPreferenceWithIntHelper(Context context, int entriesResId) {
		this(context.getResources().getStringArray(entriesResId));
	}

	public ListPreferenceWithIntHelper(String[] entries) {
		mEntries = entries;
	}

	@Override
	public void initPreference(ListPreference preference, Integer fieldValue)
	{
		preference.setEntries(mEntries);
		Util.populateListPreferenceEntryValues(preference);
		preference.setValueIndex(fieldValue);
		preference.setSummary(mEntries[fieldValue]);
	}

	@Override
	public boolean updatePreference(ListPreference preference, Object newPrefValue)
	{
		Integer index = Integer.valueOf((String) newPrefValue, 10);
		setFieldValue(index);
		preference.setSummary(mEntries[index]);
		return true;
	}
}