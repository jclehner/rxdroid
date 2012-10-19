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

import java.util.NoSuchElementException;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.SparseIntArray;
import at.caspase.rxdroid.util.Timer;

public final class Theme
{
	private static final String TAG = Theme.class.getName();
	private static final boolean LOGV = false;

	public static final int LIGHT = R.style.LightTheme;
	public static final int DARK = R.style.DarkTheme;

	private static final SparseIntArray sAttrCache = new SparseIntArray();
//	private static final ThreadLocal<TypedValue> sTypedValue = new ThreadLocal<TypedValue>() {
//
//		@Override
//		protected TypedValue initialValue()
//		{
//			return new TypedValue();
//		}
//	};

	public static boolean isDark() {
		return Settings.getBoolean(Settings.Keys.THEME_IS_DARK, Version.SDK_IS_PRE_HONEYCOMB);
	}

	public static int get() {
		return isDark() ? DARK : LIGHT;
	}

	public static int getResourceAttribute(int attr)
	{
		synchronized(sAttrCache)
		{
			if(sAttrCache.indexOfKey(attr) < 0)
			{
				final Timer t = LOGV ? new Timer() : null;

				final Context c = RxDroid.getContext();
				final int[] attrs = { attr };
				final TypedArray a = c.obtainStyledAttributes(get(), attrs);
				final int resId = a.getResourceId(0, 0);

				a.recycle();

				if(resId == 0)
					throw new NoSuchElementException();

				sAttrCache.put(attr, resId);

				if(LOGV) Log.v(TAG, "getResourceAttributes: " + t);
			}

			return sAttrCache.get(attr);
		}
	}

	public static void clearAttributeCache()
	{
		synchronized(sAttrCache) {
			sAttrCache.clear();
		}
	}

	private Theme() {}
}
