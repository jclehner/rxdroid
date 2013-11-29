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
