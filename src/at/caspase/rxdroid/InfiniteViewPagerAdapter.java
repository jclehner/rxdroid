/**
 * Copyright (C) 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.caspase.rxdroid;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

public class InfiniteViewPagerAdapter extends PagerAdapter
{
	private static final int PAGES_PER_SIDE = 10000;
	private static final int ITEM_COUNT = 2 * PAGES_PER_SIDE + 1;

	public static final int CENTER = PAGES_PER_SIDE + 1;

	private ViewFactory mFactory;

	public interface ViewFactory
	{
		View makeView(int offset /*, boolean wasSlideLeft*/);
	}

	public InfiniteViewPagerAdapter(ViewFactory viewFactory) {
		mFactory = viewFactory;
	}

	@Override
	public int getCount() {
		return ITEM_COUNT;
	}

	@Override
	public boolean isViewFromObject(View v, Object o) {
		return v == (View) o;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		if(mFactory == null)
			throw new IllegalStateException("No ViewFactory supplied");

		final int offset = position - CENTER;

		View v = mFactory.makeView(offset);
		container.addView(v);
		return v;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object item) {
		container.removeView((View) item);
	}

	public static int getPagerOffset(ViewPager pager) {
		return pager.getCurrentItem() - CENTER;
	}
}
