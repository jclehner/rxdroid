/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

import java.util.Locale;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.Version;
import at.jclehner.rxdroid.util.Util;

public class LanguagePreference extends ListPreference
{
	private static final String TAG = LanguagePreference.class.getSimpleName();

	public LanguagePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		final int languageCount = Version.LANGUAGES.length;

		final String[] entries = new String[languageCount + 1];
		entries[0] = context.getString(R.string._title_default);

		for(int i = 1; i != entries.length; ++i)
		{
			//final Locale loc = new Locale(Version.LANGUAGES[i-1]);
			entries[i] = getLocaleName(Version.LANGUAGES[i-1]);
		}

		final String[] values = new String[entries.length];
		values[0] = "";
		System.arraycopy(Version.LANGUAGES, 0, values, 1, languageCount);

		setEntries(entries);
		setEntryValues(values);
	}

	public static void setLanguage(String language)
	{
		final Resources res = RxDroid.getContext().getResources();
		final Configuration cfg = res.getConfiguration();

		if(!cfg.locale.getLanguage().equals(language))
		{
			Log.i(TAG, "Setting language to \"" + language + "\"");
			cfg.locale = new Locale(language);
			res.updateConfiguration(cfg, res.getDisplayMetrics());
			Locale.setDefault(cfg.locale);
		}
		else
			Log.i(TAG, "Ignoring language change to " + language + " - already set!");
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
	{
		super.onSetInitialValue(restoreValue, defaultValue);
		setSummaryFromValue(getValue());
	}

	@Override
	protected boolean callChangeListener(Object newValue)
	{
		if(super.callChangeListener(newValue))
		{
			final String lang = (String) newValue;

			setSummaryFromValue(lang);
			setLanguage(lang);

			//RxDroid.toastLong(R.string._toast_restart_app);
			return true;
		}

		return false;
	}

	private void setSummaryFromValue(String value)
	{
		final String summary;

		if(!TextUtils.isEmpty(value))
			summary = getLocaleName(value);
		else
			summary = getContext().getString(R.string._title_default);

		setSummary(summary);
	}

	private static String getLocaleName(String lang)
	{
		final Locale loc = new Locale(lang);
		return Util.capitalize(loc.getDisplayName(loc));
	}
}
