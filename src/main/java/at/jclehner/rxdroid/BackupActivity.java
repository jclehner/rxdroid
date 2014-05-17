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

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import java.io.File;

import at.jclehner.rxdroid.util.Components;

public class BackupActivity extends SherlockFragmentActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);
		super.onCreate(savedInstanceState);

		setTitle(R.string._title_backup_restore);

		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if(!Backup.DIRECTORY.exists())
			Backup.DIRECTORY.mkdirs();
		else if(!Backup.DIRECTORY.isDirectory())
		{
			// Hackish, but simple - a full blown AlertDialog would be
			// overkill...

			final File newFile = new File(Backup.DIRECTORY + "___");

			Backup.DIRECTORY.renameTo(newFile);
			Backup.DIRECTORY.mkdirs();

			Toast.makeText(this, Backup.DIRECTORY + " -> " + newFile, Toast.LENGTH_LONG).show();
		}

		getSupportFragmentManager().beginTransaction().replace(
				android.R.id.content, new BackupFragment()).commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == android.R.id.home)
			finish();

		return super.onOptionsItemSelected(item);
	}
}
