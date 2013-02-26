package at.jclehner.rxdroid.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;


/**
 * Smarter RingtonePreference with automatic summary and some sugar.
 * <p>
 * Apart from automatic summaries, this class handles
 * <code>"default"</code> and <code>"silent"</code> in
 * <code>android:defaultValue</code>.
 *
 * @author Joseph Lehner
 *
 */
public class SmartRingtonePreference extends RingtonePreference
{
	public SmartRingtonePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index)
	{
		final String defValue = (String) super.onGetDefaultValue(a, index);
		if("default".equals(defValue))
			return Settings.System.DEFAULT_NOTIFICATION_URI.toString();
		else if("silent".equals(defValue))
			return "";

		return defValue;
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValueObj)
	{
		super.onSetInitialValue(restorePersistedValue, defaultValueObj);

		final String value;
		if(restorePersistedValue)
			value = getPersistedString((String) defaultValueObj);
		else
			value = (String) defaultValueObj;

		setSummaryFromValue(value);
	}

	@Override
	protected boolean callChangeListener(Object newValue)
	{
		if(super.callChangeListener(newValue))
		{
			setSummaryFromValue((String) newValue);
			return true;
		}

		return false;
	}

	private void setSummaryFromValue(String value)
	{
		final Context c = getContext();
		final Ringtone r = RingtoneManager.getRingtone(c, Uri.parse(value));
		setSummary(r.getTitle(c));
	}
}
