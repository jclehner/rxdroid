package at.caspase.rxdroid.db;

import java.lang.reflect.Field;

import android.util.Log;
import at.caspase.rxdroid.util.Reflect;

import com.j256.ormlite.field.DatabaseField;

public final class UpgradeUtils
{
	private static final String TAG = UpgradeUtils.class.getName();
	private static final boolean LOGV = true;

	public static void copyDrug(Drug dest, Entry src)
	{
		Class<?> clsD = dest.getClass();
		Class<?> clsS = src.getClass();

		if(LOGV) Log.d(TAG, "copyDrug:");

		for(Field fS : clsS.getDeclaredFields())
		{
			if(fS.isAnnotationPresent(DatabaseField.class))
			{
				Field fD = Reflect.getDeclaredField(clsD, fS.getName());
				if(fD != null)
				{
					try
					{
						fD.setAccessible(true);
						fS.setAccessible(true);
						fD.set(dest, fS.get(src));
						if(LOGV) Log.d(TAG, "  " + fS.getName());
					}
					catch(IllegalArgumentException e)
					{
						//Log.w(TAG, e);
					}
					catch(IllegalAccessException e)
					{
						//Log.w(TAG, e);
					}
				}
			}
		}
	}

	private UpgradeUtils() {}
}
