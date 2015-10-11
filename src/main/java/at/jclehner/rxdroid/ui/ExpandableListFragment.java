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

package at.jclehner.rxdroid.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.TextView;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Version;

public class ExpandableListFragment extends Fragment implements
		OnChildClickListener, OnGroupClickListener
{
	private ExpandableListView mList;
	private TextView mEmptyView;
	private ExpandableListAdapter mAdapter;
	private boolean mExpandAll = false;

	private int mEmptyViewTextResId;

	private OnGroupCollapseExpandListener mGroupCollapseExpandListener;

	public interface OnGroupCollapseExpandListener extends
			OnGroupCollapseListener, OnGroupExpandListener
	{}

	public void setEmptyViewText(int resId)
	{
		mEmptyViewTextResId = resId;

		if(mEmptyView != null)
			mEmptyView.setText(resId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.layout_expandable_list, null);
		mList = (ExpandableListView) v.findViewById(android.R.id.list);
		mEmptyView = (TextView) v.findViewById(android.R.id.empty);

		mList.setAdapter(mAdapter);
		mList.setOnGroupCollapseListener(mGroupCollapseExpandListener);
		mList.setOnGroupExpandListener(mGroupCollapseExpandListener);
		mList.setOnGroupClickListener(this);
		mList.setOnChildClickListener(this);

		if(mExpandAll)
			expandAllInternal();

		mList.setEmptyView(mEmptyView);
		mEmptyView.setText(mEmptyViewTextResId);

		return v;
	}

	@Override
	public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
		return false;
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		return false;
	}

	public void setListAdapter(ExpandableListAdapter adapter)
	{
		mAdapter = adapter;

		if(mList != null)
			mList.setAdapter(adapter);
	}

	public ExpandableListAdapter getListAdapter() {
		return mAdapter;
	}

	public void setOnGroupCollapseExpandListener(OnGroupCollapseExpandListener l)
	{
		mGroupCollapseExpandListener = l;

		if(mList != null)
		{
			mList.setOnGroupCollapseListener(l);
			mList.setOnGroupExpandListener(l);
		}
	}

	public void expandAll(boolean animate)
	{
		mExpandAll = true;

		if(mList == null || mAdapter == null)
			return;

		expandAllInternal();
	}

	public void collapseAll()
	{
		mExpandAll = false;

		if(mList == null || mAdapter == null)
			return;

		for(int i = 0; i != mAdapter.getGroupCount(); ++i)
			mList.collapseGroup(i);
	}

	public boolean isListEmpty()
	{
		if(mAdapter != null)
			return mAdapter.getGroupCount() == 0;

		return true;
	}

	protected ExpandableListView getListView() {
		return mList;
	}

	private void expandAllInternal(/*boolean animate*/)
	{
		for(int i = 0; i != mAdapter.getGroupCount(); ++i)
		{
			if(Version.SDK_IS_JELLYBEAN_OR_NEWER)
				mList.expandGroup(i /*, animate*/);
			else
				mList.expandGroup(i);
		}
	}
}
