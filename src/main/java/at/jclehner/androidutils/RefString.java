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


import android.content.Context;
import android.content.res.Resources;

public class RefString
{
	public static String resolve(Context context, CharSequence text)
	{
		final StringBuilder sb = new StringBuilder(text);
		final String appPackage = context.getApplicationContext().getPackageName();

		resolvePrefix(context, sb, "string", appPackage);
		resolvePrefix(context, sb, "android:string", "android");

		return sb.toString();
	}

	public static String resolve(Context context, int textResId) {
		return resolve(context, context.getString(textResId));
	}

	public static String resolve(Context context, int textResId, Object... formatArgs) {
		return resolve(context, context.getString(textResId, formatArgs));
	}

	private static final void resolvePrefix(Context context, StringBuilder sb, String prefix, String defPackage)
	{
		final Resources res = context.getResources();

		int beg;

		while((beg = sb.indexOf("[@" + prefix + "/")) != -1)
		{
			int end = sb.indexOf("]", beg);
			String name = sb.substring(beg + 2, end);
			int resId = res.getIdentifier(name, null, defPackage);
			if(resId == 0)
				throw new IllegalArgumentException("No @" + name + " in package " + defPackage);

			sb.replace(beg, end + 1, context.getString(resId));
		}
	}

	private RefString() {}
}
