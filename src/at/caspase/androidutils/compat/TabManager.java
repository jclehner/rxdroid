package at.caspase.androidutils.compat;

import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class TabManager implements TabHost.OnTabChangeListener
{
	private final String TAG = TabManager.class.getName();
	private final boolean LOGV = true;

	private final FragmentActivity mActivity;
	private final TabHost mTabHost;
	private final int mContainerId;
	private final HashMap<String, TabManager.TabInfo> mTabs =
			new HashMap<String, TabManager.TabInfo>();

	private TabManager.TabInfo mLastTab = null;

	static final class TabInfo
	{
		private final String tag;
		private final Class<?> clazz;
		private final Bundle args;
		private Fragment fragment;

		public TabInfo(String tag, Class<?> clazz, Bundle args)
		{
			this.tag = tag;
			this.clazz = clazz;
			this.args = args;
		}
	}

	public TabManager(FragmentActivity activity, TabHost tabHost, int containerId)
	{
		mActivity = activity;
		mTabHost = tabHost;
		mContainerId = containerId;
		mTabHost.setOnTabChangedListener(this);
	}

	public void addTab(TabSpec tabSpec, Class<?> clazz, Bundle args)
	{
		final String tag = tabSpec.getTag();
		final TabManager.TabInfo info = new TabInfo(tag, clazz, args);
		final FragmentManager fm = mActivity.getSupportFragmentManager();

		tabSpec.setContent(new DummyTabFactory(mActivity));

		info.fragment = fm.findFragmentByTag(tag);
		if(info.fragment != null && !info.fragment.isDetached())
		{
			final FragmentTransaction ft = fm.beginTransaction();
			ft.detach(info.fragment);
			ft.commit();
		}

		mTabs.put(tag, info);
		mTabHost.addTab(tabSpec);
	}

	@Override
	public void onTabChanged(String tabId)
	{
		final FragmentManager fm = mActivity.getSupportFragmentManager();
		final TabManager.TabInfo newTab = mTabs.get(tabId);

		if(LOGV) Log.v(TAG, "onTabChanged(" + tabId + "): newTab=" + newTab);

		if(newTab != mLastTab)
		{
			final FragmentTransaction ft = fm.beginTransaction();
			if(mLastTab != null && mLastTab.fragment != null)
				ft.detach(mLastTab.fragment);

			if(newTab != null)
			{
				if(newTab.fragment == null)
				{
					newTab.fragment = Fragment.instantiate(mActivity, newTab.clazz.getName(), newTab.args);
					ft.add(mContainerId, newTab.fragment, newTab.tag);
				}
				else
					ft.attach(newTab.fragment);
			}

			mLastTab = newTab;
			ft.commit();
			fm.executePendingTransactions();
		}

	}

	static class DummyTabFactory implements TabHost.TabContentFactory
	{
		private final Context mContext;

		public DummyTabFactory(Context context) {
			mContext = context;
		}

		@Override
		public View createTabContent(String tag)
		{
			View v = new View(mContext.getApplicationContext());
			v.setMinimumHeight(0);
			v.setMinimumWidth(0);
			return v;
		}
	}

}