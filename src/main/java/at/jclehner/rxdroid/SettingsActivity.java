package at.jclehner.rxdroid;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import at.jclehner.rxdroid.util.Components;

public class SettingsActivity extends ActionBarActivity
{
	private static final String EXTRA_PREFERENCE_SCREEN = "rxdroid:preference_screen";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, 0);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_activity);

		if(savedInstanceState == null)
		{
			final int resId;

			String prefScreenResIdStr = getIntent().getStringExtra(EXTRA_PREFERENCE_SCREEN);
			if(prefScreenResIdStr != null)
			{
				if(prefScreenResIdStr.charAt(0) == '@')
					prefScreenResIdStr = prefScreenResIdStr.substring(1);

				resId = getResources().getIdentifier(prefScreenResIdStr, null, getApplicationInfo().packageName);
			}
			else
				resId = R.xml.settings;

			final SettingsFragment f = SettingsFragment.newInstance(resId);
			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
		}
	}

	@Override
	protected void onResume()
	{
		Components.onResumeActivity(this, 0);
		super.onResume();
	}
}
