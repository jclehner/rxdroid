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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import android.os.Bundle;

public class Extras
{
	private static Map<Object, Bundle> sExtras = Collections.synchronizedMap(new WeakHashMap<Object, Bundle>());

	public static Bundle get(Object object)
	{
		if(!isRegistered(object))
			sExtras.put(object, new Bundle());

		return sExtras.get(object);
	}

	public static void remove(Object object) {
		sExtras.remove(object);
	}

	public static boolean isRegistered(Object object) {
		return sExtras.containsKey(object);
	}

	public static boolean containsKey(Object object, String extraKey)
	{
		if(!isRegistered(object))
			return false;

		return sExtras.get(object).containsKey(extraKey);
	}
}
