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

package at.jclehner.rxdroid.util;

import java.io.Closeable;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Scanner;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.Spannable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TimePicker;
import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.DumbTime;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Schedule;

public final class Util
{
	private static final String TAG = Util.class.getName();

	public static void detachFromParent(View v)
	{
		if(v == null)
			return;

		ViewParent parent = v.getParent();
		if(parent != null)
		{
			if(parent instanceof ViewGroup)
				((ViewGroup) parent).removeView(v);
			else
				Log.w(TAG, "detachFromParent: parent is not a ViewGroup");
		}
	}

	public static void lockActivityOrientation(Activity activity, int orientation)
	{
		activity.setRequestedOrientation(orientation);
		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	}

//	public static <T extends Serializable> T copy(T object)
//	{
//
//	}

	public static void applyStyle(Spannable spannable, Object style) {
		spannable.setSpan(style, 0, spannable.length(), 0);
	}

	public static int pixelsFromSips(int sips) {
		return Math.round(sips * getDisplayMetrics().scaledDensity);
	}

	public static int pixelsFromDips(int dips) {
		return Math.round(dips * getDisplayMetrics().density);
	}

	public static int getDoseTimeDrawableFromDoseViewId(int doseViewId)
	{
		switch(doseViewId)
		{
				case R.id.morning:
						return R.drawable.ic_morning;
				case R.id.noon:
						return R.drawable.ic_noon;
				case R.id.evening:
						return R.drawable.ic_evening;
				case R.id.night:
						return R.drawable.ic_night;
		}

		throw new IllegalArgumentException();
	}

	public static int getDoseTimeDrawableFromDoseTime(int doseTime)
	{
		return getDoseTimeDrawableFromDoseViewId(Constants.getDoseViewId(doseTime));
	}

	public static int getDoseTimeFromDoseViewId(int doseViewId)
	{
		switch(doseViewId)
		{
				case R.id.morning:
						return Drug.TIME_MORNING;
				case R.id.noon:
						return Drug.TIME_NOON;
				case R.id.evening:
						return Drug.TIME_EVENING;
				case R.id.night:
						return Drug.TIME_NIGHT;
		}

		throw new IllegalArgumentException();
	}

	public static String getDoseTimeName(int doseTime)
	{
		final int resId;

		switch(doseTime)
		{
			case Schedule.TIME_MORNING:
				resId = R.string._title_morning;
				break;

			case Schedule.TIME_NOON:
				resId = R.string._title_noon;
				break;

			case Schedule.TIME_EVENING:
				resId = R.string._title_evening;
				break;

			case Schedule.TIME_NIGHT:
				resId = R.string._title_night;
				break;

			default:
				throw new IllegalArgumentException();
		}

		return RxDroid.getContext().getString(resId);
	}

	public static int getDrugIconDrawable(Context context, int icon)
	{
		switch(icon)
		{
			case Drug.ICON_SYRINGE:
				return Theme.getResourceAttribute(R.attr.drugIconSyringe);

			case Drug.ICON_GLASS:
				return Theme.getResourceAttribute(R.attr.drugIconGlass);

			case Drug.ICON_TUBE:
				return Theme.getResourceAttribute(R.attr.drugIconTube);

			case Drug.ICON_RING:
				return Theme.getResourceAttribute(R.attr.drugIconRing);

			case Drug.ICON_TABLET:
				// fall through, for now

			default:
				return Theme.getResourceAttribute(R.attr.drugIconTablet);
		}
	}

	/**
	 * Obtains a string attribute from an AttributeSet.
	 * <p>
	 * Note that this function automatically resolves string references.
	 *
	 * @param context The context
	 * @param attrs An AttributeSet to query
	 * @param namespace The attribute's namespace (in the form of <code>http://schemas.android.com/apk/res/&lt;package&gt;</code>)
	 * @param attribute The name of the attribute to query
	 * @param defaultValue A default value, in case there's no such attribute
	 * @return The attribute's value, or <code>null</code> if it does not exist
	 */
	public static String getStringAttribute(Context context, AttributeSet attrs, String namespace, String attribute, String defaultValue)
	{
		int resId = attrs.getAttributeResourceValue(namespace, attribute, -1);
		String value;

		if(resId == -1)
			value = attrs.getAttributeValue(namespace, attribute);
		else
			value = context.getString(resId);

		return value == null ? defaultValue : value;
	}

	public static void populateListPreferenceEntryValues(Preference preference)
	{
		ListPreference pref = (ListPreference) preference;
		int entryCount = pref.getEntries().length;

		String[] values = new String[entryCount];
		for(int i = 0; i != entryCount; ++i)
			values[i] = Integer.toString(i);

		pref.setEntryValues(values);
	}

	public static String millis(long millis)
	{
		return millis + "ms (" + new DumbTime(millis, true).toString(true, true) + ")";
	}

	public static boolean equals(Object a, Object b)
	{
		if(a == null && b == null)
			return true;
		else if(a != null)
			return a.equals(b);

		return b.equals(a);
	}

	/**
	 * Returns an array index for the given weekday.
	 *
	 * @param weekday a weekday as found in Calendar's DAY_OF_WEEK field.
	 * @return <code>0</code> for Monday, <code>1</code> for Tuesday, etc.
	 */
	public static int calWeekdayToIndex(int weekday) {
		return CollectionUtils.indexOf(weekday, Constants.WEEK_DAYS);
	}

	public static String rot13(String string)
	{
		final StringBuilder sb = new StringBuilder(string.length());

		for(int i = 0; i != string.length(); ++i)
		{
			char c = string.charAt(i);

			if(isAsciiLetter(c))
			{
				final int start = Character.isUpperCase(c) ? 65 : 97;
				if(c - start < 26)
					c = (char) (((c - start + 13) % 26) + start);
				else
					throw new IllegalStateException("Character out of range: c=" + c + "(" + (int) c + "), start=" + start);
			}

			sb.append(c);
		}

		return sb.toString();
	}

	public static boolean isAsciiLetter(char c)
	{
		if(c >= 97 /* a */)
			return c <= 'z';
		else if(c >= 65 /* A */)
			return c <= 'Z';

		return false;
	}

	public static void dumpObjectMembers(String tag, int priority, Object object, String name)
	{
		if(object == null)
		{
			Log.println(priority, tag, "dumpObjectMembers: (null) " + name);
			return;
		}

		final Class<?> clazz = object.getClass();
		final StringBuilder sb = new StringBuilder();
		sb.append("dumpObjectMembers: (" + clazz.getSimpleName() + ") " + name + "\n");

		for(Field f : clazz.getDeclaredFields())
		{
			int m = f.getModifiers();

			if(Modifier.isStatic(m))
				continue;

			sb.append("  (" + f.getType().getSimpleName() + ") " + f.getName() + "=");

			try
			{
				Reflect.makeAccessible(f);

				if(f.getType().isArray())
					sb.append(arrayToString(f.get(object)));
				else
					sb.append(f.get(object));
			}
			catch(IllegalArgumentException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(IllegalAccessException e)
			{
				sb.append("(inaccessible)");
			}

			sb.append("\n");
		}

		sb.append("----------\n");

		Log.println(priority, tag, sb.toString());
	}

	public static void sleepAtMost(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch(InterruptedException e)
		{
			// ignore
		}
	}

	public static void setTimePickerTime(TimePicker picker, DumbTime time)
	{
		picker.setCurrentHour(time.getHours());
		picker.setCurrentMinute(time.getMinutes());
	}

	public static String arrayToString(Object array)
	{
		if(array == null)
			return "null";

		final Class<?> type = array.getClass();
		if(!type.isArray())
			throw new IllegalArgumentException("Not an array");

		final int length = Array.getLength(array);
		if(length == 0)
			return "[]";

		final StringBuilder sb = new StringBuilder("[");

		for(int i = 0; i != length; ++i)
		{
			if(i != 0)
				sb.append(", ");

			final Object elem = Array.get(array, i);
			if(elem != null)
			{
				final Class<?> elemType = elem.getClass();

				if(elemType.isArray())
					sb.append(arrayToString(elem));
				else if(elemType == String.class)
					sb.append("\"" + elem.toString() + "\""); // TODO escape string?
				else
					sb.append(elem.toString());
			}
			else
				sb.append("null");
		}

		return sb.append("]").toString();
	}

	public static String streamToString(InputStream is)
	{
		final StringBuilder sb = new StringBuilder();
		final Scanner scanner = new Scanner(is);

		try
		{
			while(scanner.hasNextLine())
				sb.append(scanner.nextLine());
		}
		finally
		{
			scanner.close();
		}

		return sb.toString();
	}

	public static void closeQuietly(Closeable closeable)
	{
		try
		{
			closeable.close();
		}
		catch(Exception e)
		{
			// ignore
		}
	}

	private static DisplayMetrics getDisplayMetrics() {
		return RxDroid.getContext().getResources().getDisplayMetrics();
	}
}
