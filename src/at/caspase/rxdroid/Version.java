/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
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

import java.util.StringTokenizer;

import android.content.Context;

/**
 * Provides information about the current version.
 * 
 * @author Joseph Lehner
 *
 */
public final class Version
{
	@SuppressWarnings("unused")
	private static final String TAG = Version.class.getName();
	
	public static final boolean LOGV = true;
		
	/**
	 * Short format. Example: <code>1.2.3</code>
	 */
	public static final int FORMAT_SHORT = 0;
	/**
	 * Long format. Example: <code>1.2.3-r6666</code>
	 */
	public static final int FORMAT_LONG = 1;
	/**
	 * Full format. Example: <code>RxDroid 1.2.3-r6666</code>
	 */
	public static final int FORMAT_FULL = 2;
	
	private static String sVersion;
	private static String sRevision;
	private static String sAppName;
	
	/**
	 * Calls {@link #get(int)} with {@link #FORMAT_LONG}
	 */
	public static String get() {
		return get(FORMAT_LONG);
	}
	
	/**
	 * Returns the current version in the specified format.
	 */
	public static String get(int format)
	{
		init();
		
		switch(format)
		{
			case FORMAT_SHORT:
				return sVersion;
				
			case FORMAT_LONG:
				return get(FORMAT_SHORT) + "-r" + sRevision;
				
			case FORMAT_FULL:
				return sAppName + " " + get(FORMAT_LONG);
			
			default:
				throw new IllegalArgumentException();
		}			
	}
	
	public static String getRevision() {
		return sRevision;
	}
	
	private static synchronized void init()
	{
		if(sVersion == null)
		{
			final Context c = GlobalContext.get();
			sVersion = c.getString(R.string.version);
			sRevision = c.getString(R.string.vcs_revision);
			sAppName = c.getString(R.string.app_name);
			
			final StringTokenizer st = new StringTokenizer(sRevision);
			if(st.countTokens() != 3)
				return;
			
			st.nextToken(); // discard
			sRevision = st.nextToken();			
		}		
	}
	
	private Version() {}
}
