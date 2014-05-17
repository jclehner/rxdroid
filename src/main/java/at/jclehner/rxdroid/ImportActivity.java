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
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import at.jclehner.rxdroid.ui.DialogueLike;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class ImportActivity extends SherlockFragmentActivity
{
	private static final String TAG = ImportActivity.class.getSimpleName();

	public static class Dialogish extends DialogueLike
	{
		private boolean mCanRestore = false;
		private Backup.BackupFile mFile;

		public static Dialogish newInstance(String file)
		{
			final Dialogish instance = new Dialogish();
			instance.getArguments().putString("file", file);
			return instance;
		}

		@Override
		public void onResume()
		{
			super.onResume();

			mFile = new Backup.BackupFile(getBackupFilePath());
			mCanRestore = mFile.isValid();

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
		public void onButtonClick(Button button, int which)
		{
			if(which == BUTTON_POSITIVE && mCanRestore)
			{
				if(!mFile.isEncrypted())
					showPasswordDialog();
				else
					restoreBackup(null);
			}
			else
			{
				if(getActivity() instanceof ImportActivity)
					getActivity().finish();
				else
					getFragmentManager().popBackStack();
			}
		}

		private void showPasswordDialog()
		{
			final EditText edit = new EditText(getActivity());
			edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			edit.setHint(R.string._title_password);

			final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
			ab.setView(edit);
			//ab.setCancelable(false);
			ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					restoreBackup(edit.getText().toString());
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
						b.setEnabled(false);

					final Window w = dialog.getWindow();
					if(w != null)
					{
						w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
										| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
						);
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
				}
			});

			dialog.show();
		}

		private void restoreBackup(String password)
		{
			try
			{
				mFile.restore(password);
				startActivity(RxDroid.getLaunchIntent());
				getActivity().finish();
			}
			catch(ZipException e)
			{
				throw new WrappedCheckedException(e);
			}
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

		getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
				new Dialogish()).commit();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}
}
