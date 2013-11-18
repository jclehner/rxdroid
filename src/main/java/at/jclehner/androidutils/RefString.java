package at.jclehner.androidutils;


import android.content.Context;
import android.content.res.Resources;

public class RefString
{
	public static String resolve(Context context, CharSequence text)
	{
		final StringBuilder sb = new StringBuilder(text);
		final String defPackage = context.getApplicationContext().getPackageName();
		final Resources res = context.getResources();

		int beg;

		while((beg = sb.indexOf("[@string/")) != -1)
		{
			int end = sb.indexOf("]", beg);
			String name = sb.substring(beg + 2, end);
			int resId = res.getIdentifier(name, null, defPackage);
			if(resId == 0)
				throw new IllegalArgumentException("No @" + name + " in package " + defPackage);

			sb.replace(beg, end + 1, context.getString(resId));
		}

		return sb.toString();
	}

	public static String resolve(Context context, int textResId) {
		return resolve(context, context.getString(textResId));
	}

	private RefString() {}
}
