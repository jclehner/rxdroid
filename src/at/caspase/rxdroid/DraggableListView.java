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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

public class DraggableListView extends ListView
{
	private static final String TAG = DraggableListView.class.getName();

	private Context mContext;
	private WindowManager mWindowMgr;

	private ImageView mDragView;
	private Bitmap mDragBitmap;
	private WindowManager.LayoutParams mLayoutParams;

	private int mLastAboveIdx = INVALID_POSITION;
	private int mLastBelowIdx = INVALID_POSITION;

	private int mLastDragIdx = INVALID_POSITION;

	private int mDragOriginIdx = INVALID_POSITION;
	private int mDragOriginHeight;

	private View mDragOriginView;

	private int mLastY = -1;
	private boolean mIsMovingDown;

	private OnDropListener mDropListener;

	public interface OnDropListener {
		void onDrop(int fromIndex, int toIndex);
	}


	public DraggableListView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.listViewStyle);
	}

	public DraggableListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		mContext = context;
		mWindowMgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		mLayoutParams = new WindowManager.LayoutParams();

		super.setOnItemLongClickListener(mOnItemLongClickListener);
	}

	@Override
	public void setOnItemLongClickListener(OnItemLongClickListener l) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch(action)
		{
			case MotionEvent.ACTION_MOVE:
				drag(event);
				break;

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				stopDrag(event);
				break;

			case MotionEvent.ACTION_OUTSIDE:
				return true;

			default:

		}

		return super.onTouchEvent(event);
	}

	/**
	 * Returns the index of the element at the specified coordinates.
	 * <p>
	 * Note that this is different from {@link #pointToPosition(int, int)}, which
	 * returns the position, wherein the top view (regardless of whether it's
	 * the first child in the ListView) always has <code>position == 0</code>.
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	protected int pointToIndex(int x, int y)
	{
		int pos = pointToPosition(x, y);
		return pos - getFirstVisiblePosition();
	}

	private void startDrag(int index)
	{
		View v = getChildAt(index);
		if(v == null)
			return;

		initDragView(v);

		// first store the original dimensions in the view's tag
		ViewInfo info = ViewInfo.fromView(v);
		v.setTag(info);

		// now obtain the view again, but change the height of the
		// drag origin to 1, which effectively hides it
		info = ViewInfo.fromView(v);
		info.height = 1;
		info.apply(v);

		layoutChildren();

		mDragOriginIdx = index;
		mDragOriginView = v;
		mDragOriginHeight = v.getHeight();
	}

	private void drag(MotionEvent event)
	{
		if(mDragView == null)
			return;

		int y = Math.round(event.getY());

		mIsMovingDown = y > mLastY;
		mLastY = y;

		mLayoutParams.y = y;
		mWindowMgr.updateViewLayout(mDragView, mLayoutParams);

		//mDragView.forceLayout();

		final int halfHeight = 2 + mDragOriginHeight / 2;
		final int top = y - halfHeight;
		final int bottom = y + halfHeight;

		final int aboveIdx = pointToIndex(1, top);
		final int belowIdx = pointToIndex(1, bottom);

		mLastDragIdx = pointToIndex(1, y);

		if(bottom + halfHeight > getBottom())
			scrollTo(1, bottom);

		/*if(aboveIdx == -1 && belowIdx != 0)
			return;

		if(belowIdx == -1)
		{
			if(aboveIdx == -1 || aboveIdx == getCount())
				// dragged to end of list
				;
			else
				return;
		}

		if(aboveIdx == mDragOriginIdx || belowIdx == mDragOriginIdx)
			return;

		if(aboveIdx == belowIdx)
			return;

		if(mLastAboveIdx != aboveIdx || mLastBelowIdx != belowIdx)
		{
			Log.d(TAG, "drag: aboveIdx=" + aboveIdx + ", belowIdx=" + belowIdx + ", halfHeight=" + halfHeight);
			Log.d(TAG, "   top=" + top + ", bottom=" + bottom);
		}

		//View v = getChildAt(aboveIdx);
		//if(v != null)
		//{
		//	final int aboveCenter = v.getTop() + v.getHeight() / 2;
		//	if(y

		//if(!mIsMovingDown && aboveIdx == mLastAboveIdx)

		if(aboveIdx == belowIdx)
		{
			restoreViewSizeAt(aboveIdx);
			return;
		}

		boolean wasChanged = false;


		if(mLastAboveIdx != aboveIdx)
		{
			wasChanged = true;
			restoreViewSizeAt(mLastAboveIdx);
			restoreViewSizeAt(aboveIdx);
			expandViewAt(aboveIdx, halfHeight, false);
			Log.d(TAG, "\n");
			mLastAboveIdx = aboveIdx;
		}


		if(mLastBelowIdx != belowIdx)
		{
			wasChanged = true;
			restoreViewSizeAt(mLastBelowIdx);
			restoreViewSizeAt(belowIdx);
			expandViewAt(belowIdx, halfHeight, true);
			mLastBelowIdx = belowIdx;


		}

		if(wasChanged)
			Log.d(TAG, "---");

		layoutChildren();*/

		/*final int dragIdx = pointToIndex(1, y);

		final int halfHeight = mDragView.getHeight() / 2;
		final int top = y - halfHeight - 1;
		final int bottom = y + halfHeight + 1;

		final int aboveIdx = pointToIndex(1, top);
		final int belowIdx = pointToIndex(1, bottom);

		if(mLastDragIdx != dragIdx)
			mLastDragIdx = dragIdx;
		else
			return;

		Log.d(TAG, "drag: dragIdx=" + dragIdx);

		final int expandBy = mDragView.getHeight() / 2;

		applyTagInfoAt(mLastBelowIdx);
		expandViewAt(belowIdx, expandBy, true);
		mLastBelowIdx = belowIdx;


		applyTagInfoAt(mLastAboveIdx);
		expandViewAt(aboveIdx, expandBy, false);
		mLastAboveIdx = aboveIdx;*/
	}

	@SuppressWarnings("unchecked")
	private void stopDrag(MotionEvent event)
	{
		/*restoreViewSizeAt(mLastAboveIdx);
		restoreViewSizeAt(mLastDragIdx);
		restoreViewSizeAt(mLastBelowIdx);*/
		restoreViewSizeAt(mDragOriginIdx, true);
		releaseDragViewResources();

		Log.d(TAG, "DnD:" + mDragOriginIdx + " -> " + mLastDragIdx);

		if(mDragOriginIdx == -1)
			return;

		if(mDropListener != null)
			mDropListener.onDrop(mDragOriginIdx, mLastDragIdx);

		ListAdapter adapter = getAdapter();
		if(adapter != null && adapter instanceof ArrayAdapter<?>)
		{
			@SuppressWarnings("rawtypes")
			ArrayAdapter aAdapter = (ArrayAdapter) getAdapter();

			Object o = aAdapter.getItem(mDragOriginIdx);
			aAdapter.remove(o);
			aAdapter.insert(o, mLastDragIdx);
			aAdapter.notifyDataSetChanged();
		}

		mLastAboveIdx = INVALID_POSITION;
		mLastBelowIdx = INVALID_POSITION;
		mLastDragIdx = INVALID_POSITION;
		mDragOriginIdx = INVALID_POSITION;

		mDragOriginView = null;

		layoutChildren();
	}

	private void initDragView(View originView)
	{
		// XXX
		originView.setBackgroundColor(Color.RED);

		originView.setDrawingCacheEnabled(true);
		originView.buildDrawingCache();
		mDragBitmap = Bitmap.createBitmap(originView.getDrawingCache());

		// XXX
		originView.setBackgroundDrawable(null);

		mDragView = new ImageView(mContext);
		mDragView.setImageBitmap(mDragBitmap);
		mDragView.setBackgroundResource(android.R.drawable.alert_dark_frame);

		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(originView.getWidth(), originView.getHeight());
		mDragView.setLayoutParams(params);

		mLayoutParams.gravity = Gravity.TOP;
		mLayoutParams.x = 0;
		mLayoutParams.y = originView.getTop();
		//mLayoutParams.height = childView.getHeight();
		//mLayoutParams.width = childView.getWidth();
		//mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;

		mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		mLayoutParams.format = PixelFormat.TRANSLUCENT;

		mWindowMgr.addView(mDragView, mLayoutParams);



		Log.d(TAG, "mDragView: " + mDragView.getHeight() + "x" + mDragView.getWidth());
		Log.d(TAG, "childView: " + originView.getHeight() + "x" + originView.getWidth());
	}

	private void releaseDragViewResources()
	{
		if(mDragBitmap != null)
		{
			mDragBitmap.recycle();
			mDragBitmap = null;
		}

		if(mDragView != null)
		{
			mWindowMgr.removeView(mDragView);
			mDragView.setImageBitmap(null);
			mDragView = null;
		}
	}

	private void expandViewAt(int index, int expandBy, boolean expandAtTop)
	{
		if(index == mDragOriginIdx)
		{
			//Log.d(TAG, "expandViewAt: refusing to expand origin view");
			return;
		}


		View v = getChildAt(index);
		if(v == null)
			return;

		Log.d(TAG, "* expandViewAt: index=" + index);

		Object tag = v.getTag();
		if(tag == null)
			v.setTag(ViewInfo.fromView(v));

		ViewInfo info = ViewInfo.fromView(v);
		info.height += expandBy;

		if(expandAtTop)
			info.paddingTop += expandBy;
		else
			info.paddingBottom += expandBy;

		info.apply(v);
		//layoutChildren();
	}

	private void restoreViewSizeAt(int index) {
		restoreViewSizeAt(index, false);
	}

	private void restoreViewSizeAt(int index, boolean allowOriginIdx)
	{
		if(!allowOriginIdx && index == mDragOriginIdx)
			return;

		View v = getChildAt(index);
		if(v == null)
			return;

		ViewInfo info = (ViewInfo) v.getTag();
		if(info != null)
		{
			info.apply(v);
			Log.d(TAG, "* restoreViewSizeAt: index=" + index);
			Log.d(TAG, "  top=" + info.paddingTop + ", bottom=" + info.paddingBottom + ", height=" + info.height);

			//layoutChildren();
		}
		//v.setTag(null);
	}

	private OnItemLongClickListener mOnItemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
		{
			startDrag(position - parent.getFirstVisiblePosition());
			v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			return true;
		}
	};

	private static class ViewInfo
	{
		int paddingTop;
		int paddingBottom;
		int height;
		boolean isExpanded = false;

		public ViewInfo() {}

		public ViewInfo(ViewInfo other)
		{
			paddingTop = other.paddingTop;
			paddingBottom = other.paddingBottom;
			height = other.height;
		}

		public void apply(View v)
		{
			ViewGroup.LayoutParams params = v.getLayoutParams();
			params.height = height;
			v.setLayoutParams(params);

			int left = v.getPaddingLeft();
			int right = v.getPaddingRight();
			v.setPadding(left, paddingTop, right, paddingBottom);
			v.forceLayout();
		}

		public static ViewInfo fromView(View v)
		{
			ViewInfo info = new ViewInfo();
			info.paddingTop = v.getPaddingTop();
			info.paddingBottom = v.getPaddingBottom();
			info.height = v.getHeight();

			return info;
		}
	}
}
