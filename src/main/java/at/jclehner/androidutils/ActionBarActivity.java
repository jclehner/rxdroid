/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2015 Joseph Lehner <joseph.c.lehner@gmail.com>
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

import android.os.Build;
import android.view.KeyEvent;

/**
 * Dirty fix for a bug in LG ROMs.
 * See <a href="https://code.google.com/p/android/issues/detail?id=78154">
 * issue #78154</a>.
 */
public class ActionBarActivity extends android.support.v7.app.ActionBarActivity
{
	private static final boolean USE_WORKAROUND = false;
//	private static final boolean USE_WORKAROUND =
//			Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT &&
//			Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1 &&
//			("LGE".equalsIgnoreCase(Build.MANUFACTURER) || "E6710".equalsIgnoreCase(Build.DEVICE));

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
