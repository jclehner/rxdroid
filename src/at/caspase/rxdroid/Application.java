/**
 * Copyright (C) 2011, 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
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

package at.caspase.rxdroid;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;


public class Application extends android.app.Application
{
	private static WeakHashMap<Activity, Boolean> sActivityVisibility =
			new WeakHashMap<Activity, Boolean>();

	private static long sUnlockedTime = 0;
	private static WeakReference<Context> sContextRef;

	@Override
	public void onCreate()
	{
		sContextRef = new WeakReference<Context>(getApplicationContext());

		Settings.init();
		AutoIntakeCreator.registerSelf();

		super.onCreate();
	}

	/**
	 * Calls {@link GlobalContext#get(boolean)} with <code>allowNullContext=false</code>.
	 */
	public static Context getContext() {
		return getContext(false);
	}

	public static Context getContext(boolean allowNullContext)
	{
		final Context c = sContextRef.get();
		if(c == null && !allowNullContext)
			throw new IllegalStateException("Context is null");
		return c;
	}

	public static void doStartActivity(Intent intent) {
		getContext().startActivity(intent);
	}

	public static boolean isLocked()
	{
		final int timeoutSeconds = Settings.getInt(Settings.Keys.LOCKSCREEN_TIMEOUT, 0);
		if(timeoutSeconds == 0)
			return sUnlockedTime == 0;

		final long now = System.currentTimeMillis();
		if(sUnlockedTime > now)
			return true;

		final long diffSeconds = (now - sUnlockedTime) / 1000;
		return diffSeconds >= timeoutSeconds;
	}

	public static void unlock() {
		sUnlockedTime = System.currentTimeMillis();
	}

	public static void setIsVisible(Activity activity, boolean isVisible) {
		sActivityVisibility.put(activity, isVisible);
	}

	public static boolean isUiVisible()
	{
		for(Activity activity : sActivityVisibility.keySet())
		{
			if(sActivityVisibility.get(activity))
				return true;
		}

		return false;
	}
}
