package at.jclehner.rxdroid;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import at.jclehner.rxdroid.util.Components;

public class DoseTimePreferenceActivity2 extends ActionBarActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, 0);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_activity);

		if(savedInstanceState == null)
		{
			getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
					new DoseTimePreferenceFragment()).commit();
		}
	}

	@Override
	protected void onResume()
	{
		Components.onResumeActivity(this, 0);
		super.onResume();
	}
}
