package at.jclehner.rxdroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import at.jclehner.androidutils.AlertDialogFragmentBuilder;
import at.jclehner.androidutils.LoaderListFragment;
import at.jclehner.androidutils.PopupMenuCompatBuilder;
import at.jclehner.rxdroid.util.DateTime;

public class BackupFragment extends LoaderListFragment<File>
{
	static class BackupFile extends LLFLoader.ItemHolder<File> implements Comparable<BackupFile>
	{
		BackupFile(File file)
		{
			super(file);

			this.uri = Uri.fromFile(file);

			final String fileStr = file.getAbsolutePath();
			final String extDir = Environment.getExternalStorageDirectory().getAbsolutePath();
			if(fileStr.startsWith(extDir))
				location = fileStr.substring(extDir.length() + 1);
			else
				location = fileStr;

			dateTime = DateTime.toNativeDateAndTime(new Date(file.lastModified()));
		}

		@Override
		public int compareTo(BackupFile another)
		{
			return (int) (another.item.lastModified() - this.item.lastModified());
		}

		final Uri uri;
		final String location;
		final String dateTime;
	}

	static class Loader extends LLFLoader<File> implements FilenameFilter
	{
		Loader(Context context)
		{
			super(context);
		}

		@Override
		public List<? extends ItemHolder<File>> loadInBackground()
		{
			final File dir = new File(Environment.getExternalStorageDirectory(), "RxDroid");
			if(!dir.exists())
			{
				dir.mkdir();
				return Collections.emptyList();
			}
			else if(!dir.isDirectory())
				throw new IllegalStateException(dir + ": not a directory");

			final List<BackupFile> data = new ArrayList<BackupFile>();

			for(File file : dir.listFiles(this))
				data.add(new BackupFile(file));

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

			final BackupFile data = getItemHolder(position);

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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		final MenuItem item = menu.add(getString(R.string._title_create_backup))
				.setIcon(R.drawable.ic_action_add)
				.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
				{
					@Override
					public boolean onMenuItemClick(MenuItem menuItem)
					{
						try
						{
							Backup.createBackup(null);
							getLoaderManager().restartLoader(0, null, BackupFragment.this);
						}
						catch(ZipException e)
						{
							showExceptionDialog(e);
						}

						return true;
					}
				});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
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
	protected void onLoaderException(RuntimeException e) {
		showExceptionDialog(e);
	}

	private void showExceptionDialog(Exception e)
	{
		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
		ab.setTitle(R.string._title_error);
		ab.setIcon(android.R.drawable.ic_dialog_alert);
		ab.setPositiveButton(android.R.string.ok, null);
		ab.setCancelable(true);

		ab.setMessage(Html.fromHtml("<tt>" + e.getClass().getSimpleName() + "</tt><br/>"
				+ Html.escapeHtml(e.getMessage())));

		//ab.show(getFragmentManager(), "create_error");
		ab.show();
	}


	private View.OnClickListener mMenuListener = new View.OnClickListener()
	{
		@Override
		public void onClick(final View v)
		{
			final BackupFile file = (BackupFile) v.getTag();

			final PopupMenuCompatBuilder builder = new PopupMenuCompatBuilder(getActivity(), v);
			builder.setMenuResId(R.menu.backup);
			builder.setTitle(file.dateTime);
			builder.setOnMenuItemClickListener(new PopupMenuCompatBuilder.OnMenuItemClickListener()
			{
				@Override
				public boolean onMenuItemClick(android.view.MenuItem item)
				{

					if(item.getItemId() == R.id.menuitem_restore)
					{
						final FragmentManager fm = getFragmentManager();
						final FragmentTransaction ft = fm.beginTransaction();
						ft.replace(android.R.id.content, ImportActivity.Dialogish.newInstance(file.item.toString()));
						ft.addToBackStack(null);

						ft.commit();
					}
					else if(item.getItemId() == R.id.menuitem_delete)
					{
						if(!file.item.delete())
							Toast.makeText(getActivity(), R.string._title_error, Toast.LENGTH_SHORT).show();

						getLoaderManager().restartLoader(0, null, BackupFragment.this);
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

			builder.show();
		}
	};
}
