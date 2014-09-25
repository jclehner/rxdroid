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

package at.jclehner.rxdroid.preferences;

import java.io.Serializable;

import android.content.Context;
import android.util.AttributeSet;
import at.jclehner.androidutils.AdvancedDialogPreference;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Version;

public abstract class BaseAdvancedDialogPreference<T extends Serializable> extends AdvancedDialogPreference<T>
{
	public BaseAdvancedDialogPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public BaseAdvancedDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected int getDialogThemeResId()
	{
		return Version.SDK_IS_JELLYBEAN_OR_NEWER ? 0 :
				R.style.Theme_RxDroid_Dialog;
	}
}
