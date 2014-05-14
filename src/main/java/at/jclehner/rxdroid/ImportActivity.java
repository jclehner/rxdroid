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
		private boolean mHasZipException = false;
		private ZipFile mZip;

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

			setTitle(R.string._title_warning);

			try
			{
				mZip = new ZipFile(getBackupFile());

				final String[] info = mZip.getComment() != null ?
						mZip.getComment().split(":") : null;

				if(info == null || info.length == 0 || !"rxdbak1".equals(info[0]))
					throw new ZipException("Not an RxDroid backup file");

				final Date cDate = new Date(Long.parseLong(info[1]));

				setMessage(R.string._msg_restore_backup_warning, DateTime.toNativeDateAndTime(cDate));

				final String file = mZip.getFile().toString();
				final String extDir = Environment.getExternalStorageDirectory().toString();
				if(file.startsWith(extDir))
					setDetail(file.substring(extDir.length()));
				else
					setDetail(file);
			}
			catch(ZipException e)
			{
				handleZipException(e);
			}
		}

		@Override
		public void onButtonClick(Button button, int which)
		{
			if(which == BUTTON_POSITIVE)
			{
				try
				{
					if(mZip.isEncrypted())
					{
						final EditText edit = new EditText(getActivity());
						edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
						edit.setHint(R.string._title_password);

						final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
						ab.setView(edit);
						ab.setCancelable(false);
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
					else
						restoreBackup(null);

				} catch(ZipException e)
				{
					handleZipException(e);
				}
			}
			else if(which == BUTTON_NEGATIVE)
			{
				if(getActivity() instanceof ImportActivity)
					getActivity().finish();
				else
					getFragmentManager().popBackStack();
			}
		}

		private void restoreBackup(String password)
		{
			try
			{
				if(password != null)
					mZip.setPassword(password);

				Backup.restoreBackup(mZip);

				startActivity(RxDroid.getLaunchIntent());
				getActivity().finish();
			}
			catch(ZipException e)
			{
				handleZipException(e);
			}
		}

		private String getBackupFile()
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

		private void handleZipException(ZipException e)
		{
			mHasZipException = true;
			Log.w(TAG, e);

			setTitle(R.string._title_error);
			setMessage(R.string._msg_restore_backup_error, e.getLocalizedMessage());
			setDetail(mZip.getFile().toString());

			setNegativeButtonText(R.string._btn_report);

			getButton(BUTTON_POSITIVE).setVisibility(View.GONE);
			setNegativeButtonText(android.R.string.ok);
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
