package at.jclehner.androidutils;

import android.os.Build;
import android.view.KeyEvent;

/**
 * Dirty fix for a bug in LG ROMs.
 * See <a href="https://code.google.com/p/android/issues/detail?id=78154">
 * issue #78154</a>.
 */
public class ActionBarActivity extends android.support.v7.app.ActionBarActivity
{
	private static final boolean USE_WORKAROUND =
			Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT &&
			Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1 &&
			("LGE".equalsIgnoreCase(Build.MANUFACTURER) || "E6710".equalsIgnoreCase(Build.DEVICE));

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(needsWorkaround(keyCode))
			return true;

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(needsWorkaround(keyCode))
		{
			openOptionsMenu();
			return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	private static boolean needsWorkaround(int keyCode) {
		return USE_WORKAROUND && keyCode == KeyEvent.KEYCODE_MENU;
	}
}
