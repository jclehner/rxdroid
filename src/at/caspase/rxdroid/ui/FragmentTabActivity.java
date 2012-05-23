package at.caspase.rxdroid.ui;


import java.util.Date;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TabHost;
import at.caspase.androidutils.compat.TabManager;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.Settings;
import at.caspase.rxdroid.db.Drug;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class FragmentTabActivity extends SherlockFragmentActivity
{
	private TabHost mTabHost;
	private TabManager mTabManager;

	private static final boolean FALSE = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.setContentView(R.layout.fragment_tab_activity);

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);


		ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ab.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		if(/* TODO some check */ true)
		{
			//mTabHost.setup();
			//mTabManager = new TabManager(this, mTabHost, R.id.realtabcontent);

			final Object[][] allTabArgs = {
					{ "morning", R.string._Morning, R.drawable.ic_morning, Drug.TIME_MORNING },
					{ "noon", R.string._Noon, R.drawable.ic_noon, Drug.TIME_NOON },
					{ "evening", R.string._Evening, R.drawable.ic_evening, Drug.TIME_EVENING },
					{ "night", R.string._Night, R.drawable.ic_night, Drug.TIME_NIGHT }
			};

			final Resources res = getResources();
			final Date date = Settings.instance().getActiveDate();

			for(Object[] tabArgs: allTabArgs)
			{
				final String tag = (String) tabArgs[0];
				final int titleResId = (Integer) tabArgs[1];
				final int iconResId = (Integer) tabArgs[2];
				final int doseTime = (Integer) tabArgs[3];

				/*final TabSpec tabSpec = mTabHost.newTabSpec(tag);
				tabSpec.setIndicator(getString(titleResId), res.getDrawable(iconResId));

				final Bundle args = new Bundle();
				args.putSerializable(DrugListFragment.ARG_DATE, date);
				args.putInt(DrugListFragment.ARG_DOSE_TIME, doseTime);

				mTabManager.addTab(tabSpec, DrugListFragment.class, args);*/

				final Bundle args = new Bundle();
				args.putSerializable(DrugListFragment.ARG_DATE, date);
				args.putInt(DrugListFragment.ARG_DOSE_TIME, doseTime);

				final Tab tab = ab.newTab();
				tab
					.setText(titleResId)
					//.setIcon(iconResId)
					.setTabListener(new TabListener<DrugListFragment>(this, tag, DrugListFragment.class, args));

				ab.addTab(tab);
			}
		}

	}

	@Override
	public void setContentView(int layoutResID) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setContentView(View view) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setContentView(View view, LayoutParams params) {
		throw new UnsupportedOperationException();
	}
}
