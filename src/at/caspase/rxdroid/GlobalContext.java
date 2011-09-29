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

import android.content.Context;

/**
 * Provides a globally available context.
 * 
 * @author Joseph Lehner
 */
public final class GlobalContext
{
	static Context sContext;
	
	/**
	 * Set the globally available context.
	 *
	 * @param context a context to obtain the application context from.
	 */
	public static synchronized void set(Context context)
	{
		if(sContext == null)
			sContext = context.getApplicationContext();
	}
	
	/**
	 * Calls {@link #get(boolean)} with <code>allowNullContext=false</code>.
	 */
	public static synchronized Context get() {
		return get(false);
	}
	
	/**
	 * Gets the stored context.
	 * 
	 * @param allowNullContext If set to <code>true</code>, the function will <em>not</em> throw
	 * 	an exception if the currently stored context is <code>null</code> 
	 * @return a reference to the stored context
	 */
	public static synchronized Context get(boolean allowNullContext)
	{
		if(sContext == null)
		{
			if(!allowNullContext)
				throw new IllegalStateException("Context is null");
		}
				
		return sContext;
	}	
	
	private GlobalContext() {}
}
