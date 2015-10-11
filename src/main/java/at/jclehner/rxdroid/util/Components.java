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

package at.jclehner.rxdroid.util;

import java.util.Locale;

import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.Window;

import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.LockscreenActivity;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.SystemEventReceiver;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.db.Database;

/**
 * Utility functions for Android application components.
 *
 * @author Joseph Lehner
 *
 */
public final class Components
{
	private static final String TAG = Components.class.getSimpleName();

	public static final int NO_DATABASE_INIT = 1;
	public static final int NO_SETTINGS_INIT = 1 << 1;
	public static final int NO_LOCKSCREEN = 1 << 2;
	public static final int NO_VISIBILITY_REGISTRY = 1 << 3;
	public static final int NO_THEME = 1 << 4;

	/**
	 * Initializes stuff that is common to all components (database, settings, ...).
	 */
	public static void onCreate(Context context, int flags)
	{
		if((flags & NO_DATABASE_INIT) == 0)
			Database.init();

		if((flags & NO_SETTINGS_INIT) == 0)
			Settings.init();
	}

	/**
	 * Initializes activity-specific stuff.
	 */
	public static void onCreateActivity(AppCompatActivity activity, int flags)
	{
		onCreate(activity.getApplicationContext(), flags);

		// XXX for now...
		flags |= NO_LOCKSCREEN | NO_VISIBILITY_REGISTRY;

		if((flags & NO_THEME) == 0)
			activity.setTheme(Theme.get());

		activity.requestWindowFeature(Window.FEATURE_ACTION_BAR);

		if(BuildConfig.DEBUG)
		{
			final String lang = Settings.getString(Keys.LANGUAGE);
			if(lang != null && lang.length() != 0)
			{
				final Resources res = activity.getResources();
				final Configuration cfg = res.getConfiguration();

				if(!lang.equals(cfg.locale.getLanguage()))
				{
					cfg.locale = new Locale(lang);
					res.updateConfiguration(cfg, res.getDisplayMetrics());
					Log.i(TAG, "Setting language to '" + lang + "'");
				}
				else
					Log.d(TAG, "Language '" + lang + "' already set");
			}
		}

		SystemEventReceiver.registerOnSystemTimeChangeListener(activity);
	}

	public static void onResumeActivity(AppCompatActivity activity, int flags)
	{
		if((flags & NO_LOCKSCREEN) == 0)
			LockscreenActivity.startMaybe(activity);

		if((flags & NO_VISIBILITY_REGISTRY) == 0)
			RxDroid.setIsVisible(activity, true);

		Settings.maybeLockInPortraitMode(activity);
	}

	public static void onPauseActivity(AppCompatActivity activity, int flags) {
		RxDroid.setIsVisible(activity, false);
	}

	private Components() {}
}
