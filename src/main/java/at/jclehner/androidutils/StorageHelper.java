package at.jclehner.androidutils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Path;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.WorkSource;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Util;

public class StorageHelper
{
	private static final String TAG = "StorageHelper";
	private static final boolean DEBUG = false;

	public interface Formatter
	{
		String getRemovableName(int index, boolean single);
		String getNonRemovableName(int index, boolean single);
	}

	public static class SimpleFormatter implements Formatter
	{
		@Override
		public String getRemovableName(int index, boolean single)
		{
			return "[ext" + (!single ? " " + index : "") + "]";
		}

		@Override
		public String getNonRemovableName(int index, boolean single)
		{
			return "[int" + (!single ? " " + index : "") + "]";
		}
	}

	public static class PathInfo
	{
		PathInfo(File path, boolean emulated, boolean removable)
		{
			this.path = path;
			this.emulated = emulated;
			this.removable = removable;
		}

		@Override
		public String toString()
		{
			return "PathInfo { '" + path + "' "  + (emulated ? "E" : "") + (removable ? "R" : "") + " }";
		}

		public boolean isConsideredRemovable()
		{
			return !emulated && removable;
		}

		public final File path;
		public final boolean emulated;
		public final boolean removable;
	}

	public static String getPrettyName(String path, Context context, Formatter formatter)
	{
		if(formatter == null)
			formatter = new SimpleFormatter();

		final List<PathInfo> dirs = getDirectories(context);
		for(PathInfo dir : dirs)
		{
			final String absDirPath = dir.path.getAbsolutePath();

			if(path.startsWith(absDirPath))
				return path.replace(absDirPath, getPrettyName(dir, dirs, formatter));
		}

		return path;
	}

	private static String getPrettyName(PathInfo dir, List<PathInfo> dirs, Formatter formatter)
	{
		List<PathInfo> filtered = getRemovablePaths(dirs);
		//count -= filtered.size();
		int index = filtered.indexOf(dir);
		if (index >= 0)
			return formatter.getRemovableName(index, filtered.size() == 1);

		filtered = getInternalPaths(dirs);
		//count -= filtered.size();
		index = filtered.indexOf(dir);
		if (index >= 0)
			return formatter.getNonRemovableName(index, filtered.size() == 1);

		throw new IllegalArgumentException("path=" + dir.path);
	}

	public static List<PathInfo> getRemovablePaths(List<PathInfo> paths)
	{
		return CollectionUtils.filter(paths, new CollectionUtils.Filter<PathInfo>() {
			@Override
			public boolean matches(PathInfo pathInfo)
			{
				return pathInfo.isConsideredRemovable();
			}
		});
	}

	public static List<PathInfo> getInternalPaths(List<PathInfo> paths)
	{
		return CollectionUtils.filter(paths, new CollectionUtils.Filter<PathInfo>() {
			@Override
			public boolean matches(PathInfo pathInfo)
			{
				return !pathInfo.isConsideredRemovable();
			}
		});
	}

	public static List<PathInfo> getDirectories(Context context)
	{
		final List<File> dirList = new ArrayList<>();
		dirList.add(Environment.getExternalStorageDirectory());

		getDirsFromContext(context, dirList);
		getDirsFromEnv(dirList);
		getDirsFromHardcodedPaths(dirList);

		// Remove duplicates

		final Set<String> dirSet = new HashSet<>();
		for (int i = 0; i != dirList.size(); ++i)
		{
			final File dir = dirList.get(i);
			if (!dir.isDirectory())
			{
				if (DEBUG) Log.d(TAG, "Not a directory: " + dir);
				continue;
			}

			try
			{
				if(dirSet.add(dir.getCanonicalPath()))
					continue;
				else
					if (DEBUG) Log.d(TAG, "Duplicate path: " + dir);
			}
			catch(IOException e)
			{
				if (DEBUG) Log.d(TAG, "getDirectories", e);
			}

			if (i != 0)
			{
				dirList.remove(i);
				--i;
			}
		}

		final List<PathInfo> dirs = new ArrayList<>();
		for(int i = 0; i != dirList.size(); ++i)
		{
			try
			{
				final File path = dirList.get(i);
				dirs.add(new PathInfo(
						path,
						isEmulated(path, dirList),
						isRemovable(path, dirList)
				));
			}
			catch (IllegalArgumentException e)
			{
				Log.w(TAG, e);
			}
		}

		return dirs;
	}

	private static void getDirsFromEnv(List<File> outDirs)
	{
		// [0] = static final String in android.os.Environment
		// [1] = environment variable name
		final String[][] envs = {
				{ "ENV_SECONDARY_STORAGE", "SECONDARY_STORAGE" },
				{ null, "EXTERNAL_STORAGE_SD" }
		};

		for (String[] env : envs)
		{
			final File path = getPathEnv(env[0], env[1], null);
			if (path != null)
				outDirs.add(path);
		}
	}

	private static void getDirsFromHardcodedPaths(List<File> outDirs)
	{
		final String storage = getPathEnv(null, "ANDROID_STORAGE", "/storage").getPath();
		// Inspired by https://stackoverflow.com/questions/13976982
		final String[] paths = {
				storage + "/sdcard1",
				storage + "/sdcard2",
				storage + "/extSdCard",
				storage + "/ext_sd",
				storage + "/external_SD",
				storage + "/removable/sdcard1",
				"/mnt/external_sd",
				"/mnt/extSdCard",
				"/mnt/external1",
				"/mnt/media_rw/sdcard1",
				"/mnt/sdcard/external_sd",
				"/mnt/ext_card",
				"/mnt/Removable/MicroSD",
				"/external_sd",
				"/sdcard2",
				"/Removable/MicroSD",
				"/data/sdext"
		};

		for (int i = 0; i != paths.length; ++i)
		{
			final File path = new File(paths[i]);
			if (path.exists() && path.isDirectory())
				outDirs.add(path);
		}
	}

	@TargetApi(19)
	private static void getDirsFromContext(Context context, List<File> outDirs)
	{
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			return;

		final File[] dirs = context.getExternalFilesDirs(null);

		for (int i = 1; i < dirs.length; ++i)
		{
			if (dirs[i] == null)
				continue;

			final String absPath = dirs[i].getAbsolutePath();
			final String path = absPath.replaceAll(
					"/Android/data/" + context.getPackageName().replace(".", "\\.") + ".*", "");
			if (!path.equals(absPath))
				outDirs.add(new File(path));
			else
				if (DEBUG) Log.d(TAG, "Ignoring " + absPath);
		}
	}

	private static File getPathEnv(String fieldName, String name, String defPath)
	{
		String envVar = name;

		if (fieldName != null)
		{
			try
			{
				final Field f = Environment.class.getDeclaredField(fieldName);
				f.setAccessible(true);
				envVar = (String) f.get(null);
			}
			catch(Exception e)
			{
				if (DEBUG) Log.d(TAG, "getPathEnv", e);
			}
		}

		final String path = System.getenv(envVar);
		if (path != null)
			return new File(path);
		else if (defPath != null)
			return new File(defPath);

		return null;
	}

	private static boolean isRemovable(File path, List<File> dirList)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			return Environment.isExternalStorageRemovable(path);
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
		{
			if (path.equals(dirList.get(0)))
				return Environment.isExternalStorageRemovable();
		}

		// Assume that any dirs but the first one are removable
		return dirList.indexOf(path) > 0;
	}


	private static boolean isEmulated(File path, List<File> dirList)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			return Environment.isExternalStorageEmulated(path);

		try
		{
			if (path.getCanonicalPath().contains("/emulated/"))
				return true;
		}
		catch(IOException e)
		{
			if (DEBUG) Log.d(TAG, "isEmulated", e);
		}

		return !isRemovable(path, dirList);
	}

	private static Set<String> getMounted()
	{
		final Set<String> mounted = new HashSet<>();
		Scanner s = null;

		try
		{
			s = new Scanner(new File("/proc/mounts"));

			while (s.hasNextLine())
			{
				final String[] tokens = s.nextLine().split("\\s+");
				if (tokens.length > 1)
					mounted.add(tokens[1]);
			}
		}
		catch(IOException e)
		{
			Log.w(TAG, e);
		}
		finally
		{
			Util.closeQuietly(s);
		}

		return mounted;
	}

	public static class MountWatcher extends FileObserver
	{
		public MountWatcher(String path)
		{
			super(path);
		}

		@Override
		public void onEvent(int event, String path)
		{
			Log.d(TAG, path + ": event=" + eventToString(event));
		}

		private static String eventToString(int event)
		{
			switch(event & ALL_EVENTS)
			{
				case ACCESS: return "ACCESS";
				case MODIFY: return "MODIFY";
				case ATTRIB: return "ATTRIB";
				case CLOSE_WRITE: return "CLOSE_WRITE";
				case CLOSE_NOWRITE: return "CLOSE_NOWRITE";
				case OPEN: return "OPEN";
				case MOVED_FROM: return "MOVED_FROM";
				case MOVED_TO: return "MOVED_TO";
				case CREATE: return "CREATE";
				case DELETE: return "DELETE";
				case DELETE_SELF: return "DELETE_SELF";
				case MOVE_SELF: return "MOVE_SELF";
				default: return "(EVENT 0x" + Integer.toHexString(event) + ")";
			}
		}
	}
}
