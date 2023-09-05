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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import at.jclehner.androidutils.LoaderListFragment;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

public class BackupFragment extends LoaderListFragment<DocumentFile>
{
	static class BackupFileHolder extends LLFLoader.ItemHolder<DocumentFile> implements Comparable<BackupFileHolder>
	{
		BackupFileHolder(DocumentFile file)
		{
			super(file);

			uri = file.getUri();
			location = file.getName();

			final Backup.BackupFile bf = new Backup.BackupFile(uri);

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

	static class Loader extends LLFLoader<DocumentFile>
	{
		Loader(Context context)
		{
			super(context);
		}

		@Override
		public List<? extends ItemHolder<DocumentFile>> doLoadInBackground()
		{
			final List<BackupFileHolder> ret = new ArrayList<>();
			final DocumentFile dir = Backup.getDirectory();

			if(dir != null)
			{
				for(DocumentFile df : dir.listFiles())
				{
					if(df.isFile() && df.getName().endsWith(".rxdbak"))
						ret.add(new BackupFileHolder(df));
				}

				for (File f : mContext.getCacheDir().listFiles())
				{
					if(f.isFile() && f.getName().endsWith(".rxdbak"))
						ret.add(new BackupFileHolder(DocumentFile.fromFile(f)));
				}

				Collections.sort(ret);
			}

			return ret;
		}
	}

	class Adapter extends LLFAdapter<DocumentFile>
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
							Backup.createBackup(false,null);

							getLoaderManager().restartLoader(0, null, BackupFragment.this);

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
	protected LLFLoader<DocumentFile> onCreateLoader()
	{
		return new Loader(getActivity());
	}

	@Override
	protected LLFAdapter<DocumentFile> onCreateAdapter()
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

	private void shareBackupFile(DocumentFile df)
	{
		final Intent target = new Intent(Intent.ACTION_SEND);
		target.setType("application/octet-stream");
		target.putExtra(Intent.EXTRA_STREAM, df.getUri());

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
							ft.replace(android.R.id.content, BackupActivity.ImportDialog.newInstance(file.item.getUri()));
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
						// FIXME
						shareBackupFile(file.item);
					}

					return true;
				}
			});

			pm.show();
		}
	};
}
