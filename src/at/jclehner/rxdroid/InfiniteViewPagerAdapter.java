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

package at.jclehner.rxdroid;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import at.jclehner.rxdroid.util.Util;

public class InfiniteViewPagerAdapter extends PagerAdapter
{
	private static final int PAGES_PER_SIDE = 10000;
	private static final int ITEM_COUNT = 2 * PAGES_PER_SIDE + 1;

	public static final int CENTER = PAGES_PER_SIDE + 1;
	public static final int MAX = ITEM_COUNT - 1;

	private ViewFactory mFactory;
	private int mCurrentItemIndex = -1;

	public interface ViewFactory
	{
		View makeView(int offset /*, boolean wasSlideLeft*/);
	}

	public InfiniteViewPagerAdapter(ViewFactory viewFactory) {
		mFactory = viewFactory;
	}

	public int getCurrentItemIndex() {
		return mCurrentItemIndex;
	}

	@Override
	public int getCount() {
		return ITEM_COUNT;
	}

	/*@Override
	public int getItemPosition(Object object) {
		return 1;
	}*/

	@Override
	public boolean isViewFromObject(View v, Object o) {
		return v == (View) o;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		if(mFactory == null)
			throw new IllegalStateException("No ViewFactory supplied");

		View v = mFactory.makeView(position - CENTER);
		Util.detachFromParent(v);
		container.addView(v);
		return v;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object item) {
		container.removeView((View) item);
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object)
	{
		super.setPrimaryItem(container, position, object);
		mCurrentItemIndex = position;
	}

	/*public static int getPagerOffset(ViewPager pager) {
		return pager.getCurrentItem() - CENTER;
	}*/
}
