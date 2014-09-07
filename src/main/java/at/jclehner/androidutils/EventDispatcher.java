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

package at.jclehner.androidutils;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

import android.util.Log;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

/**
 * Very simple event bus implementation.
 *
 * @author Joseph Lehner
 *
 * @param <T> the event handler type.
 */
public class EventDispatcher<T>
{
	private static final String TAG = EventDispatcher.class.getSimpleName();
	private static final boolean LOGV = false;

	private final WeakHashMap<Object, Void> mReceivers =
			new WeakHashMap<Object, Void>();

	public EventDispatcher() {}

	public synchronized void register(T eventHandler)
	{
		mReceivers.put(eventHandler, null);
		//if(LOGV) Log.v(TAG, "register: " + eventHandler.getClass() + " (" + mReceivers.size() + ")");
	}

	public synchronized void unregister(T eventHandler) {
		mReceivers.remove(eventHandler);
	}

	public void post(String eventName, Object... args) {
		post(eventName, Reflect.getTypes(args), args);
	}

	public synchronized void post(String eventName, Class<?>[] argTypes, Object... args)
	{
		if(LOGV) Log.v(TAG, "post: event=" + eventName + ": " + mReceivers.size() + " potential receivers");

		for(Object receiver : mReceivers.keySet())
		{
			final Method m = Reflect.getMethod(receiver.getClass(), eventName, argTypes);
			if(m == null)
			{
				Log.w(TAG, "  no such method: " + receiver.getClass().getSimpleName() + "." + eventName + Util.arrayToString(argTypes));
				continue;
			}

			try
			{
				Reflect.invokeMethod(m, receiver, args);
				if(LOGV) Log.v(TAG, "  found method " + m);
			}
			catch(WrappedCheckedException e)
			{
				Log.w(TAG, "Failed to dispatch event " + receiver.getClass().getSimpleName() + "." + m.getName(), e);
			}
		}
	}
}
