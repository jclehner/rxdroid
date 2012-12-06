/**
 * Copyright (C) 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.DatabaseHelper.DatabaseError;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class SplashScreenActivity extends Activity implements OnClickListener
{
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

	private static final String TAG = SplashScreenActivity.class.getName();
	private static final String ARG_EXCEPTION = "exception";

	private final BroadcastReceiver mReceiver = new DatabaseStatusReceiver();
	private WrappedCheckedException mException = null;

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setTheme(Theme.get());
		setContentView(R.layout.loader);

		if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
		{
			final SpannableString dateString = new SpannableString(DateTime.toNativeDate(DateTime.today()));

			Util.applyStyle(dateString, new RelativeSizeSpan(0.75f));
			Util.applyStyle(dateString, new UnderlineSpan());

			getActionBar().setSubtitle(dateString);
		}

		try
		{
			Class.forName(com.michaelnovakjr.numberpicker.NumberPicker.class.getName());
		}
		catch(ClassNotFoundException e)
		{
			throw new WrappedCheckedException("NumberPicker library is missing", e);
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		//registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_MAIN));
		RxDroid.getLocalBroadcastManager().registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_MAIN));
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

			sb.append(" " + getString(R.string._msg_db_error_footer, getString(R.string._btn_reset)));
			((AlertDialog) dialog).setMessage(sb);
		}
		else
			super.onPrepareDialog(id, dialog, args);
	}

	private void loadDatabaseAndLaunchMainActivity() {
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
