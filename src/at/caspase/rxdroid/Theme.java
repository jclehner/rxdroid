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

				final Context c = Application.getContext();
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
