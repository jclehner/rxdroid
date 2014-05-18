/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

package at.jclehner.rxdroid;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import at.jclehner.androidutils.RefString;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.DatabaseHelper.DatabaseError;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.ui.DialogueLike;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.EmailIntentSender;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.acra.ACRA;
import org.acra.collector.CrashReportData;
import org.acra.collector.CrashReportDataFactory;

public class SplashScreenActivity extends SherlockFragmentActivity implements OnClickListener
{
	@SuppressWarnings("unused")
	private static final boolean USE_MSG_HANDLER = BuildConfig.DEBUG;

	public class DatabaseStatusReceiver extends BroadcastReceiver
	{
		public static final String EXTRA_MESSAGE = "at.jclehner.rxdroid.extra.MESSAGE";
		public static final String EXTRA_PROGRESS = "at.jclehner.rxdroid.extra.PROGRESS";

		@Override
		public void onReceive(Context context, Intent intent)
		{
			if(intent == null)
				return;

			final TextView msg = (TextView) SplashScreenActivity.this.findViewById(R.id.text_loading);
			if(msg == null)
				return;

			final int msgResId = intent.getIntExtra(EXTRA_MESSAGE, R.string._title_db_status_loading);
			msg.setText(getString(R.string._title_database) + ": " + getString(msgResId));
		}
	}

	public class DatabaseStatusHandler extends Handler
	{
		public static final int MSG_SET_TEXT = 0;

		@Override
		public void handleMessage(Message msg)
		{
			if(msg.what == MSG_SET_TEXT)
			{
				final TextView text = (TextView) findViewById(R.id.text_loading);
				if(text != null)
				{
					final int textResId = msg.arg1;
					text.setText(getString(R.string._title_database) + ": " + getString(textResId));
					return;
				}
			}

			super.handleMessage(msg);
		}
	}

	private static final String TAG = SplashScreenActivity.class.getSimpleName();
	private static final String ARG_EXCEPTION = "exception";

	private final BroadcastReceiver mReceiver = new DatabaseStatusReceiver();
	private Date mDate;
	private boolean mLaunchMainActivity = true;

	private WrappedCheckedException mException = null;

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Settings.init();

		setTheme(Theme.get());
		super.onCreate(savedInstanceState);

		final long bootCompletedTimestamp = Settings.getLong(Settings.Keys.BOOT_COMPLETED_TIMESTAMP, 0);
		final long bootTimestamp = RxDroid.getBootTimestamp();
		final boolean forceSplashWarning =
				BuildConfig.DEBUG && Settings.getBoolean(Settings.Keys.DEBUG_FORCE_SPLASH_WARNING, false);

		// getBootTimestamp() does not return a constant value when using the fallback
		// method, so we allow the times to be off by +/- 100ms
		if(!Util.equalsLong(bootCompletedTimestamp, bootTimestamp, 100) || forceSplashWarning)
		{
			if(bootTimestamp != Settings.getLong(Settings.Keys.LAST_NOT_STARTED_WARNING_TIMESTAMP, 0)
					|| forceSplashWarning)
			{
				Settings.putLong(Settings.Keys.LAST_NOT_STARTED_WARNING_TIMESTAMP, bootTimestamp);

				final long lastUpdateTimestamp = RxDroid.getLastUpdateTimestamp();
				if(lastUpdateTimestamp != 0 && lastUpdateTimestamp > bootTimestamp && !forceSplashWarning)
				{
					Log.w(TAG, "Notification service was not runnning because the app was updated");
				}
				else
				{
					mLaunchMainActivity = false;

					Log.w(TAG, "Notification service was not started on boot: " +
						bootCompletedTimestamp + " vs " + bootTimestamp);

					final Fragment f = new AutostartIssueDialog();
					getSupportFragmentManager().beginTransaction().add(android.R.id.content,
							f).commitAllowingStateLoss();

					//getSupportFragmentManager().beginTransaction().replace(
					//		android.R.id.content, new AutostartIssueDialog()).commit();
				}
			}
		}
		else if(BuildConfig.DEBUG)
		{
			Log.d(TAG,
					"onCreate:" +
					"\n  bootCompletedTimestamp=" + bootCompletedTimestamp +
					"\n           bootTimestamp=" + bootTimestamp);
		}


		if(mLaunchMainActivity)
			setContentView(R.layout.loader);

		mDate = Settings.getActiveDate();

		final SpannableString dateString = new SpannableString(DateTime.toNativeDate(mDate));

		Util.applyStyle(dateString, new RelativeSizeSpan(0.75f));
		Util.applyStyle(dateString, new UnderlineSpan());

		getSupportActionBar().setSubtitle(dateString);

		try
		{
			Class.forName(com.michaelnovakjr.numberpicker.NumberPicker.class.getName());
		}
		catch(ClassNotFoundException e)
		{
			throw new WrappedCheckedException("NumberPicker library is missing", e);
		}
	}

	@Override
	protected void onResume()
	{
		//registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_MAIN));
		RxDroid.getLocalBroadcastManager().registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_MAIN));
		if(mLaunchMainActivity)
			loadDatabaseAndLaunchMainActivity();
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		RxDroid.getLocalBroadcastManager().unregisterReceiver(mReceiver);
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == Dialog.BUTTON_POSITIVE)
		{
			if(deleteDatabase())
			{
				Toast.makeText(this, R.string._toast_db_reset_success, Toast.LENGTH_SHORT).show();
				loadDatabaseAndLaunchMainActivity();
			}
			else
				Toast.makeText(this, R.string._toast_db_reset_failure, Toast.LENGTH_LONG).show();
		}
		else if(which == Dialog.BUTTON_NEGATIVE && mException != null)
			throw mException;

		finish();
	}

	public static void setStatusMessage(int msgResId)
	{
		final Context context = RxDroid.getContext();
		final Intent intent = new Intent(context, DatabaseStatusReceiver.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.putExtra(DatabaseStatusReceiver.EXTRA_MESSAGE, msgResId);

		LocalBroadcastManager bm = RxDroid.getLocalBroadcastManager();
		bm.sendBroadcast(intent);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id, Bundle args)
	{
		if(id == R.id.db_error_dialog)
		{
			final AlertDialog.Builder ab = new AlertDialog.Builder(SplashScreenActivity.this);
			ab.setTitle(R.string._title_error);
			ab.setIcon(android.R.drawable.ic_dialog_alert);
			ab.setCancelable(false);
			ab.setNegativeButton(R.string._btn_exit, SplashScreenActivity.this);
			ab.setPositiveButton(R.string._btn_reset, SplashScreenActivity.this);
			ab.setMessage("");

			return ab.create();
		}
		return super.onCreateDialog(id, args);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args)
	{
		if(id == R.id.db_error_dialog)
		{
//			final WrappedCheckedException exception =
			mException = (WrappedCheckedException) args.getSerializable(ARG_EXCEPTION);

			final StringBuilder sb = new StringBuilder();

			if(mException.getCauseType() == DatabaseError.class)
			{
				switch(((DatabaseError) mException.getCause()).getType())
				{
					case DatabaseError.E_GENERAL:
						sb.append(getString(R.string._msg_db_error_general));
						break;

					case DatabaseError.E_UPGRADE:
						sb.append(getString(R.string._msg_db_error_upgrade));
						break;

					case DatabaseError.E_DOWNGRADE:
						sb.append(getString(R.string._msg_db_error_downgrade));
						break;
				}
			}
			else
				sb.append(getString(R.string._msg_db_error_general));

			sb.append(" " + RefString.resolve(this, R.string._msg_db_error_footer));
			((AlertDialog) dialog).setMessage(sb);
		}
		else
			super.onPrepareDialog(id, dialog, args);
	}

	/* package */ void loadDatabaseAndLaunchMainActivity() {
		new DatabaseIntializerTask().execute(0);
	}

	private boolean deleteDatabase()
	{
		final String packageName = getApplicationInfo().packageName;

		final File dbDir = new File(Environment.getDataDirectory(), "data/" + packageName + "/databases");
		final File currentDb = new File(dbDir, DatabaseHelper.DB_NAME);

		Log.d(TAG, "deleteDatabase: ");
		Log.d(TAG, "  dbDir=" + dbDir);
		Log.d(TAG, "  currentDb=" + currentDb);
		Log.d(TAG, "  dbDir.canWrite() = " + dbDir.canWrite());
		Log.d(TAG, "  currentDb.exists() = " + currentDb.exists());
		Log.d(TAG, "  currentDb.canWrite() = " + currentDb.canWrite());

		if(!dbDir.canWrite() || !currentDb.exists() || !currentDb.canWrite())
			return false;

		return currentDb.delete();
	}

	private void launchMainActivity()
	{
		(new Thread() {

			@SuppressWarnings("unused")
			@Override
			public void run()
			{
				while(Database.hasPendingOperations())
				{
					Log.i(TAG, "Waiting for database to settle");
					try
					{
						Thread.sleep(50);
					}
					catch (InterruptedException e)
					{
						Log.w(TAG, e);
						break;
					}
				}

				final boolean isFirstLaunch;

				if(!BuildConfig.DEBUG && Database.countAll(Drug.class) != 0)
				{
					isFirstLaunch = false;
					Settings.putBoolean(Settings.Keys.IS_FIRST_LAUNCH, false);
				}
				else
					isFirstLaunch = Settings.getBoolean(Settings.Keys.IS_FIRST_LAUNCH, true);

				final Class<?> intentClass = isFirstLaunch ? DoseTimePreferenceActivity.class : DrugListActivity.class;

				Intent intent = new Intent(getBaseContext(), intentClass);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
				intent.putExtra(DoseTimePreferenceActivity.EXTRA_IS_FIRST_LAUNCH, isFirstLaunch);
				intent.putExtra(DrugListActivity.EXTRA_DATE, mDate);
				startActivity(intent);

				finish();
			}
		}).start();
	}

	private class DatabaseIntializerTask extends AsyncTask<Integer, Void, WrappedCheckedException>
	{
		//private boolean mAttemptedDatabaseReload = false;

		@Override
		protected WrappedCheckedException doInBackground(Integer... params)
		{
			int count = (params == null || params.length == 0) ? 0 : params[0];

			try
			{
				if(count == 0)
					Database.init();
				else
					Database.reload(RxDroid.getContext());
			}
			catch(Exception e)
			{
				if(count < Database.TABLE_COUNT)
					return doInBackground(++count);

				return new WrappedCheckedException(e);
			}

			return null;
		}

		@SuppressWarnings("deprecation")
		@Override
		protected void onPostExecute(WrappedCheckedException result)
		{
			setStatusMessage(R.string._title_db_status_loading);

			if(result == null)
				launchMainActivity();
			else
			{
				Log.w(TAG, result.getRootCause());

				Bundle args = new Bundle();
				args.putSerializable(ARG_EXCEPTION, result);
				showDialog(R.id.db_error_dialog, args);
			}
		}
	}
}

class AutostartIssueDialog extends DialogueLike
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setTitle(R.string._title_warning);
		setMessage(Html.fromHtml(RefString.resolve(getActivity(), R.string._msg_not_started_on_boot)));
		setIcon(Theme.getResourceAttribute(R.attr.iconWarning));
		setNegativeButtonText(R.string._btn_report);
	}

	@Override
	public void onButtonClick(Button button, int which)
	{
		if(which == BUTTON_POSITIVE)
		{
			getActivity().setContentView(R.layout.loader);
			((SplashScreenActivity) getActivity()).loadDatabaseAndLaunchMainActivity();
		}
		else
		{
			final Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.setType("plain/text");
			intent.putExtra(Intent.EXTRA_EMAIL, "josephclehner+rxdroid-issue@gmail.com");
			intent.putExtra(Intent.EXTRA_SUBJECT, "[ISSUE] " + Build.MODEL + " auto-start");
			intent.putExtra(Intent.EXTRA_TEXT,
					"MANUFACTURER: " + Build.MANUFACTURER + "\n" +
							"PRODUCT : " + Build.PRODUCT + "\n" +
							"MODEL : " + Build.MODEL + "\n" +
							"DEVICE : " + Build.DEVICE + "\n" +
							"DISPLAY : " + Build.DISPLAY + "\n" +
							"RELEASE : " + Build.VERSION.RELEASE + "\n" +
							"SDK_INT : " + Build.VERSION.SDK_INT + "\n" +
							"=========================\n\n"
			);

			startActivity(Intent.createChooser(intent, getString(R.string._btn_report)));
		}
	}
}
