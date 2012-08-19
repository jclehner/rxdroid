package at.caspase.rxdroid.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseField;

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

	static void upgradeToVersion1(DatabaseHelper dbHelper)
	{

	}

	static void upgradeToVersion2(DatabaseHelper dbHelper)
	{

	}

	static void createAlterTableSyntax(Class<?> oldDataClass, Class<?> newDataClass)
	{
		List<Field> added = new ArrayList<Field>();
		List<Field> removed = new ArrayList<Field>();
		List<Field> changed = new ArrayList<Field>();

		for(Field f : Reflect.getDeclaredAnnotatedFields(oldDataClass, DatabaseField.class))
		{
			final Field newField = Reflect.getDeclaredField(newDataClass, f.getName());
			if(newField != null)
			{
				String oldName = getColumnName(f);
				String newName = getColumnName(newField);

				if(!oldName.equals(newName))
				{

				}


			}
			else
				removed.add(f);




		}


	}

	private static String getColumnName(Field f)
	{
		final DatabaseField a = f.getAnnotation(DatabaseField.class);
		final String columnName = Reflect.getAnnotationParameter(a, "columnName");
		if(columnName == null || columnName.length() == 0)
			return f.getName();
		return columnName;
	}




	private UpgradeHelper() {}
}
