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

package at.jclehner.rxdroid.util;

import android.annotation.TargetApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.Html;
import android.text.Spannable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import org.joda.time.LocalDate;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.DumbTime;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.Version;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Schedule;

public final class Util
{
	private static final String TAG = Util.class.getSimpleName();

	public static boolean equalsIgnoresNull(Object a, Object b)
	{
		if(a == null && b == null)
			return true;
		else if(a != null && b != null)
			return a.equals(b);
		else
			return false;
	}

	public static boolean equalsLong(long long1, long long2, long epsilon) {
		return Math.abs(long1 - long2) < epsilon;
	}

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

	public static void lockActivityOrientation(AppCompatActivity activity,
			int orientation)
	{
		activity.setRequestedOrientation(orientation);
		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	}

	// public static <T extends Serializable> T copy(T object)
	// {
	//
	// }

	public static void applyStyle(Spannable spannable, Object style)
	{
		spannable.setSpan(style, 0, spannable.length(), 0);
	}

	public static int pixelsFromSips(int sips)
	{
		return Math.round(sips * getDisplayMetrics(null).scaledDensity);
	}

	public static int pixelsFromDips(int dips) {
		return pixelsFromDips(null, dips);
	}

	public static int pixelsFromDips(Context context, int dips) {
		return Math.round(dips * getDisplayMetrics(context).density);
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
		return getDoseTimeDrawableFromDoseViewId(Constants
				.getDoseViewId(doseTime));
	}

	public static String escapeHtml(CharSequence str)
	{
		if(Build.VERSION.SDK_INT >= 16)
			return Html.escapeHtml(str);

		// yes, this function is extremely stupid, but it suffices
		// for our purposes

		final StringBuilder sb = new StringBuilder(str.length());
		for(int i = 0; i != str.length(); ++i)
		{
			final int cp = Character.codePointAt(str, i);
			sb.append("&#x" + Integer.toHexString(cp));
		}

		return sb.toString();
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
				return "(invalid " + doseTime + ")";
		}

		return RxDroid.getContext().getString(resId);
	}

	public static int getDrugIconDrawable(int icon)
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

			case Drug.ICON_CAPSULE:
				return Theme.getResourceAttribute(R.attr.drugIconCapsule);

			case Drug.ICON_INHALER:
				return Theme.getResourceAttribute(R.attr.drugIconInhaler);

			case Drug.ICON_AMPOULE:
				return Theme.getResourceAttribute(R.attr.drugIconAmpoule);

			case Drug.ICON_IV_BAG:
				return Theme.getResourceAttribute(R.attr.drugIconIvBag);

			case Drug.ICON_PIPETTE:
				return Theme.getResourceAttribute(R.attr.drugIconPipette);

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
	 * @param context
	 *            The context
	 * @param attrs
	 *            An AttributeSet to query
	 * @param namespace
	 *            The attribute's namespace (in the form of
	 *            <code>http://schemas.android.com/apk/res/&lt;package&gt;</code>
	 *            )
	 * @param attribute
	 *            The name of the attribute to query
	 * @param defaultValue
	 *            A default value, in case there's no such attribute
	 * @return The attribute's value, or <code>null</code> if it does not exist
	 */
	public static String getStringAttribute(Context context,
			AttributeSet attrs, String namespace, String attribute,
			String defaultValue)
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

	@Deprecated
	public static String millis(long millis) {
		return Millis.toString(millis);
	}

	/**
	 * Returns an array index for the given weekday.
	 *
	 * @param weekday
	 *            a weekday as found in Calendar's DAY_OF_WEEK field.
	 * @return <code>0</code> for Monday, <code>1</code> for Tuesday, etc.
	 */
	public static int calWeekdayToIndex(int weekday)
	{
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

	public static String getDbErrorMessage(Context context, DatabaseHelper.DatabaseError e)
	{
		switch(e.getType())
		{
			case DatabaseHelper.DatabaseError.E_DOWNGRADE:
				return context.getString(R.string._msg_db_error_downgrade);

			case DatabaseHelper.DatabaseError.E_UPGRADE:
				return context.getString(R.string._msg_db_error_upgrade);

			default:
				return context.getString(R.string._msg_db_error_general);
		}
	}

	public static boolean isAsciiLetter(int c)
	{
		if(c >= 97 /* a */)
			return c <= 'z';
		else if(c >= 65 /* A */)
			return c <= 'Z';

		return false;
	}

	public static void dumpObjectMembers(String tag, int priority,
			Object object, String name)
	{
		if(object == null)
		{
			Log.println(priority, tag, "dumpObjectMembers: (null) " + name);
			return;
		}

		final Class<?> clazz = object.getClass();
		final StringBuilder sb = new StringBuilder();
		sb.append("dumpObjectMembers: (" + clazz.getSimpleName() + ") " + name + "\n");

		// for(Field f : clazz.getDeclaredFields())
		for(Field f : Reflect.getAllFields(clazz))
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

	public static boolean wasInstalledViaGooglePlay()
	{
		final String installer = getInstallerPackageName(RxDroid.getContext().getPackageName());
		if(installer == null)
			return false;

		return installer.startsWith("com.android") || installer.startsWith("com.google");
	}

	public static String getInstallerPackageName(String packageName)
	{
		final PackageManager pm = RxDroid.getContext().getPackageManager();
		return pm.getInstallerPackageName(packageName);
	}

	public static void closeQuietly(Closeable closeable)
	{
		if(closeable == null)
			return;

		try
		{
			closeable.close();
		}
		catch(IOException e)
		{
			// ignore
		}
	}

	public static void copy(InputStream in, OutputStream out) throws IOException
	{
		byte[] buf = new byte[10*1024];
		int len;

		while((len = in.read(buf)) > 0)
			out.write(buf, 0, len);

		closeQuietly(in);
		closeQuietly(out);
	}

	public static String capitalize(String str)
	{
		if(str.length() == 0)
			return "";

		char first = str.charAt(0);
		if(Character.isLetter(first) && Character.isLowerCase(first))
			first = Character.toUpperCase(first);
		else
			return str;

		if(str.length() > 1)
			return first + str.substring(1);

		return Character.toString(first);
	}

	public static Bundle createBundle(Object... args)
	{
		if(args.length % 2 != 0)
			throw new IllegalArgumentException(
					"Array length must be divisible by 2");

		Bundle b = new Bundle();

		for(int i = 0; i < args.length; i += 2)
		{
			final String key;
			try
			{
				key = (String) args[i];
			}
			catch(ClassCastException e)
			{
				throw new IllegalArgumentException(
						"Expected string type for key at pos " + i);
			}

			final Object value = args[i + 1];
			final Class<?> type = value.getClass();

			if(type == int.class || type == Integer.class)
				b.putInt(key, (Integer) value);
			else if(type == boolean.class || type == Boolean.class)
				b.putBoolean(key, (Boolean) value);
			else if(type == String.class)
				b.putString(key, (String) value);
			else
				throw new IllegalArgumentException("Unhandled value type: "
						+ type);
		}

		return b;
	}

	public static void showExceptionDialog(Context context, Exception e)
	{
		final AlertDialog.Builder ab = new AlertDialog.Builder(context);
		ab.setTitle(R.string._title_error);
		ab.setIcon(android.R.drawable.ic_dialog_alert);
		ab.setPositiveButton(android.R.string.ok, null);
		ab.setCancelable(true);

		ab.setMessage(Html.fromHtml("<tt>" + e.getClass().getSimpleName() + "</tt><br/>"
				+ Util.escapeHtml(e.getMessage())));
		ab.show();
	}

	/*
	 * VULGAR FRACTION 1/4 ¼ &frac14; &#188; &#xBC; VULGAR FRACTION 1/2 ½
	 * &frac12; &#189; &#xBD; VULGAR FRACTION 3/4 ¾ &frac34; &#190; &#xBE;
	 * VULGAR FRACTION 1/3 ⅓ -- &#8531; &#x2153; VULGAR FRACTION 2/3 ⅔ --
	 * &#8532; &#x2154; VULGAR FRACTION 1/5 ⅕ -- &#8533; &#x2155; VULGAR
	 * FRACTION 2/5 ⅖ -- &#8354; &#x2156; VULGAR FRACTION 3/5 ⅗ -- &#8535;
	 * &#x2157; VULGAR FRACTION 4/5 ⅘ -- &#8536; &#x2158; VULGAR FRACTION 1/6 ⅙
	 * -- &#8537; &#x2159; VULGAR FRACTION 5/6 ⅚ -- &#8538; &#x215A; VULGAR
	 * FRACTION 1/8 ⅛ -- &#8539; &#x215B; VULGAR FRACTION 3/8 ⅜ -- &#8540;
	 * &#x215C; VULGAR FRACTION 5/8 ⅝ -- &#8541; &#x215D; VULGAR FRACTION 7/8 ⅞
	 * -- &#8542; &#x215E;
	 */

	private static final String[][] PRETTY_FRACTIONS = { { "\u00BD" }, // "1/2"
			{ "\u2153", "\u2154" }, // "1/3", "2/3"
			{ "\u00BC", null, "\u00BE" }, // "1/4", null, "3/4"
			{ "\u2155", "\u2156", "\u2157", "\u2158" }, // "1/5", "2/5", "3/5", "4/5"
			{ "\u2159", null, null, null, "\u215A" }, // "1/6", null, null, null, "5/6"
			null,
			{ "\u215B", null, "\u215C", null, "\u215D", null, "\u215E" } // "1/8", null, "3/8", null, "5/8", null, "7/8"
	};

	public static String prettify(Fraction frac)
	{
		/*if(!Settings.getBoolean(Keys.USE_PRETTY_FRACTIONS, Settings.Defaults.USE_PRETTY_FRACTIONS))
			return frac.toString();*/

		// Characters for fifths and sixths are available in the unicode specs,
		// but not in the standard
		// Android fonts: on Gingerbread, a placeholder box icon is displayed,
		// while Jellybean converts
		// them to their simple counterparts (i.e. 1/5 instead of ⅕)

		if(!frac.isInteger() && (frac.denominator() <= (Version.SDK_IS_LOLLIPOP_OR_NEWER ? 6 : 4) || frac.denominator() == 8))
		{
			final int[] data = frac.getFractionData(true);
			// numerator minus integer component!
			final int wholeNum = data[0];
			final int numerator = data[1];
			final int denominator = data[2];

			final String pretty = PRETTY_FRACTIONS[denominator - 2][numerator - 1];
			if(pretty != null)
				return wholeNum != 0 ? (wholeNum + pretty) : pretty;
		}

		return frac.toString();
	}

	public static String visibilityToString(int visibility)
	{
		switch(visibility)
		{
			case View.VISIBLE:
				return "VISIBLE";

			case View.INVISIBLE:
				return "INVISIBLE";

			case View.GONE:
				return "GONE";

			default:
				return "??? (" + visibility + ")";
		}
	}

	public static String stackTraceToString(Throwable t)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	public static View findViewById(View root, int id)
	{
		Log.d(TAG, "findViewById");

		if(root.getId() == id)
			return root;

		if(root instanceof ViewGroup)
			return findViewById((ViewGroup) root, id, 0);

		return null;
	}

	// https://gist.github.com/mrenouf/889747
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		FileInputStream fIn = null;
		FileOutputStream fOut = null;
		FileChannel source = null;
		FileChannel destination = null;
		try {
			fIn = new FileInputStream(sourceFile);
			source = fIn.getChannel();
			fOut = new FileOutputStream(destFile);
			destination = fOut.getChannel();
			long transfered = 0;
			long bytes = source.size();
			while (transfered < bytes) {
				transfered += destination.transferFrom(source, 0, source.size());
				destination.position(transfered);
			}
		} finally {
			if (source != null) {
				source.close();
			} else if (fIn != null) {
				fIn.close();
			}
			if (destination != null) {
				destination.close();
			} else if (fOut != null) {
				fOut.close();
			}
		}
	}

	public static String runCommand(String cmdline)
	{
		try
		{
			Process process = Runtime.getRuntime().exec(cmdline);
			process.waitFor();
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			StringBuilder log = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				log.append(line + "\n");
			}

			return log.toString();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static boolean writeToFile(File file, String data)
	{
		try
		{
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file));
			osw.write(data);
			osw.close();
			return true;
		}
		catch(FileNotFoundException e)
		{
			return false;
		}
		catch(IOException e)
		{
			return false;
		}
	}

	private static View findViewById(ViewGroup group, int id, int level)
	{
		for(int i = 0; i != group.getChildCount(); ++i)
		{
			final View child = group.getChildAt(i);

			Log.d(TAG, charsToString(' ', 2*level) + child);

			if(child.getId() == id)
				return child;
			else if(child instanceof ViewGroup)
				return findViewById((ViewGroup) child, id, level + 1);
		}

		return null;
	}

	private static String charsToString(char ch, int count)
	{
		char[] data = new char[count];
	    Arrays.fill(data, ch);
	    return new String(data);
	}

	private static DisplayMetrics getDisplayMetrics(Context context)
	{
		if(context == null)
			context = RxDroid.getContext();

		return context.getResources().getDisplayMetrics();
	}
}
