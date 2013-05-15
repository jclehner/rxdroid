package at.jclehner.rxdroid.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import at.jclehner.rxdroid.R;

public class ExpandableListFragment extends Fragment
{
	private ExpandableListView mList;
	private ExpandableListAdapter mAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.layout_expandable_list, null);
		mList = ((ExpandableListView) v.findViewById(android.R.id.list));
		mList.setAdapter(mAdapter);
		return v;
	}

	public void setListAdapter(ExpandableListAdapter adapter)
	{
		mAdapter = adapter;

		if(mList != null)
			mList.setAdapter(adapter);
	}

	public ExpandableListAdapter getAdapter() {
		return mAdapter;
	}
}
