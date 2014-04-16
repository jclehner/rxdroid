package at.jclehner.rxdroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.util.Date;

import at.jclehner.rxdroid.ui.DialogueLike;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;

public class ImportActivity extends SherlockFragmentActivity
{
	private static final String TAG = ImportActivity.class.getSimpleName();

	public static class Dialogish extends DialogueLike
	{
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
		public void onButtonClick(View button, int which)
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
						//edit.setImeOptions(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);

						final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
						//ab.setMessage("Enter password to decrypt backup file");
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
						ab.show();
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


				final Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(RxDroid.getPackageInfo().packageName);
				if(Version.SDK_IS_HONEYCOMB_OR_NEWER)
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				else
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

				startActivity(intent);
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
				return data.getSchemeSpecificPart();

			throw new IllegalStateException("No 'file' argument given and no data in hosting Activity");
		}

		private void handleZipException(ZipException e)
		{
			Log.w(TAG, e);

			setTitle(R.string._title_error);
			setMessage(R.string._msg_restore_backup_error, e.getLocalizedMessage());
			setDetail(mZip.getFile().toString());

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
