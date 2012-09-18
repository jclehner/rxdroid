/**
 * Copyright (C) 2011, 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.caspase.androidutils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.widget.Button;
import at.caspase.rxdroid.Version;

public abstract class MultiPageDialog extends AlertDialogFragment
{
	private static final int BUTTON_BACK;
	private static final int BUTTON_NEXT;

	static
	{
		if(Version.SDK_IS_HONEYCOMB_OR_LATER)
		{
			BUTTON_BACK = Dialog.BUTTON_NEGATIVE;
			BUTTON_NEXT = Dialog.BUTTON_POSITIVE;
		}
		else
		{
			BUTTON_BACK = Dialog.BUTTON_POSITIVE;
			BUTTON_NEXT = Dialog.BUTTON_NEGATIVE;
		}
	}

	private int mPage;

	public final void setPage(int page)
	{
		mPage = page;
		onPageChanged(page);
		updateButtonTexts();
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == BUTTON_NEXT)
		{
			if(mPage + 1 < getPageCount())
			{
				setPage(mPage + 1);
				return;
			}
		}
		else if(which == BUTTON_BACK)
		{
			if(mPage > 0)
			{
				setPage(mPage - 1);
				return;
			}
		}
		else
			return;

		dismiss();
	}

	@Override
	public void onShow()
	{
		setPage(mPage);
	}

	protected abstract CharSequence getBackButtonText(boolean isAtBeginning);
	protected abstract CharSequence getNextButtonText(boolean isAtEnd);

	protected abstract void onPageChanged(int page);
	protected abstract int getPageCount();

	protected final int getCurrentPage() {
		return mPage;
	}

	private void updateButtonTexts()
	{
		Button b = getButton(BUTTON_BACK);
		if(b != null)
			b.setText(getBackButtonText(mPage == 0));

		b = getButton(BUTTON_NEXT);
		if(b != null)
			b.setText(getNextButtonText(mPage == getPageCount() - 1));
	}
}
