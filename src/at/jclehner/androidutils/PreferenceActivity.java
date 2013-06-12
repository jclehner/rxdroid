package at.jclehner.androidutils;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

@SuppressWarnings("deprecation")
public abstract class PreferenceActivity extends SherlockPreferenceActivity
{
	private static final String TAG = PreferenceActivity.class.getSimpleName();
	private static final boolean USE_WORKAROUND = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN;

	private static final String EXTRA_PREFERENCE_SCREEN_KEY =
			PreferenceActivity.class.getName() + ".EXTRA_PREFERENCE_SCREEN_KEY";

	private static final String EXTRA_PREFERENCES_RES_ID =
			PreferenceActivity.class.getName() + ".EXTRA_PREFERENCES_RES_ID";

	private int mEnqueuedPreferencesResId = 0;

	private boolean mIsRootPreferenceScreen = true;

	private boolean mIgnoreNextSetPreferenceScreenCall = false;
	private PreferenceScreen mLastIgnoredPreferenceScreen = null;

	private final Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if(shouldShowIndeterminateProgressOnScreenChange())
			requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final int resId = intent.getIntExtra(EXTRA_PREFERENCES_RES_ID, 0);
		final String key = intent.getStringExtra(EXTRA_PREFERENCE_SCREEN_KEY);

		if(key != null && resId != 0)
		{
			// even though there is no difference visually, it seems that skipping
			// the first call to setPreferenceScreen() makes the process of switching
			// a little faster
			mIgnoreNextSetPreferenceScreenCall = true;
			addPreferencesFromResource(resId);
			final PreferenceScreen ps = (PreferenceScreen) mLastIgnoredPreferenceScreen.findPreference(key);

			setPreferenceScreen(ps);
			mIsRootPreferenceScreen = false;
		}

		if(mIsRootPreferenceScreen && mEnqueuedPreferencesResId != 0)
			addPreferencesFromResource(mEnqueuedPreferencesResId);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		setSupportProgressBarIndeterminateVisibility(false);
	}

	public void enqueuePreferencesFromResource(int preferencesResId) {
		mEnqueuedPreferencesResId = preferencesResId;
	}

	@Override
	public void setPreferenceScreen(PreferenceScreen preferenceScreen)
	{
		if(!mIgnoreNextSetPreferenceScreenCall)
			super.setPreferenceScreen(preferenceScreen);
		else
		{
			mLastIgnoredPreferenceScreen = preferenceScreen;
			mIgnoreNextSetPreferenceScreenCall = false;
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		if(USE_WORKAROUND && mEnqueuedPreferencesResId != 0)
		{
			if(preference instanceof PreferenceScreen)
			{
				if(preference.getIntent() == null)
				{
					final String key = preference.getKey();
					if(key != null)
					{
						final Dialog dialog = ((PreferenceScreen) preference).getDialog();
						if(dialog != null)
							dialog.hide();

						if(shouldShowIndeterminateProgressOnScreenChange())
						{
							mHandler.post(new Runnable() {

								@Override
								public void run()
								{
									setSupportProgressBarIndeterminateVisibility(true);
								}
							});
						}

						final Intent intent = new Intent(getBaseContext(), getClass());
						intent.setAction(Intent.ACTION_MAIN);
						intent.putExtra(EXTRA_PREFERENCES_RES_ID, mEnqueuedPreferencesResId);
						intent.putExtra(EXTRA_PREFERENCE_SCREEN_KEY, key);

						if(true)
						{
							startActivity(intent);
							return true;
						}
						else
						{
							preference.setIntent(intent);
							return false;
						}
					}
					else
					{
						Log.w(TAG, "Clicked PreferenceScreen without key. Cannot work my magic!");

						// see http://code.google.com/p/android/issues/detail?id=4611#c35

						super.onPreferenceTreeClick(preferenceScreen, preference);

						try
						{
							final Dialog dialog = ((PreferenceScreen) preference).getDialog();
							dialog.getWindow().getDecorView().setBackgroundDrawable(this.getWindow()
									.getDecorView().getBackground().getConstantState().newDrawable());
						}
						catch(NullPointerException e)
						{
							Log.w(TAG, e);
						}

						return false;
					}
				}
			}
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == android.R.id.home)
		{
			if(!mIsRootPreferenceScreen)
			{
				onBackPressed();
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if(!mIsRootPreferenceScreen && shouldHideOptionsMenuInSubscreens())
			return false;

		return super.onCreateOptionsMenu(menu);
	}

	protected boolean shouldHideOptionsMenuInSubscreens() {
		return false;
	}

	protected boolean shouldShowIndeterminateProgressOnScreenChange() {
		return false;
	}
}

