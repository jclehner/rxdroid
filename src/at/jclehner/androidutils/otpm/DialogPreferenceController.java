package at.jclehner.androidutils.otpm;

import android.preference.DialogPreference;

public abstract class DialogPreferenceController<P extends DialogPreference, T> extends PreferenceController<P, T>
{
	public void initPreference(P preference, T fieldValue)
	{
		if(hideWidget())
			preference.setWidgetLayoutResource(0);
	}

	protected boolean hideWidget() {
		return true;
	}
}
