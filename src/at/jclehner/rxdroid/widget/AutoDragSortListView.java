package at.jclehner.rxdroid.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Theme;

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

	public AutoDragSortListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		if(isInEditMode())
			return;

		mController = new DragSortController(this);
		mController.setDragInitMode(DragSortController.ON_DOWN);
		mController.setSortEnabled(true);

		final int alpha = (Theme.isDark() ? 0xc0 : 0x40) << 24;
		final int colorId = Theme.isDark() ? R.color.active_text_dark : R.color.active_text_light;

		// first remove the alpha via XOR, then OR the new alpha back in
		mController.setBackgroundColor((getResources().getColor(colorId) ^ 0xff000000) | alpha);

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
