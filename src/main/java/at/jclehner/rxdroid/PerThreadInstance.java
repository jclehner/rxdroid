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

import java.util.HashMap;

import at.jclehner.androidutils.Reflect;

/**
 * Per-thread instance storage with caching.
 *
 * This class implements a storage for global per-thread instances, caching them
 * based on specified constructor arguments.
 *
 * Usage example:
 * <pre>
 * // If PerThreadInstance.get() was called with ctor args of "yyyy-MM-dd" before
 * // in the calling thread, this instance will be returned. Otherwise, a new
 * // object will be instantiated using new SimpleDateFormat("yyyy-MM-dd");
 *
 * SimpleDateFormat sdf = PerThreadInstance.get(SimpleDateFormat.class, "yyyy-MM-dd");
 * </pre>
 *
 * @author Joseph Lehner
 *
 */
public final class PerThreadInstance extends ThreadLocal<HashMap<Class<?>, HashMap<Object[], Object>>>
{
	// Reminder to self: premature optimization is the root of all evil!
	private static final boolean ENABLED = false;

	private static final PerThreadInstance DATA = ENABLED ? new PerThreadInstance() : null;

	@SuppressWarnings("unused")
	public static <T> T get(Class<? extends T> clazz, Object... ctorArgs)
	{
		if(!ENABLED)
			return Reflect.newInstance(clazz, ctorArgs);

		synchronized(PerThreadInstance.DATA)
		{
			HashMap<Object[], Object> classData = PerThreadInstance.DATA.get(clazz);
			if(classData == null)
			{
				classData = new HashMap<Object[], Object>();
				PerThreadInstance.DATA.get().put(clazz, classData);
			}

			@SuppressWarnings("unchecked")
			T value = (T) classData.get(ctorArgs);
			if(value == null)
			{
				value = Reflect.newInstance(clazz, ctorArgs);
				classData.put(ctorArgs, value);
			}

			return value;
		}
	}

	@Override
	protected HashMap<Class<?>, HashMap<Object[], Object>> initialValue() {
		return new HashMap<Class<?>, HashMap<Object[],Object>>();
	}

	private HashMap<Object[], Object> get(Class<?> clazz) {
		return get().get(clazz);
	}
}
