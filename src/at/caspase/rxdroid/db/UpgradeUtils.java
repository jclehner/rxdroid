/**
 * Copyright (C) 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.caspase.rxdroid.db;

import java.lang.reflect.Field;

import android.util.Log;
import at.caspase.rxdroid.util.Reflect;

import com.j256.ormlite.field.DatabaseField;

public final class UpgradeUtils
{
	private static final String TAG = UpgradeUtils.class.getName();
	private static final boolean LOGV = true;

	public static void copyDrug(Drug dest, Entry src)
	{
		Class<?> clsD = dest.getClass();
		Class<?> clsS = src.getClass();

		if(LOGV) Log.d(TAG, "copyDrug:");

		for(Field fS : clsS.getDeclaredFields())
		{
			if(fS.isAnnotationPresent(DatabaseField.class))
			{
				Field fD = Reflect.getDeclaredField(clsD, fS.getName());
				if(fD != null)
				{
					try
					{
						fD.setAccessible(true);
						fS.setAccessible(true);
						fD.set(dest, fS.get(src));
						if(LOGV) Log.d(TAG, "  " + fS.getName());
					}
					catch(IllegalArgumentException e)
					{
						//Log.w(TAG, e);
					}
					catch(IllegalAccessException e)
					{
						//Log.w(TAG, e);
					}
				}
			}
		}
	}

	private UpgradeUtils() {}
}
