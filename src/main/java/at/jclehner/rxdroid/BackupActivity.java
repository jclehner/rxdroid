/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.ui.DialogLike;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class BackupActivity extends AppCompatActivity implements DialogLike.OnButtonClickListener,
		ActivityCompat.OnRequestPermissionsResultCallback
{
	public static final String EXTRA_NO_BACKUP_CREATION = "rxdroid:no_backup_creation";

	public static class ImportDialog extends DialogLike
	{
		private boolean mCanRestore = false;
		private Backup.BackupFile mFile;

		public static ImportDialog newInstance(String file)
		{
			final ImportDialog instance = new ImportDialog();
			instance.getArguments().putString("file", file);
			return instance;
		}

		@Override
		public void onResume()
		{
			super.onResume();

			mFile = new Backup.BackupFile(getBackupFilePath());
			mCanRestore = mFile.isValid();

			((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string._title_restore);

			if(mCanRestore)
			{
				getButton(BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
				setTitle(R.string._title_warning);
				setMessage(R.string._msg_restore_backup_warning,
						DateTime.toNativeDateAndTime(mFile.getTimestamp()));
			}
			else
			{
				getButton(BUTTON_NEGATIVE).setVisibility(View.GONE);
				setTitle(R.string._title_error);
				setMessage(R.string._msg_invalid_backup_file);
			}

			setDetail(mFile.getLocation());
		}

		@Override
		public void onButtonClick(DialogLike dialogLike, int which)
		{
			if(which == BUTTON_POSITIVE && mCanRestore)
			{
				if(mFile.isEncrypted())
				{
					final String key = Settings.getString(Settings.Keys.BACKUP_KEY, "");
					if(key.length() == 0 || !restoreBackup(key))
						showPasswordDialog();
				}
				else
					restoreBackup(null);
			}
			else
				finishOrPopBack();
		}

		private void showPasswordDialog()
		{
			final EditText edit = new EditText(getActivity());
			edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			edit.setHint(R.string._title_password);

			final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
			ab.setView(edit);
			// null listener since we're using a View.OnClickListener on the Button itself
			ab.setPositiveButton(android.R.string.ok, null);
			ab.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					finishOrPopBack();
				}
			});

			final AlertDialog dialog = ab.create();
			dialog.setOnShowListener(new DialogInterface.OnShowListener()
			{
				@Override
				public void onShow(DialogInterface dialogInterface)
				{
					final Button b = dialog.getButton(Dialog.BUTTON_POSITIVE);
					if(b != null)
					{
						b.setEnabled(false);
						b.setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								if(!restoreBackup(edit.getText().toString()))
									edit.setError(getString(R.string._title_error));
								else
									dialog.dismiss();
							}
						});
					}
				}
			});

			edit.addTextChangedListener(new TextWatcher()
			{
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {}

				@Override
				public void afterTextChanged(Editable s)
				{
					final Button b = dialog.getButton(Dialog.BUTTON_POSITIVE);
					if(b == null)
						return;

					if(s == null || s.length() == 0)
						b.setEnabled(false);
					else
						b.setEnabled(true);

					edit.setError(null);
				}
			});

			edit.setOnFocusChangeListener(new View.OnFocusChangeListener()
			{
				@Override
				public void onFocusChange(View v, boolean hasFocus)
				{
					if(hasFocus)
					{
						dialog.getWindow().setSoftInputMode(
								WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
								| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
						);
					}
				}
			});

			dialog.show();
		}

		private void finishOrPopBack()
		{
			if(Intent.ACTION_VIEW.equals(getActivity().getIntent().getAction()))
				getActivity().finish();
			else
				getFragmentManager().popBackStack();
		}

		private boolean restoreBackup(String password)
		{
			Log.i("BackupActivity", "Restoring backup with DBv" + mFile.dbVersion());

			if(mFile.restore(password))
			{
				if(mFile.dbVersion() == DatabaseHelper.DB_VERSION)
				{
					Database.reload(RxDroid.getContext());
					startActivity(RxDroid.getLaunchIntent());
					getActivity().finish();
				}
				else
				{
					RxDroid.forceRestart();
				}

				return true;
			}

			return false;
		}

		private String getBackupFilePath()
		{
			final String arg = getArguments().getString("file");
			if(arg != null)
				return arg;

			final Uri data = getActivity().getIntent().getData();
			if(data != null)
			{
				if(!ContentResolver.SCHEME_CONTENT.equals(data.getScheme()))
					return data.getSchemeSpecificPart();

				return createBackupFileFromContentStream(data);
			}

			throw new IllegalStateException("No 'file' argument given and no data in hosting Activity");
		}

		private String createBackupFileFromContentStream(Uri uri)
		{
			try
			{
				final File tempFile = new File(getActivity().getCacheDir(), "temp.rxdbak");
				final InputStream in = getActivity().getContentResolver().openInputStream(uri);
				final OutputStream out = new FileOutputStream(tempFile);
				Util.copy(in, out);

				return tempFile.getAbsolutePath();
			}
			catch(FileNotFoundException e)
			{
				throw new WrappedCheckedException(e);
			}
			catch(IOException e)
			{
				throw new WrappedCheckedException(e);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_activity);
		setTitle(R.string._title_backup_restore);

		mStorageListener = new Backup.StorageStateListener(this) {
			@Override
			public void onStateChanged(String storageState, Intent intent)
			{
				setContentFragment(false);
			}
		};

		if(savedInstanceState == null)
			setContentFragment(true);
		else
			mStorageListener.onStateChanged(Environment.getExternalStorageState(), null);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mStorageListener.register(this);
	}

	@Override
	protected void onPause()
	{
		mStorageListener.unregister(this);
		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == android.R.id.home)
			finish();

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onButtonClick(DialogLike dialogLike, int which) {
		finish();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		if(grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
		{
			mStorageListener.update(this);
			setContentFragment(false);
		}
	}

	private boolean shouldShowPermissionDialog()
	{
		return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED;
	}

	private void setContentFragment(boolean calledFromOnCreate)
	{
		if(shouldShowPermissionDialog())
		{
			ActivityCompat.requestPermissions(this,
					new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);

		}

		final Fragment content;

		if(!Intent.ACTION_VIEW.equals(getIntent().getAction()))
		{
			//getActionBar().setDisplayShowHomeEnabled(true);
			//getActionBar().setDisplayHomeAsUpEnabled(true);

			if(mStorageListener.isReadable())
			{
				content = new BackupFragment();

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
			}
			else
			{
				final DialogLike dialog = new DialogLike();
				dialog.setTitle(getString(R.string._title_error));
				dialog.setMessage(getString(R.string._msg_external_storage_not_readable));
				dialog.setPositiveButtonText(getString(android.R.string.ok));

				content = dialog;
			}
		}
		else
			content = new ImportDialog();

		//setContentView(R.layout.simple_activity);

		final Fragment currentFragment = getFragmentManager().findFragmentByTag("content");
		final FragmentTransaction ft = getFragmentManager().beginTransaction();

		if(currentFragment != null)
		{
			//ft.detach(currentFragment);
			ft.remove(currentFragment);
		}

		ft.replace(android.R.id.content, content, "content");

		if(!calledFromOnCreate)
			ft.addToBackStack(null);

		ft.commit();
		invalidateOptionsMenu();
	}

	private Backup.StorageStateListener mStorageListener;
}
