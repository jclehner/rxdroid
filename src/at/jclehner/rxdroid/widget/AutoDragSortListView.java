package at.jclehner.rxdroid.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
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

	@Override
	public boolean startDrag(int position, int dragFlags, int deltaX, int deltaY) {
		Log.d("AutoDragSortListView", "startDrag: pos=" + position);
		return super.startDrag(position, dragFlags, deltaX, deltaY);
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
