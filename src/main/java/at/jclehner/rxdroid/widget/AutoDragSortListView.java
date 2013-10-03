/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.Version;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

public class AutoDragSortListView extends DragSortListView
{
	public interface OnOrderChangedListener
	{
		void onOrderChanged();
	}

	@SuppressWarnings("rawtypes")
	private ArrayAdapter mAdapter;
	private DragSortController mController;
	private OnOrderChangedListener mOnOrderChangedListener;

	private View mDraggedChild;
	private Drawable mDraggedChildBackground;

	public AutoDragSortListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		if(isInEditMode())
			return;

		final int alpha = (Theme.isDark() ? 0xc0 : 0x40) << 24;
		final int colorId = Theme.isDark() ? R.color.active_text_light : R.color.active_text_dark;
		// first remove the alpha via XOR, then OR the new alpha back in
		final int color = (getResources().getColor(colorId) ^ 0xff000000) | alpha;

		mController = new DragSortController(this) {

			@Override
			public View onCreateFloatView(int position)
			{
				if(Version.SDK_IS_PRE_HONEYCOMB)
				{
					mDraggedChild = getChildAt(position + getHeaderViewsCount() - getFirstVisiblePosition());

					if(mDraggedChild != null)
					{
						// On pre-Honeycomb, the ListView items appear to have a background set
						//v.setBackgroundResource(0);
						mDraggedChildBackground = mDraggedChild.getBackground();
						mDraggedChild.setBackgroundColor(color);
					}
				}

				return super.onCreateFloatView(position);
			}

			@SuppressWarnings("deprecation")
			@Override
			public void onDestroyFloatView(View floatView)
			{
				if(Version.SDK_IS_PRE_HONEYCOMB)
				{
					mDraggedChild.setBackgroundDrawable(mDraggedChildBackground);
					mDraggedChildBackground = null;
					mDraggedChild = null;
				}

				super.onDestroyFloatView(floatView);
			}

		};

		mController.setDragInitMode(DragSortController.ON_DOWN);
		mController.setSortEnabled(true);

		mController.setBackgroundColor(color);

		setOnTouchListener(mController);
		setFloatViewManager(mController);
		setDropListener(mDropListener);
	}

	public void setDragHandleId(int id)
	{
		mController.setDragHandleId(id);
		setDragEnabled(true);
	}

	public void setOnOrderChangedListener(OnOrderChangedListener l) {
		mOnOrderChangedListener = l;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setAdapter(ListAdapter adapter)
	{
		if(adapter instanceof ArrayAdapter)
		{
			mAdapter = (ArrayAdapter) adapter;
			super.setAdapter(mAdapter);
		}
		else
			throw new IllegalArgumentException("Only ArrayAdapters are supported");
	}

	private final DropListener mDropListener = new DropListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void drop(int from, int to)
		{
			Object item = mAdapter.getItem(from);
			mAdapter.remove(item);
			mAdapter.insert(item, to);
			mAdapter.notifyDataSetChanged();

			if(mOnOrderChangedListener != null)
				mOnOrderChangedListener.onOrderChanged();
		}
	};
}
