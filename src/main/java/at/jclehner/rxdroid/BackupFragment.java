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

import android.os.Environment;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import at.jclehner.androidutils.LoaderListFragment;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

public class BackupFragment extends LoaderListFragment<File>
{
	static class BackupFileHolder extends LLFLoader.ItemHolder<File> implements Comparable<BackupFileHolder>
	{
		BackupFileHolder(File file)
		{
			super(file);

			final Backup.BackupFile bf = new Backup.BackupFile(file.getAbsolutePath());

			uri = Uri.fromFile(file);
			location = bf.getLocation();
			isValid = bf.isValid();

			if(isValid)
				mTimestamp = bf.getTimestamp();
			else
				mTimestamp = new Date(file.lastModified());

			dateTime = DateTime.toNativeDateAndTime(mTimestamp);
		}

		@Override
		public int compareTo(BackupFileHolder another) {
			return another.mTimestamp.compareTo(mTimestamp);
		}

		private final Date mTimestamp;

		final Uri uri;
		final String location;
		final String dateTime;
		final boolean isValid;
	}

	static class Loader extends LLFLoader<File> implements FilenameFilter
	{
		Loader(Context context)
		{
			super(context);
		}

		@Override
		public List<? extends ItemHolder<File>> doLoadInBackground()
		{
			final List<File> dirs = new ArrayList<>();
			dirs.add(mContext.getFilesDir());
			dirs.add(new File(Environment.getExternalStorageDirectory(), "RxDroid"));

			final List<BackupFileHolder> data = new ArrayList<BackupFileHolder>();

			for(File dir : dirs)
			{
				if(!dir.exists() || !dir.isDirectory())
					continue;

				final File[] files = dir.listFiles(this);
				if(files != null)
				{
					for(File file : dir.listFiles(this))
					{
						if(file.isFile())
							data.add(new BackupFileHolder(file));
					}
				}
			}

			Collections.sort(data);

			return data;
		}

		@Override
		public boolean accept(File dir, String filename)
		{
			return filename.endsWith(".rxdbak");
		}
	}

	class Adapter extends LLFAdapter<File>
	{
		Adapter()
		{
			super(BackupFragment.this);
		}

		@Override
		public View getView(int position, View view, ViewGroup parent)
		{
			if(view == null)
			{
				view = LayoutInflater.from(getActivity()).inflate(
						R.layout.list_item_2_menu, parent, false);
			}

			final BackupFileHolder data = getItemHolder(position);

			final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
			final TextView text2 = (TextView) view.findViewById(android.R.id.text2);

			text1.setText(data.dateTime);

			text2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
			text2.setTypeface(Typeface.MONOSPACE);
			text2.setText(data.location);

			view.findViewById(R.id.btn_menu).setOnClickListener(mMenuListener);
			view.findViewById(R.id.btn_menu).setTag(data);

			return view;
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		if(!getActivity().getIntent().getBooleanExtra(BackupActivity.EXTRA_NO_BACKUP_CREATION, false))
		{
			final MenuItem item = menu.add(getString(R.string._title_create_backup))
					.setIcon(R.drawable.ic_action_add_box_white)
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
					{
						@Override
						public boolean onMenuItemClick(MenuItem menuItem)
						{
							final File outFile;
							if(Backup.StorageStateListener.isWritable(Backup.getStorageState()))
								outFile = null;
							else
							{
								outFile = getActivity().getFileStreamPath("backup.rxdbak");
								outFile.delete();
								Toast.makeText(getActivity(), R.string._msg_external_storage_not_writeable,
										Toast.LENGTH_LONG).show();
							}

							try
							{
								Backup.createBackup(outFile, null);
							} catch(ZipException e)
							{
								showExceptionDialog(e);
							}

							if(outFile == null)
								getLoaderManager().restartLoader(0, null, BackupFragment.this);
							else
								shareBackupFile(outFile);

							return true;
						}
					});

			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string._title_backup_restore);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string._msg_no_backups_available));
	}

	@Override
	protected LLFLoader<File> onCreateLoader()
	{
		return new Loader(getActivity());
	}

	@Override
	protected LLFAdapter<File> onCreateAdapter()
	{
		return new Adapter();
	}

	@Override
	protected void onLoaderException(RuntimeException e)
	{
		showExceptionDialog(e);
		Log.w("BackupFragment", e);
	}

	private void showExceptionDialog(Exception e)
	{
		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
		ab.setTitle(R.string._title_error);
		ab.setIcon(android.R.drawable.ic_dialog_alert);
		ab.setPositiveButton(android.R.string.ok, null);
		ab.setCancelable(true);

		ab.setMessage(Html.fromHtml("<tt>" + e.getClass().getSimpleName() + "</tt><br/>"
				+ Util.escapeHtml(e.getMessage())));

		//ab.show(getFragmentManager(), "create_error");
		ab.show();
	}

	private void shareBackupFile(File file)
	{
		final Intent target = new Intent(Intent.ACTION_SEND);
		target.putExtra(Intent.EXTRA_STREAM, Util.getExternalFileUri(file));
		target.setType("application/octet-stream");

		final Intent intent = Intent.createChooser(target, getString(R.string._title_share));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		startActivity(intent);
	}


	private View.OnClickListener mMenuListener = new View.OnClickListener()
	{
		@Override
		public void onClick(final View v)
		{
			final BackupFileHolder file = (BackupFileHolder) v.getTag();

			final PopupMenu pm = new PopupMenu(getActivity(), v);
			pm.inflate(R.menu.backup);
			pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
			{
				@Override
				public boolean onMenuItemClick(android.view.MenuItem item)
				{

					if(item.getItemId() == R.id.menuitem_restore)
					{
						if(file.isValid)
						{
							final FragmentManager fm = getFragmentManager();
							final FragmentTransaction ft = fm.beginTransaction();
							ft.replace(android.R.id.content, BackupActivity.ImportDialog.newInstance(file.item.toString()));
							ft.addToBackStack(null);

							ft.commit();
						}
						else
						{
							Toast.makeText(getActivity(), R.string._msg_invalid_backup_file,
									Toast.LENGTH_LONG).show();
						}
					}
					else if(item.getItemId() == R.id.menuitem_delete)
					{
						final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
						ab.setMessage(getString(R.string._title_delete_item, file.item.getName()));
						ab.setNegativeButton(android.R.string.cancel, null);
						ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									if(!file.item.delete())
										Toast.makeText(getActivity(), R.string._title_error, Toast.LENGTH_SHORT).show();

									getLoaderManager().restartLoader(0, null, BackupFragment.this);
								}
						});

						ab.show();
					}
					else if(item.getItemId() == R.id.menuitem_share)
					{
						shareBackupFile(file.item);
					}

					return true;
				}
			});

			pm.show();
		}
	};
}
