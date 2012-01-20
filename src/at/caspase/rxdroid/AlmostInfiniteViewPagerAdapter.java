package at.caspase.rxdroid;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public class AlmostInfiniteViewPagerAdapter extends PagerAdapter
{
	private static final int PAGES_PER_SIDE = 10000;
	private static final int ITEM_COUNT = 2 * PAGES_PER_SIDE + 1;

	public static final int CENTER = PAGES_PER_SIDE + 1;

	private int mOffset = 0;
	private ViewFactory mFactory;

	public interface ViewFactory
	{
		View makeView(int offset, boolean wasSlideLeft);
	}

	public void setViewFactory(ViewFactory viewFactory) {
		mFactory = viewFactory;
	}

	public int getOffset() {
		return mOffset;
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
		final boolean wasSlideLeft = offset < mOffset;
		mOffset = offset;

		View v = mFactory.makeView(offset, wasSlideLeft);
		container.addView(v);
		return v;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object item) {
		container.removeView((View) item);
	}
}
