package at.jclehner.rxdroid.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;


public abstract class SimpleBaseExpandanbleListAdapter extends BaseExpandableListAdapter
{
	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}
}
