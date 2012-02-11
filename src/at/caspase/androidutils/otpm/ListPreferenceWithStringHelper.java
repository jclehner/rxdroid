package at.caspase.androidutils.otpm;

import android.content.Context;
import android.content.res.Resources;
import android.preference.ListPreference;

public abstract class ListPreferenceWithStringHelper extends PreferenceHelper<ListPreference, String>
{
	private final String[] mEntries;
	private final String[] mValues;

	public ListPreferenceWithStringHelper() {
		throw new UnsupportedOperationException("You must extend this class to use it");
	}

	public ListPreferenceWithStringHelper(Context context, int entriesResId, int valuesResId)
	{
		Resources r = context.getResources();
		mEntries = r.getStringArray(entriesResId);
		mValues = r.getStringArray(valuesResId);
	}

	public ListPreferenceWithStringHelper(String[] entries, String[] values)
	{
		mEntries = entries;
		mValues = values;
	}

	@Override
	public void initPreference(ListPreference preference, String fieldValue)
	{
		preference.setEntries(mEntries);
		preference.setEntryValues(mValues);
		preference.setValue(fieldValue);
	}

	@Override
	public boolean updatePreference(ListPreference preference, Object newPrefValue)
	{
		setFieldValue((String) newPrefValue);
		return true;
	}
}