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

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.view.View;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.github.espiandev.showcaseview.ShowcaseView.OnShowcaseEventListener;

public final class ShowcaseViews
{
	private static final String TAG = ShowcaseViews.class.getSimpleName();

	private final List<ShowcaseView> mViews = new ArrayList<ShowcaseView>();

	private OnShowcaseEventListener mListener;

	public void add(ShowcaseView sv)
	{
		if(sv == null)
			return;

		if(sv.getVisibility() == View.VISIBLE)
		{
			sv.setOnShowcaseEventListener(mLocalListener);
			mViews.add(sv);
		}
		else
			Log.d("ShowcaseViews", "add: not adding; view might be redundant!");
	}

	public void setOnShowcaseEventListener(OnShowcaseEventListener l) {
		mListener = l;
	}

	public boolean hasViews() {
		return !mViews.isEmpty();
	}

	public void show()
	{
		if(hasViews())
		{
			final ShowcaseView sv = mViews.get(0);
			if(sv.getParent() == null)
				sv.show(false);

			mViews.remove(0);
		}
	}

	public void hide()
	{
		while(!mViews.isEmpty())
			mViews.remove(0).hide();
	}

	public void clear() {
		mViews.clear();
	}

	public ShowcaseViews() {}

	private final OnShowcaseEventListener mLocalListener = new OnShowcaseEventListener() {

		@Override
		public void onShowcaseViewShow(ShowcaseView showcaseView)
		{
			if(mListener != null)
				mListener.onShowcaseViewShow(showcaseView);
		}

		@Override
		public void onShowcaseViewHide(ShowcaseView showcaseView)
		{
			show();

			if(mListener != null)
				mListener.onShowcaseViewHide(showcaseView);
		}
	};

}
