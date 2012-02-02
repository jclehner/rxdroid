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

package at.caspase.rxdroid;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.DatabaseHelper;
import at.caspase.rxdroid.db.DatabaseHelper.DatabaseError;

public class LoaderActivity extends Activity implements OnClickListener
{
	private static final String TAG = LoaderActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loader);
		loadDatabaseAndLaunchMainActivity();
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
		else if(which != Dialog.BUTTON_NEGATIVE)
			return;

		finish();
	}

	private void loadDatabaseAndLaunchMainActivity()
	{
		if(loadDatabase())
			launchMainActivity();
	}

	private boolean loadDatabase()
	{
		GlobalContext.set(getApplicationContext());

		try
		{
			Database.init();
			return true;
		}
		catch(DatabaseError e)
		{
			Log.w(TAG, e);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string._title_error);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setCancelable(false);

			StringBuilder sb = new StringBuilder();

			switch(e.getType())
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

			sb.append(getString(R.string._msg_db_error_footer));

			builder.setMessage(sb.toString());
			builder.setNegativeButton(R.string._btn_exit, this);
			builder.setPositiveButton(R.string._btn_reset, this);

			builder.show();
		}

		return false;
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
		Intent intent = new Intent(this, DrugListActivity.class);
		//Intent intent = new Intent(this, DrugSortActivity.class); // XXX
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(intent);

		finish();
	}
}
