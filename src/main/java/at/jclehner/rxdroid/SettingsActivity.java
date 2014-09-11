package at.jclehner.rxdroid;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import at.jclehner.rxdroid.util.Components;

public class SettingsActivity extends ActionBarActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, 0);
		super.onCreate(savedInstanceState);

		getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
				new SettingsFragment()).commit();
	}

	@Override
	protected void onResume()
	{
		Components.onResumeActivity(this, 0);
		super.onResume();
	}
}
