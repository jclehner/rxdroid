package at.jclehner.rxdroid;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.util.Util;

public class Backup
{
	private static final String TAG = Backup.class.getSimpleName();

	public static void createBackup(File outFile)
	{
		if(outFile == null)
			outFile = new File(Environment.getExternalStorageDirectory(), "RxDroid/" + System.currentTimeMillis() + ".rxdbak");
		synchronized(Database.LOCK_DATA)
		{
			ZipOutputStream zos = null;

			try
			{
				final File dir = new File(RxDroid.getPackageInfo().applicationInfo.dataDir);
				final FileOutputStream os = new FileOutputStream(outFile);
				zos = new ZipOutputStream(new BufferedOutputStream(os));

				zos.setComment("rxdbak1:" + System.currentTimeMillis() + ":DBv" + DatabaseHelper.DB_VERSION);

				for(int i = 0; i != FILES.length; ++i)
				{
					final File file = new File(dir, FILES[i]);
					if(!file.exists())
						continue;

					final ZipEntry ze = new ZipEntry(FILES[i]);
					zos.putNextEntry(ze);
					zos.write(getFileBytes(file));
					zos.closeEntry();
				}
			}
			catch(IOException e)
			{
				Log.w(TAG, e);
			}
			finally
			{
				Util.closeQuietly(zos);
			}

			final Intent target = new Intent(Intent.ACTION_SEND);
			target.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outFile));
			target.setType("application/octet-stream");

			final Intent i = Intent.createChooser(target, "Backup");
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			RxDroid.doStartActivity(i);
		}
	}

	private static final String[] FILES = {
			"databases/" + DatabaseHelper.DB_NAME,
			"shared_prefs/at.jclehner.rxdroid_preferences.xml",
			"shared_prefs/showcase_internal.xml"
	};

	private static byte[] getFileBytes(File file) throws IOException
	{

		final RandomAccessFile f = new RandomAccessFile(file, "r");
		byte[] b = new byte[(int)f.length()];
		f.read(b);
		return b;
	}
}
