package at.caspase.rxdroid.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import at.caspase.rxdroid.util.Reflect;

public final class UpgradeHelper
{
	static boolean upgradeTo(int version, DatabaseHelper dbHelper)
	{
		final Method m = Reflect.getMethod(
				UpgradeHelper.class,
				"upgradeToVersion" + version,
				DatabaseHelper.class
		);

		if(m == null)
			return false;

		Reflect.invokeMethod(m, null, dbHelper);
		return true;
	}

	static void upgradeToVersion2(DatabaseHelper dbHelper)
	{

	}






	private UpgradeHelper() {}
}
