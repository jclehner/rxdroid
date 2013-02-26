/**
 * RxDroid - A Medication Reminder
 * Copyright 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
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

package at.jclehner.androidutils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.Button;
import at.jclehner.rxdroid.Version;

public abstract class MultiPageDialog extends AdvancedAlertDialog implements OnClickListener
{
	private static final int BUTTON_BACK;
	private static final int BUTTON_NEXT;

	static
	{
		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
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

	private int mCurrentPage;

	public MultiPageDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	public MultiPageDialog(Context context) {
		super(context);
	}

	public final void setPage(int page)
	{
		mCurrentPage = page;
		onPageChanged(page);
		updateButtonTexts();
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == BUTTON_NEXT)
		{
			if(mCurrentPage + 1 < getPageCount())
			{
				setPage(mCurrentPage + 1);
				return;
			}
		}
		else if(which == BUTTON_BACK)
		{
			if(mCurrentPage > 0)
			{
				setPage(mCurrentPage - 1);
				return;
			}
		}
		else
			return;

		dismiss();
	}

	protected abstract CharSequence getBackButtonText(boolean isAtBeginning);
	protected abstract CharSequence getNextButtonText(boolean isAtEnd);

	protected abstract void onPageChanged(int page);
	protected abstract int getPageCount();

	protected final int getCurrentPage() {
		return mCurrentPage;
	}

	@Override
	protected final void onShow() {
		setPage(mCurrentPage);
	}

	private void updateButtonTexts()
	{
		Button b = getButton(BUTTON_BACK);
		if(b != null)
			b.setText(getBackButtonText(mCurrentPage == 0));

		b = getButton(BUTTON_NEXT);
		if(b != null)
			b.setText(getNextButtonText(mCurrentPage == getPageCount() - 1));
	}
}
