package at.caspase.rxdroid.util;

import android.content.Context;

/**
 * Localization utilities.
 * 
 * @author Joseph Lehner
 *
 */
public class L10N
{
	public static String getText(Context context, int resId, Object... args) {
		return getText(context.getString(resId), args);
	}
	
	public static String getText(String format, Object... args)
	{
		for(int i = 0; i != args.length; ++i)
			format.replaceAll("^[%](%\\{?" + i + "\\}?)", args[i].toString());
		
		return format;
	}
}
