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

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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

	static class Loader extends LLFLoader<File>
	{
		Loader(Context context)
		{
			super(context);
		}

		@Override
		public List<? extends ItemHolder<File>> doLoadInBackground()
		{
			final List<BackupFileHolder> data = new ArrayList<BackupFileHolder>();

			for(File file : Backup.getBackupFiles(mContext))
				data.add(new BackupFileHolder(file));

			Collections.sort(data);

			return data;
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

			text2.setTextAppearance(getActivity(), android.R.attr.textAppearanceSmall);
			text2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
			text2.setTypeface(Typeface.MONOSPACE);
			text2.setText(data.location);

			view.findViewById(R.id.btn_menu).setOnClickListener(mMenuListener);
			view.findViewById(R.id.btn_menu).setTag(data);

			return view;
		}
	}

	private boolean mShowDialogIfNotWriteable = true;
	private File[] mDirectories;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		if(!getActivity().getIntent().getBooleanExtra(BackupActivity.EXTRA_NO_BACKUP_CREATION, false))
		{
			MenuItem item;

			if(BuildConfig.DEBUG)
			{
				item = menu.add("Encrypt all").setIcon(R.drawable.ic_action_lock_closed);
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						new AsyncTask<Void, String, Exception>()
						{
							private ProgressDialog mDialog;
							private List<File> mFiles;

							@Override
							protected void onPreExecute()
							{
								mFiles = Backup.getBackupFiles(getActivity());

								mDialog = new ProgressDialog(getActivity());
								mDialog.setTitle("Encrypting...");
								mDialog.setCancelable(false);
								mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
								mDialog.setIndeterminate(false);
								mDialog.setMax(mFiles.size());
								mDialog.setProgress(0);
								mDialog.show();
							}

							@Override
							protected Exception doInBackground(Void... params)
							{
								int progress = 0;

								for(File file : mFiles)
								{
									try
									{
										Backup.encrypt(getActivity(), file, "foobar");
									}
									catch(IOException | ZipException e)
									{
										Log.w("BackupFragment", e);
										return e;
									}

									mDialog.setProgress(progress++);
								}

								return null;
							}

							@Override
							protected void onCancelled()
							{
								super.onCancelled();
							}

							@Override
							protected void onPostExecute(Exception e)
							{
								if(mDialog != null)
								{
									mDialog.dismiss();
									mDialog = null;
								}

								if(e != null)
									Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();

								restartLoader();

							}
						}.execute();
						return true;
					}
				});
			}

			item = menu.add(getString(R.string._title_create_backup))
					.setIcon(R.drawable.ic_action_add_box_white)
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
					{
						@Override
						public boolean onMenuItemClick(MenuItem menuItem)
						{
							final String storageState = Backup.getStorageState();
							if(Backup.StorageStateListener.isWritable(storageState))
							{
								try
								{
									Backup.createBackup(null, null);
									getLoaderManager().restartLoader(0, null, BackupFragment.this);
								} catch(ZipException e)
								{
									showExceptionDialog(e);
								}
							}
							else
							{
								if(mShowDialogIfNotWriteable)
								{
									final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
									ab.setPositiveButton(android.R.string.ok, null);
									ab.setMessage(R.string._msg_external_storage_not_writeable);
									ab.setTitle(R.string._title_error);
									ab.setIcon(android.R.drawable.ic_dialog_alert);

									ab.show();
									mShowDialogIfNotWriteable = false;
								}
								else
								{
									Toast.makeText(getActivity(), R.string._msg_external_storage_not_writeable,
											Toast.LENGTH_LONG).show();
								}
							}

							return true;
						}
					});

			item.setShowAsAction(MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
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
		mShowDialogIfNotWriteable = true;

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
						final Intent target = new Intent(Intent.ACTION_SEND);
						target.putExtra(Intent.EXTRA_STREAM, file.uri);
						target.setType("application/octet-stream");

						final Intent intent = Intent.createChooser(target, getString(R.string._title_share));
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						startActivity(intent);
					}

					return true;
				}
			});

			pm.show();
		}
	};
}
