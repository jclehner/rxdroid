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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
	private static final int REQUEST_BACKUP_DIRECTORY = 0;
	private static final String TAG = BackupActivity.class.getSimpleName();

	public static class ImportDialog extends DialogLike
	{
		private boolean mCanRestore = false;
		private Backup.BackupFile mFile;

		public static ImportDialog newInstance(Uri uri)
		{
			final ImportDialog instance = new ImportDialog();
			instance.getArguments().putString("uri", uri.toString());
			return instance;
		}

		@Override
		public void onResume()
		{
			super.onResume();

			mFile = new Backup.BackupFile(getBackupFileUri());
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
					showPasswordDialog();
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

		private Uri getBackupFileUri()
		{
			final String uriStr = getArguments().getString("uri");
			final Uri uri = (uriStr != null) ? Uri.parse(uriStr) : getActivity().getIntent().getData();
			if(uri != null)
				return uri;

			throw new IllegalStateException("No 'uri' argument given and no data in hosting Activity");
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_activity);
		setTitle(R.string._title_backup_restore);

		if(savedInstanceState == null)
			setContentFragment(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		final MenuItem item = menu.add(getString(R.string._title_change_dir))
				.setIcon(R.drawable.ic_folder_white)
				.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
				{
					@Override
					public boolean onMenuItemClick(MenuItem menuItem)
					{
						showDirectoryChooser();
						return true;
					}
				});

		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onPause()
	{
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
			setContentFragment(false);
		}

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	private boolean shouldShowPermissionDialog()
	{
		return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			return;

		if(requestCode == REQUEST_BACKUP_DIRECTORY && resultCode == Activity.RESULT_OK)
		{
			final Uri uri = (data != null) ? data.getData() : null;
			if(uri != null)
			{
				Settings.putString(Settings.Keys.BACKUP_DIRECTORY, uri.toString());
				final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION;
				getContentResolver().takePersistableUriPermission(uri, flags);
				setContentFragment(false);
			}
		}
	}

	private void setContentFragment(boolean calledFromOnCreate)
	{
		if(shouldShowPermissionDialog())
		{
			ActivityCompat.requestPermissions(this,
					new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);

		}

		final DocumentFile backupDir = Backup.getDirectory();
		if(backupDir == null)
			showDirectoryChooser();

		final Fragment content;

		if(!Intent.ACTION_VIEW.equals(getIntent().getAction()))
			content = new BackupFragment();
		else
			content = new ImportDialog();

		//setContentView(R.layout.simple_activity);

		final FragmentManager fm = getSupportFragmentManager();
		final Fragment currentFragment = fm.findFragmentByTag("content");
		final FragmentTransaction ft = fm.beginTransaction();

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

	private void showDirectoryChooser()
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			final DocumentFile backupDir = Backup.getDirectory();
			if (backupDir != null)
				intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, backupDir.getUri());

			startActivityForResult(intent, REQUEST_BACKUP_DIRECTORY);
			return;
		}
	}
}
