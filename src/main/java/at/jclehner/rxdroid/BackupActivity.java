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
