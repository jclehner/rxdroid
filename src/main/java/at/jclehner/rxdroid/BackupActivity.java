package at.jclehner.rxdroid;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import at.jclehner.rxdroid.util.Components;

public class BackupActivity extends SherlockFragmentActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);
		super.onCreate(savedInstanceState);

		getSupportFragmentManager().beginTransaction().replace(
				android.R.id.content, new BackupFragment()).commit();
	}
}
