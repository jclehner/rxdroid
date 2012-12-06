package at.jclehner.rxdroid;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public abstract class PreferenceActivityBase extends PreferenceActivity
{
	protected void onCreate(Bundle savedInstanceState)
	{
		// See android issue #4611
		setTheme(Version.SDK_IS_PRE_HONEYCOMB ? android.R.style.Theme : Theme.get());
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		LockscreenActivity.startMaybe(this);
		Settings.maybeLockInPortraitMode(this);
		RxDroid.setIsVisible(this, true);

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER && isHomeButtonEnabled())
		{
			ActionBar ab = getActionBar();
			ab.setDisplayShowHomeEnabled(isHomeButtonEnabled());
			ab.setDisplayHomeAsUpEnabled(isHomeButtonEnabled());
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		RxDroid.setIsVisible(this, false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
		{
			if(item.getItemId() == android.R.id.home)
			{
				final Intent intent = getHomeButtonIntent();
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);

				startActivity(intent);

				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	protected abstract Intent getHomeButtonIntent();

	protected boolean isHomeButtonEnabled() {
		return true;
	}
}
