package at.jclehner.rxdroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class DoseTimePreferenceActivity extends PreferenceActivityBase
{
	public static final String EXTRA_IS_FIRST_LAUNCH = "at.jclehner.rxroid.extras.IS_FIRST_LAUNCH";

	private final Intent mHomeBtnIntent = new Intent(Intent.ACTION_MAIN);
	private boolean mIsFirstLaunch;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dose_time_settings);
		addPreferencesFromResource(R.xml.dose_times);

		final Class<?> intentClass;
		final int fistLaunchStuffVisibility;

		mIsFirstLaunch = getIntent().getBooleanExtra(EXTRA_IS_FIRST_LAUNCH, false);

		if(mIsFirstLaunch)
		{
			intentClass = DrugListActivity.class;
			fistLaunchStuffVisibility = View.VISIBLE;
		}
		else
		{
			intentClass = PreferencesActivity.class;
			fistLaunchStuffVisibility = View.GONE;
		}

		findViewById(R.id.btn_ok).setVisibility(fistLaunchStuffVisibility);
		findViewById(R.id.help).setVisibility(fistLaunchStuffVisibility);

		mHomeBtnIntent.setClass(getBaseContext(), intentClass);
	}

	public void onSaveButtonClicked(View view)
	{
		Settings.putBoolean(Settings.Keys.IS_FIRST_LAUNCH, false);
		startActivity(mHomeBtnIntent);
	}

	@Override
	protected Intent getHomeButtonIntent() {
		return mHomeBtnIntent;
	}

	@Override
	protected boolean isHomeButtonEnabled() {
		return !mIsFirstLaunch;
	}
}
