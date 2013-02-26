/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

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
