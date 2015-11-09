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

package at.jclehner.rxdroid;

import java.util.StringTokenizer;

import android.content.Context;
import android.os.Build;

/**
 * Provides information about the current version.
 *
 * @author Joseph Lehner
 *
 */
public final class Version
{
	public static final boolean SDK_IS_JELLYBEAN_OR_NEWER = Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
	public static final boolean SDK_IS_LOLLIPOP_OR_NEWER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

	private static final boolean BETA_BUILD = false;

	@SuppressWarnings("unused")
	public static final boolean BETA = BuildConfig.DEBUG || BETA_BUILD;

	public static final String[] LANGUAGES = {
		"en", "de", "it", "el"
	};

	/**
	 * Short format. Example: <code>1.2.3</code>
	 */
	public static final int FORMAT_SHORT = 0;
	/*
	 * Long format. Example: <code>1.2.3-r6666</code>
	 */
	//public static final int FORMAT_LONG = 1;
	/**
	 * Full format. Example: <code>RxDroid 1.2.3-r6666</code>
	 */
	public static final int FORMAT_FULL = 2;

	private static String sVersion;
	//private static String sRevision;
	private static String sAppName;

	/**
	 * Calls {@link #get(int)} with {@link #FORMAT_FULL}
	 */
	public static String get() {
		return get(FORMAT_FULL);
	}

	/**
	 * Returns the current version in the specified format.
	 */
	public static String get(int format)
	{
		init();

		switch(format)
		{
			case FORMAT_FULL:
			case FORMAT_SHORT:
				return sAppName + " " + sVersion;

			//case FORMAT_FULL:
			//	return get(FORMAT_SHORT) + "-r" + sRevision;


			default:
				throw new IllegalArgumentException();
		}
	}

	public static int versionCodeBeta(int minor, int patch)
	{
		if(minor > 99)
			throw new IllegalArgumentException("minor > 99: " + minor);
		else if(patch > 9)
			throw new IllegalArgumentException("patch > 9: " + patch);

		//  9210 = 0.9.21
		//  9211 = 0.9.21.1
		// 10000 = 1.0.0
		// 10101 = 1.1.1
		// 11003 = 1.10.3
		//
		//

		return 9000 + minor * 10 + patch;
	}

	private static synchronized void init()
	{
		if(sVersion == null)
		{
			sVersion = RxDroid.getPackageInfo().versionName;
			sAppName = RxDroid.getContext().getString(R.string.app_name);
		}
	}

	private Version() {}
}
